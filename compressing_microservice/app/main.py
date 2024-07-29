from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import StreamingResponse
from service.image_service import compress_image_service
import io

app = FastAPI()

@app.post("/compress-image/")
async def compress_image(file: UploadFile = File(...), quality: int = 90):
    if not file:
        raise HTTPException(status_code=422, detail="No file provided")

    if quality < 50 or quality > 100:
        raise HTTPException(status_code=400, detail="Quality must be between 50 and 100")

    try:
        optimized_data, original_size, compressed_size = await compress_image_service(file, quality)

        # Calculate the percentage reduction
        size_reduction_percentage = ((original_size - compressed_size) / original_size) * 100

        headers = {
            "X-Original-Size": str(original_size),
            "X-Compressed-Size": str(compressed_size),
            "X-Size-Reduction": f"{size_reduction_percentage:.2f}%"
        }

        return StreamingResponse(io.BytesIO(optimized_data), media_type=file.content_type, headers=headers)

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error compressing image: {str(e)}")

@app.get("/")
def read_root():
    return {"message": "Welcome to the Image Compression API"}
