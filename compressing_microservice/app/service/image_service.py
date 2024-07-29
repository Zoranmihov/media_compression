import os
import io
from PIL import Image, UnidentifiedImageError

def compress_image_service(file_path: str, quality: int):
    with open(file_path, "rb") as file:
        image_data = file.read()
    original_size = len(image_data)
    
    image = open_image(file_path)
    if image is None:
        raise Exception("Invalid image file")

    output_io = io.BytesIO()
    format = image.format

    if format == "JPEG":
        save_image(image, output_io, img_quality=quality, format=format)
    elif format == "PNG":
        image = image.convert("P", palette=Image.ADAPTIVE, colors=256)
        image.save(output_io, format=format, optimize=True)
    elif format == "WEBP":
        image.save(output_io, format=format, quality=quality)
    else:
        raise Exception("Unsupported image format")

    optimized_data = output_io.getvalue()
    compressed_size = len(optimized_data)

    if compressed_size >= original_size:
        optimized_data = image_data
        compressed_size = original_size

    os.remove(file_path)
    return optimized_data, original_size, compressed_size

def open_image(img_path):
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
