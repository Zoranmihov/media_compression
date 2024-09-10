from pymongo import MongoClient

def create_user_collection_with_schema():
    client = MongoClient("mongodb://root:example@file_storage_microservice_mongodb:27017/")
    db = client["filedb"]

    user_schema = {
        "$jsonSchema": {
            "bsonType": "object",
            "required": ["user_id", "total_storage_used", "files"],
            "properties": {
                "user_id": {
                    "bsonType": "string",
                    "description": "must be a string and is required"
                },
                "total_storage_used": {
                    "bsonType": "int",
                    "minimum": 0,
                    "description": "must be an integer greater than or equal to 0"
                },
                "files": {
                    "bsonType": "array",
                    "items": {
                        "bsonType": "object",
                        "required": ["file_id", "filename", "size", "content_type"],
                        "properties": {
                            "file_id": {
                                "bsonType": "string",
                                "description": "must be a string and is required"
                            },
                            "filename": {
                                "bsonType": "string",
                                "description": "must be a string and is required"
                            },
                            "size": {
                                "bsonType": "int",
                                "minimum": 0,
                                "description": "must be an integer greater than or equal to 0"
                            },
                            "content_type": {
                                "bsonType": "string",
                                "description": "must be a string and is required"
                            }
                        }
                    }
                }
            }
        }
    }

    try:
        db.create_collection("users", validator=user_schema)
        print("User collection created with schema validation")
    except Exception as e:
        if "already exists" in str(e):
            db.command("collMod", "users", validator=user_schema)
            print("User collection schema updated")
        else:
            print("Error creating collection: ", e)

if __name__ == "__main__":
    create_user_collection_with_schema()
