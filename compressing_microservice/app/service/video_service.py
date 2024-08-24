import os
import ffmpeg

def map_quality_to_crf(quality):
    return max(0, min(51, int((100 - quality) * 51 / 50)))

def compress_video_service(file_path: str, quality: int):
    with open(file_path, "rb") as file:
        video_data = file.read()
    original_size = len(video_data)

    # Map the quality value to CRF
    crf = map_quality_to_crf(quality)

    # Compress the video using ffmpeg
    compressed_video_path = file_path.replace(os.path.splitext(file_path)[1], f"_compressed{os.path.splitext(file_path)[1]}")
    
    try:
        ffmpeg.input(file_path).output(
            compressed_video_path,
            vcodec='libx264',
            crf=crf,
            preset='slow',
            acodec='aac',
            audio_bitrate='128k',
            movflags='faststart'  
        ).run(quiet=True)  

      
        with open(compressed_video_path, "rb") as compressed_file:
            optimized_data = compressed_file.read()
        compressed_size = len(optimized_data)

        # If the compressed size is larger than the original, use the original data
        if compressed_size >= original_size:
            optimized_data = video_data
            compressed_size = original_size

       
        os.remove(file_path)
        os.remove(compressed_video_path)

        return optimized_data, original_size, compressed_size
    except Exception as e:
       
        os.remove(file_path)
        if os.path.exists(compressed_video_path):
            os.remove(compressed_video_path)
        raise e
