from celery_app import celery_app
from pymongo import MongoClient
from minio import Minio
from minio.error import S3Error
import io
from pymongo.errors import PyMongoError

# Store active multipart upload sessions
multipart_sessions = {}

# Task to process and upload file chunks using multipart upload
@celery_app.task(bind=True, autoretry_for=(S3Error,), retry_backoff=True, max_retries=3)
def process_chunk(self, user_id, file_id, filename, chunk, content_type, part_number):
    try:
        # Initialize MinIO client
        minio_client = Minio(
            "file-storage-microservice-minio:9000",
            access_key="root",
            secret_key="root1234",
            secure=False
        )

        bucket_name = f"user-bucket-{user_id}"

        if not minio_client.bucket_exists(bucket_name):
            minio_client.make_bucket(bucket_name)

        # Initiate a multipart upload if not already initiated
        if file_id not in multipart_sessions:
            upload_id = minio_client.create_multipart_upload(bucket_name, file_id, content_type=content_type).upload_id
            multipart_sessions[file_id] = upload_id
        else:
            upload_id = multipart_sessions[file_id]

        # Upload the chunk as a part using the part number
        chunk_stream = io.BytesIO(chunk)
        minio_client.upload_part(
            bucket_name,
            file_id,
            upload_id,
            part_number,  # Each part must have a unique part number
            chunk_stream,
            length=len(chunk)
        )

    except Exception as exc:
        print(f"Error while processing chunk: {exc}")
        raise self.retry(exc=exc)

# Task to finalize the multipart upload by combining all parts
@celery_app.task(autoretry_for=(S3Error,), retry_backoff=True, max_retries=3)
def finalize_upload(user_id, file_id, filename, total_size, content_type):
    try:
        # Set up MongoDB client
        client = MongoClient("mongodb://root:example@file_storage_microservice_mongodb:27017/")
        db = client["filedb"]

        bucket_name = f"user-bucket-{user_id}"

        # Set up MinIO client
        minio_client = Minio(
            "file-storage-microservice-minio:9000",
            access_key="root",
            secret_key="root1234",
            secure=False
        )

        # Get the upload session
        upload_id = multipart_sessions.get(file_id)
        if not upload_id:
            raise Exception(f"No active upload session for file: {file_id}")

        # Complete the multipart upload by combining all parts
        parts = minio_client.list_parts(bucket_name, file_id, upload_id)
        part_list = [{"PartNumber": part.part_number, "ETag": part.etag} for part in parts]

        minio_client.complete_multipart_upload(bucket_name, file_id, upload_id, part_list)

        # Clean up multipart session data
        del multipart_sessions[file_id]

        # Insert the final metadata into MongoDB
        db.files.insert_one({
            "user_id": user_id,
            "file_id": file_id,
            "filename": filename,
            "bucket_name": bucket_name,
            "content_type": content_type,
            "size": total_size
        })

    except Exception as exc:
        print(f"Error while finalizing upload: {exc}")
        raise self.retry(exc=exc)
    
# Task to fetch the file from MinIO using file_id
@celery_app.task(bind=True)
def fetch_file(self, user_id, file_id):
    try:
        client = MongoClient("mongodb://root:example@file_storage_microservice_mongodb:27017/", maxPoolSize=10, minPoolSize=1)
        db = client["filedb"]

        file_meta = db.files.find_one({"user_id": user_id, "file_id": file_id})
        if not file_meta:
            return {"status": "error", "message": "File not found"}

        minio_client = Minio(
            "file-storage-microservice-minio:9000", 
            access_key="root",  
            secret_key="root1234", 
            secure=False  
        )

        # Return file details for streaming
        return {
            "status": "success",
            "bucket_name": file_meta["bucket_name"],
            "object_name": file_id,
            "filename": file_meta["filename"],
            "content_type": file_meta["content_type"]
        }
    except Exception as exc:
        print(f"Error fetching file: {exc}")
        return {"status": "failure", "error": str(exc)}

# Task to update the filename in MongoDB
@celery_app.task(bind=True)
def update_file_name_in_mongodb(self, user_id, file_id, new_filename):
    try:
        # Set up MongoDB client
        client = MongoClient("mongodb://root:example@file_storage_microservice_mongodb:27017/")
        db = client["filedb"]

        # Update the filename in MongoDB
        result = db.files.update_one(
            {"user_id": user_id, "file_id": file_id},
            {"$set": {"filename": new_filename}}
        )

        if result.modified_count == 0:
            return {"status": "error", "message": "File not found or filename not updated"}
        
        return {"status": "success", "file_id": file_id, "new_filename": new_filename}

    except PyMongoError as exc:
        print(f"Failed to update file metadata in MongoDB: {exc}")
        return {"status": "failure", "error": str(exc)}
    except Exception as exc:
        print(f"An unexpected error occurred: {exc}")
        return {"status": "failure", "error": str(exc)}

# Task to delete a specific file from MinIO and MongoDB
@celery_app.task(bind=True)
def delete_file(self, user_id, file_id):
    try:
        client = MongoClient("mongodb://root:example@file_storage_microservice_mongodb:27017/", maxPoolSize=10, minPoolSize=1)
        db = client["filedb"]

        file_meta = db.files.find_one({"user_id": user_id, "file_id": file_id})
        if not file_meta:
            return {"status": "error", "message": "File not found"}

        minio_client = Minio(
            "file-storage-microservice-minio:9000", 
            access_key="root",  
            secret_key="root1234", 
            secure=False  
        )

        # Use file_id as the object name to delete the file from MinIO
        minio_client.remove_object(file_meta["bucket_name"], file_id)

        # Delete the metadata from MongoDB
        db.files.delete_one({"user_id": user_id, "file_id": file_id})

        return {"status": "success", "message": "File deleted"}
    except S3Error as exc:
        print(f"Failed to delete file from MinIO: {exc}")
        return {"status": "failure", "error": str(exc)}
    except PyMongoError as exc:
        print(f"Failed to delete file metadata from MongoDB: {exc}")
        return {"status": "failure", "error": str(exc)}
    except Exception as exc:
        print(f"An unexpected error occurred: {exc}")
        return {"status": "failure", "error": str(exc)}

# Task to delete all files for a user from MinIO and MongoDB
@celery_app.task(bind=True)
def delete_all_user_files(self, user_id):
    try:
        client = MongoClient("mongodb://root:example@file_storage_microservice_mongodb:27017/", maxPoolSize=10, minPoolSize=1)
        db = client["filedb"]

        minio_client = Minio(
            "file-storage-microservice-minio:9000", 
            access_key="root",  
            secret_key="root1234", 
            secure=False  
        )

        # Retrieve all files for the user
        files = db.files.find({"user_id": user_id})

        for file_meta in files:
            # Use file_id as the object name to delete the file from MinIO
            minio_client.remove_object(file_meta['bucket_name'], file_meta['file_id'])
        
        # Delete metadata for the user from MongoDB
        db.files.delete_many({"user_id": user_id})

        return {"status": "success", "message": f"All files for user {user_id} deleted"}
    except S3Error as exc:
        print(f"Failed to delete files from MinIO: {exc}")
        return {"status": "failure", "error": str(exc)}
    except PyMongoError as exc:
        print(f"Failed to delete file metadata from MongoDB: {exc}")
        return {"status": "failure", "error": str(exc)}
    except Exception as exc:
        print(f"An unexpected error occurred: {exc}")
        return {"status": "failure", "error": str(exc)}
