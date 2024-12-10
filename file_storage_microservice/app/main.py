from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Request, WebSocket, WebSocketDisconnect, Header
from fastapi.responses import StreamingResponse
from tasks import process_chunk, finalize_upload, fetch_file, update_file_name_in_mongodb, delete_file, delete_all_user_files, fetch_user_files_task
import io
import uuid
from minio import Minio
from celery import chord
from celery.result import AsyncResult
import asyncio


app = FastAPI(
    title="file_storage_microservice",
    description="Microservice responsible for storing and returning the compressed media",
    version="1.0.0",
    docs_url="/admin/doc",
    redoc_url=None
)

# WebSockets

# WebSocket connections
active_connections = {}

@app.websocket("/ws/storage/upload/{user_id}/{file_id}")
async def websocket_upload(websocket: WebSocket, user_id: str, file_id: str):
    await websocket.accept()

    # Store the WebSocket connection for the specific user
    if user_id not in active_connections:
        active_connections[user_id] = []
    active_connections[user_id].append(websocket)

    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        # Remove WebSocket on disconnect
        active_connections[user_id].remove(websocket)
        if not active_connections[user_id]:
            del active_connections[user_id]

@app.websocket("/ws/storage/deletefiles/{user_id}")
async def websocket_delete_files(websocket: WebSocket, user_id: str):
    await websocket.accept()

    if user_id not in active_connections:
        active_connections[user_id] = []
    active_connections[user_id].append(websocket)

    try:
        while True:
            await websocket.receive_text()  # Keeps connection open for updates
    except WebSocketDisconnect:
        # Remove WebSocket on disconnect
        active_connections[user_id].remove(websocket)
        if not active_connections[user_id]:
            del active_connections[user_id]

#Rest API
@app.post("/api/storage/savefile/")
async def upload_file(
    file: UploadFile = File(...),
    thumbnail_url: str = Form(None), 
    UserId: str = Header(None), 
):
    if not UserId:
        raise HTTPException(status_code=400, detail="UserId header is missing.")

    file_id = str(uuid.uuid4())
    total_size = 0
    part_number = 1
    total_parts = 0
    tasks = [] 

    # Start with a default chunk size
    initial_chunk_size = 1024 * 1024 

    try:
        while True:
            # Read chunks of 1MB, and dynamically adjust based on previous chunk sizes
            chunk = await file.read(initial_chunk_size)
            if not chunk:
                break

            total_size += len(chunk)
            total_parts += 1

            # Dynamically adjust chunk size
            if total_size > 10 * 1024 * 1024: 
                initial_chunk_size = 1024 * 1024 * 2 

            # Notify WebSocket connections about chunk upload
            if UserId in active_connections:
                for ws in active_connections[UserId]:
                    await ws.send_json({"message": f"Uploading part {part_number}", "file_id": file_id})

            # Add chunk upload tasks to the list
            tasks.append(process_chunk.s(UserId, file_id, file.filename, chunk, file.content_type, part_number, total_parts))
            part_number += 1

        # Finalize task
        task_group = chord(tasks)(
            finalize_upload.s(UserId, file_id, file.filename, total_parts, file.content_type, thumbnail_url)
        )

        # Background task to check Celery task state and notify WebSocket
        @app.on_event("startup")
        async def check_task_status():
            while True:
                result = AsyncResult(task_group.id)
                if result.ready():
                    if UserId in active_connections:
                        for ws in active_connections[UserId]:
                            await ws.send_json({"message": "Upload complete", "file_id": file_id})
                    break
                await asyncio.sleep(2)  

        return {"file_id": file_id, "status": "File is being processed"}

    except Exception as e:
        if UserId in active_connections:
            for ws in active_connections[UserId]:
                await ws.send_json({"message": f"Upload failed: {str(e)}", "file_id": file_id})
        raise HTTPException(status_code=500, detail=f"File upload failed: {str(e)}")
    
@app.get("/api/storage/getfile/{file_id}")
async def get_file(user_id: str, file_id: str, request: Request):
    task = fetch_file.delay(user_id, file_id)
    result = task.get(timeout=10)

    if result["status"] == "success":
        minio_client = Minio(
            "file-storage-microservice-minio:9000", 
            access_key="root",  
            secret_key="root1234", 
            secure=False  
        )

        # Stream file using range requests
        def file_stream():
            response = minio_client.get_object(result["bucket_name"], result["object_name"])
            for chunk in response.stream(1024 * 1024): 
                yield chunk

        return StreamingResponse(
            file_stream(),
            media_type=result["content_type"],
            headers={"Content-Disposition": f"attachment; filename={result['filename']}"}
        )
    else:
        raise HTTPException(status_code=404, detail=result["message"])
    
@app.put("/api/storage/updatefilename/")
async def update_file_name_endpoint(
    file_id: str = Form(...),
    new_filename: str = Form(...),
    UserId: str = Header(None) 
):
    if not UserId:
        raise HTTPException(status_code=400, detail="UserId header is missing.")

    try:
        # Trigger the Celery task to update the file name in MongoDB
        task = update_file_name_in_mongodb.delay(UserId, file_id, new_filename)

        return {
            "task_id": task.id,
            "status": "File name is being updated",
            "file_id": file_id,
            "new_filename": new_filename,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"File name update failed: {str(e)}")
    
@app.delete("/api/storage/deletefile/{file_id}")
async def delete_specific_file(file_id: str, UserId: str = Header(None)):
    if not UserId:
        raise HTTPException(status_code=400, detail="UserId header is missing.")

    try:
        # Trigger the Celery task to delete the file
        task = delete_file.delay(UserId, file_id)

        # Wait for the task to complete and get the result
        result = task.get(timeout=10)

        if result["status"] == "success":
            return {
                "task_id": task.id,
                "status": "File deletion is successful",
                "file_id": file_id,
                "message": result["message"],
            }
        else:
            raise HTTPException(status_code=404, detail=result["message"])
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"An error occurred while deleting the file: {str(e)}")
 
@app.delete("/api/storage/deleteuserfiles/")
async def delete_user_files(UserId: str = Header(None)):
    if not UserId:
        raise HTTPException(status_code=400, detail="UserId header is missing.")

    try:
        # Trigger the Celery task to delete all user files
        task = delete_all_user_files.delay(UserId)

        # Notify the user via WebSocket that the deletion is in progress
        if UserId in active_connections:
            for ws in active_connections[UserId]:
                await ws.send_json({"message": "File deletion started", "user_id": UserId})

        # Poll task status in a separate coroutine to avoid blocking
        async def poll_task():
            while not task.ready():
                if UserId in active_connections:
                    for ws in active_connections[UserId]:
                        await ws.send_json({"message": "File deletion in progress", "user_id": UserId})
                await asyncio.sleep(2)
            
            result = task.get()
            if UserId in active_connections:
                message = (
                    {"message": "All files deleted successfully"} if result["status"] == "success"
                    else {"message": f"Deletion failed: {result['error']}"}
                )
                for ws in active_connections[UserId]:
                    await ws.send_json(message)

        asyncio.create_task(poll_task())
        return {"message": "File deletion task initiated"}

    except Exception as e:
        if UserId in active_connections:
            for ws in active_connections[UserId]:
                await ws.send_json({"message": f"Deletion failed: {str(e)}", "user_id": UserId})
        raise HTTPException(status_code=500, detail=f"Deletion failed: {str(e)}")

@app.get("/api/storage/getuserfiles/", response_model=list)
async def get_user_files(userid: str = Header(...)):
    try:
        # Trigger Celery task to fetch user files
        task = fetch_user_files_task.delay(userid)
        result = AsyncResult(task.id)
        result_data = result.get(timeout=10) 

        if result.state == "SUCCESS":
            return result_data
        elif result.state == "FAILURE":
            raise HTTPException(status_code=500, detail=f"Task failed: {result.info}")
        else:
            raise HTTPException(status_code=202, detail="Task is still in progress")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching user files: {str(e)}")