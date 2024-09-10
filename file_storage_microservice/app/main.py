from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Request, WebSocket, WebSocketDisconnect
from fastapi.responses import StreamingResponse
from tasks import process_chunk, finalize_upload, fetch_file, update_file_name_in_mongodb, delete_file, delete_all_user_files
import io
from model.user_model import create_user_collection_with_schema
import uuid
from minio import Minio



app = FastAPI(
    title="file_storage_microservice",
    description="Microservice responsible for storing and returning the compressed media",
    version="1.0.0",
    docs_url="/admin/doc",
    redoc_url=None
)

@app.on_event("startup")
async def startup_event():
    create_user_collection_with_schema()

# WebSockets

# Dictionary to store WebSocket connections per user
active_connections = {}

@app.websocket("/ws/storage/upload/{user_id}/{file_id}")
async def websocket_upload(websocket: WebSocket, user_id: str, file_id: str):
    await websocket.accept()
    
    # Add the WebSocket connection for the specific user
    if user_id not in active_connections:
        active_connections[user_id] = []
    active_connections[user_id].append(websocket)
    
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        # Remove WebSocket on disconnect
        active_connections[user_id].remove(websocket)
        if not active_connections[user_id]:  # Clean up if no connections are left
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
async def upload_file(user_id: str = Form(...), file: UploadFile = File(...)):
    file_id = str(uuid.uuid4())
    total_size = 0
    part_number = 1
    
    try:
        # Stream the file content in chunks
        while True:
            chunk = await file.read(1024 * 1024)  # Read in 1MB chunks
            if not chunk:
                break
            total_size += len(chunk)
            
            # Notify specific user's WebSocket connections about chunk upload
            if user_id in active_connections:
                for ws in active_connections[user_id]:
                    await ws.send_json({"message": f"Uploading part {part_number}", "file_id": file_id})
                    
            process_chunk.delay(user_id, file_id, file.filename, chunk, file.content_type, part_number)
            part_number += 1
        
        # Notify when upload is complete
        finalize_upload.delay(user_id, file_id, file.filename, total_size, file.content_type)
        if user_id in active_connections:
            for ws in active_connections[user_id]:
                await ws.send_json({"message": "Upload completed", "file_id": file_id})
        
        return {"file_id": file_id, "status": "File is being processed"}
    
    except Exception as e:
        if user_id in active_connections:
            for ws in active_connections[user_id]:
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
            for chunk in response.stream(1024 * 1024):  # Stream in 1MB chunks
                yield chunk

        return StreamingResponse(
            file_stream(),
            media_type=result["content_type"],
            headers={"Content-Disposition": f"attachment; filename={result['filename']}"}
        )
    else:
        raise HTTPException(status_code=404, detail=result["message"])
    
@app.put("/api/storage/updatefilename/")
async def update_file_name_endpoint(user_id: str = Form(...), file_id: str = Form(...), new_filename: str = Form(...)):
    try:
        # Trigger the Celery task to update the file name in MongoDB
        task = update_file_name_in_mongodb.delay(user_id, file_id, new_filename)

        return {"task_id": task.id, "status": "File name is being updated", "file_id": file_id, "new_filename": new_filename}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"File name update failed: {str(e)}")

@app.delete("/api/storage/deletefile/{file_id}")
async def delete_specific_file(user_id: str, file_id: str):
    task = delete_file.delay(user_id, file_id)
    result = task.get(timeout=10)

    if result["status"] == "success":
        return {"message": result["message"]}
    else:
        raise HTTPException(status_code=404, detail=result["message"])

@app.delete("/api/storage/deleteuserfiles/")
async def delete_user_files(user_id: str):
    try:
        # Start the Celery task for file deletion
        task = delete_all_user_files.delay(user_id)

        # Notify the user via WebSocket
        if user_id in active_connections:
            for ws in active_connections[user_id]:
                await ws.send_json({"message": "File deletion in progress", "user_id": user_id})

        result = task.get(timeout=10)

        if result["status"] == "success":
            if user_id in active_connections:
                for ws in active_connections[user_id]:
                    await ws.send_json({"message": "All files deleted successfully", "user_id": user_id})
            return {"message": result["message"]}
        else:
            if user_id in active_connections:
                for ws in active_connections[user_id]:
                    await ws.send_json({"message": f"Deletion failed: {result['message']}", "user_id": user_id})
            raise HTTPException(status_code=404, detail=result["message"])

    except Exception as e:
        if user_id in active_connections:
            for ws in active_connections[user_id]:
                await ws.send_json({"message": f"Deletion failed: {str(e)}", "user_id": user_id})
        raise HTTPException(status_code=500, detail=f"Deletion failed: {str(e)}")

