from PIL import Image, UnidentifiedImageError
import tempfile
import os
import io
import asyncio
from fastapi import UploadFile

async def compress_image_service(file: UploadFile, quality: int):
    image_data = await file.read()
    original_size = len(image_data)
    
    # Create a temporary file to save the uploaded image
    suffix = os.path.splitext(file.filename)[1]
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
        temp_file.write(image_data)
        temp_file_path = temp_file.name

    # Open the image using Pillow
    image = open_image(temp_file_path)
    if image is None:
        raise Exception("Invalid image file")

    # Create an in-memory byte buffer to save the compressed image
    output_io = io.BytesIO()

    # Determine the format of the image
    format = image.format
    if format not in ["JPEG", "PNG", "WEBP"]:
        raise Exception("Unsupported image format")

    # Save the image with the specified quality
    if format == "JPEG":
        save_image(image, output_io, img_quality=quality, format=format)
    elif format == "PNG":
        image = image.convert("P", palette=Image.ADAPTIVE, colors=256)
        image.save(output_io, format=format, optimize=True)
    elif format == "WEBP":
        image.save(output_io, format=format, quality=quality)

    # Read the compressed image
    optimized_data = output_io.getvalue()
    compressed_size = len(optimized_data)

    # If compressed size exceeds original, use the original image data
    if compressed_size >= original_size:
        optimized_data = image_data
        compressed_size = original_size

    # Cleanup temporary file
    os.remove(temp_file_path)

    return optimized_data, original_size, compressed_size

def open_image(img_path):
    """Open image in pillow"""
    try:
        img = Image.open(img_path)
    except UnidentifiedImageError as error:
        print(error)
        return None
    return img

def save_image(img, output_io, img_quality=80, format="JPEG"):
    """Save pillow img obj"""
    if img.mode in ("RGBA", "P"):
        img = img.convert("RGB")
    img.save(output_io, format=format, optimize=True, quality=img_quality)
