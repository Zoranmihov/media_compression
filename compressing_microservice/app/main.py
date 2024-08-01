from fastapi import FastAPI, UploadFile, File, HTTPException, WebSocket, WebSocketDisconnect, BackgroundTasks
from fastapi.responses import JSONResponse, FileResponse
from celery.result import AsyncResult
from service.tasks import compress_image_task, compress_video_task, celery_app, delete_file
import os
import tempfile
import asyncio

app = FastAPI(
    title="compressing-microservice",
    description="Microservice responsible for compressing and returning the compressed media",
    version="1.0.0",
    docs_url="/admin/doc",  # Custom path for Swagger UI
    redoc_url=None  # Default path for ReDoc
)

SHARED_DATA_DIR = '/shared_data/'

def save_file_temp(file: UploadFile):
    os.makedirs(SHARED_DATA_DIR, exist_ok=True)
    suffix = os.path.splitext(file.filename)[1]
    temp_file_path = tempfile.NamedTemporaryFile(delete=False, suffix=suffix, dir=SHARED_DATA_DIR).name
    with open(temp_file_path, 'wb') as temp_file:
        for chunk in iter(lambda: file.file.read(1024 * 1024), b''):
            temp_file.write(chunk)
    return temp_file_path


@app.post("/api/compress/image/")
async def compress_image(file: UploadFile = File(...), quality: int = 90):
    if not file:
        raise HTTPException(status_code=422, detail="No file provided")
    if quality < 50 or quality > 100:
        raise HTTPException(status_code=400, detail="Quality must be between 50 and 100")

    temp_file_path = save_file_temp(file)
    task = compress_image_task.apply_async(args=[temp_file_path, quality])
    return {"task_id": task.id, "status": "Processing"}

@app.post("/api/compress/video/")
async def compress_video(file: UploadFile = File(...), quality: int = 90):
    if not file:
        raise HTTPException(status_code=422, detail="No file provided")
    if quality < 50 or quality > 100:
        raise HTTPException(status_code=400, detail="Quality must be between 50 and 100")

    temp_file_path = save_file_temp(file)
    task = compress_video_task.apply_async(args=[temp_file_path, quality])
    return {"task_id": task.id, "status": "Processing"}

@app.get("/api/compress/task-status/{task_id}")
async def get_task_status(task_id: str):
    task_result = AsyncResult(task_id, app=celery_app)
    if task_result.state == 'PENDING':
        return {"state": task_result.state, "status": "Pending..."}
    elif task_result.state == 'SUCCESS':
        result = task_result.result
        return {
            "state": task_result.state,
            "original_size": result["original_size"],
            "compressed_size": result["compressed_size"],
            "download_url": f"/api/download/{task_id}"
        }
    elif task_result.state == 'FAILURE':
        return {"state": task_result.state, "status": str(task_result.info)}
    else:
        return {"state": task_result.state, "status": str(task_result.info)}

@app.get("/api/compress/downloadmedia/{task_id}")
async def download_file(task_id: str, background_tasks: BackgroundTasks):
    task_result = AsyncResult(task_id, app=celery_app)
    if task_result.state == 'SUCCESS':
        result = task_result.result
        file_path = result["file_path"]
        original_file_path = result["original_file_path"]
        
        background_tasks.add_task(delete_file, file_path)
        background_tasks.add_task(delete_file, original_file_path)
        
        return FileResponse(path=file_path, filename=os.path.basename(file_path))
    else:
        raise HTTPException(status_code=400, detail="Task not completed or failed.")

@app.websocket("/ws/compress/status/{task_id}")
async def websocket_endpoint(websocket: WebSocket, task_id: str):
    await websocket.accept()
    try:
        task_result = AsyncResult(task_id, app=celery_app)
        while task_result.state not in ["SUCCESS", "FAILURE"]:
            await websocket.send_json({"state": task_result.state})
            await asyncio.sleep(1)
            task_result = AsyncResult(task_id, app=celery_app)

        if task_result.state == 'SUCCESS':
            result = task_result.result
            await websocket.send_json({
                "state": task_result.state,
                "original_size": result["original_size"],
                "compressed_size": result["compressed_size"],
                "download_url": f"/api/download/{task_id}"
            })
        elif task_result.state == 'FAILURE':
            await websocket.send_json({"state": task_result.state, "status": str(task_result.info)})
    except WebSocketDisconnect:
        pass
