from celery_app import celery_app
from celery import chord
from pymongo import MongoClient
from minio import Minio
from minio.error import S3Error
import io
from pymongo.errors import PyMongoError
import redis
from datetime import datetime  
from pymediainfo import MediaInfo  
import tempfile  


# Track parts uploaded in Redis
redis_client = redis.Redis(host="file_storage_microservice_redis", port=6379, db=0)

# Process and upload each chunk to MinIO
@celery_app.task(bind=True, autoretry_for=(S3Error,), retry_backoff=True, max_retries=3)
def process_chunk(self, user_id, file_id, filename, chunk, content_type, part_number, total_parts):
    try:
        minio_client = Minio(
            "file-storage-microservice-minio:9000",
            access_key="root",
            secret_key="root1234",
            secure=False
        )
        bucket_name = f"user-bucket-{user_id}"

        if not minio_client.bucket_exists(bucket_name):
            minio_client.make_bucket(bucket_name)

        # Upload the chunk
        chunk_name = f"{file_id}_part_{part_number}"
        chunk_stream = io.BytesIO(chunk)
        minio_client.put_object(
            bucket_name,
            chunk_name,
            data=chunk_stream,
            length=len(chunk),
            content_type=content_type
        )

        # Track uploaded parts in Redis
        redis_client.incr(f"{file_id}_parts_uploaded")

        return f"Chunk {part_number} uploaded for {file_id}"

    except Exception as exc:
        print(f"Error processing chunk {part_number}: {exc}")
        raise self.retry(exc=exc)

@celery_app.task(bind=True, autoretry_for=(S3Error,), retry_backoff=True, max_retries=3)
def finalize_upload(self, result, user_id, file_id, filename, total_parts, content_type, thumbnail_url=None):
    try:
        minio_client = Minio(
            "file-storage-microservice-minio:9000",
            access_key="root",
            secret_key="root1234",
            secure=False
        )
        bucket_name = f"user-bucket-{user_id}"

        # Check Redis state
        parts_uploaded = int(redis_client.get(f"{file_id}_parts_uploaded") or 0)
        print(f"Redis state - Parts uploaded: {parts_uploaded}, Expected: {total_parts}")
        if parts_uploaded != total_parts:
            raise Exception(f"Not all parts uploaded: {parts_uploaded}/{total_parts}")

        # Combine parts into a single file
        sources = [f"{file_id}_part_{i}" for i in range(1, total_parts + 1)]
        combined_stream = io.BytesIO(b"".join([minio_client.get_object(bucket_name, src).read() for src in sources]))
        file_size = combined_stream.getbuffer().nbytes

        # Upload final file to MinIO
        minio_client.put_object(
            bucket_name,
            file_id,
            data=combined_stream,
            length=file_size,
            content_type=content_type
        )

        # Clean up chunks
        for src in sources:
            try:
                minio_client.remove_object(bucket_name, src)
                print(f"Deleted chunk: {src}")
            except Exception as exc:
                print(f"Error removing chunk {src}: {exc}")

        # Store metadata and thumbnail in MongoDB
        try:
            client = MongoClient("mongodb://root:example@file_storage_microservice_mongodb:27017/")
            db = client["filedb"]
            metadata = {
                "user_id": user_id,
                "file_id": file_id,
                "filename": filename,
                "bucket_name": bucket_name,
                "content_type": content_type,
                "size": file_size,
                "thumbnail_url": thumbnail_url, 
                "upload_date": datetime.utcnow()
            }
            print("Inserting metadata into MongoDB:", metadata)
            db.files.insert_one(metadata)
            print(f"Metadata for file_id {file_id} inserted successfully into MongoDB.")
        except PyMongoError as mongo_exc:
            print(f"MongoDB insertion error: {mongo_exc}")
            raise

        # Clean up Redis tracking
        redis_client.delete(f"{file_id}_parts_uploaded")
        print(f"Redis tracking for file_id {file_id} cleaned up.")

        return f"Final file {file_id} uploaded and parts cleaned up"

    except Exception as exc:
        print(f"Error finalizing upload for file_id {file_id}: {exc}")
        raise self.retry(exc=exc)

# Fetch file from MinIO and stream it
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

@celery_app.task(bind=True)
def fetch_user_files_task(self, user_id):
    try:
        # MongoDB connection
        mongo_client = MongoClient("mongodb://root:example@file_storage_microservice_mongodb:27017/")
        db = mongo_client["filedb"]
        files_collection = db["files"]

        # Query MongoDB for the user's files
        files = files_collection.find({"user_id": user_id})

        # Convert the MongoDB cursor to a list of dictionaries
        file_list = []
        for file in files:
            file["_id"] = str(file["_id"]) 
            file_list.append(file)

        return file_list

    except Exception as e:
        # Log and re-raise the exception for proper error handling
        raise self.retry(exc=e, countdown=5, max_retries=3)