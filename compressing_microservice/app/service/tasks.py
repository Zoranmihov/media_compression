from celery import Celery
from service.image_service import compress_image_service
from service.video_service import compress_video_service
import os

celery_app = Celery(
    'tasks',
    broker='redis://redis:6379/0',
    backend='redis://redis:6379/0'
)

celery_app.conf.update(
    result_expires=3600,
    worker_concurrency=2, 
    task_acks_late=True,
    worker_prefetch_multiplier=1
)

@celery_app.task
def compress_image_task(file_path: str, quality: int):
    optimized_data, original_size, compressed_size = compress_image_service(file_path, quality)
    compressed_file_path = file_path.replace('.tmp', f'_compressed.tmp')
    with open(compressed_file_path, 'wb') as f:
        f.write(optimized_data)
    return {
        "file_path": compressed_file_path,
        "original_file_path": file_path,
        "original_size": original_size,
        "compressed_size": compressed_size
    }

@celery_app.task
def compress_video_task(file_path: str, quality: int):
    optimized_data, original_size, compressed_size = compress_video_service(file_path, quality)
    compressed_file_path = file_path.replace('.tmp', f'_compressed.tmp')
    with open(compressed_file_path, 'wb') as f:
        f.write(optimized_data)
    return {
        "file_path": compressed_file_path,
        "original_file_path": file_path,
        "original_size": original_size,
        "compressed_size": compressed_size
    }

def delete_file(file_path: str):
    if os.path.exists(file_path):
        os.remove(file_path)
