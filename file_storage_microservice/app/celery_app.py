from celery import Celery

celery_app = Celery(
    'file_storage_microservice',
    broker='redis://file_storage_microservice_redis:6379/0',
    backend='redis://file_storage_microservice_redis:6379/0'
)

celery_app.conf.update(
    task_serializer='json',
    accept_content=['json'],
    result_serializer='json',
    timezone='UTC',
    enable_utc=True,
    broker_connection_retry_on_startup=True
)
