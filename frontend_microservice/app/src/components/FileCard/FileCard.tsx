"use client"

import React, { useEffect, useState } from "react";
import "./FileCard.css";

const FileCard = ({ file, onRemove, onCompress }) => {
    const [preview, setPreview] = useState("");
    const [isCompressing, setIsCompressing] = useState(false);
    const [downloadUrl, setDownloadUrl] = useState("");
    const [isSavedToCloud, setIsSavedToCloud] = useState(false); // Track cloud save status
    const [compressionInfo, setCompressionInfo] = useState(null); // Track compression info

    useEffect(() => {
        const generatePreview = () => {
            if (file.type.startsWith("image/")) {
                setPreview(URL.createObjectURL(file));
            } else if (file.type.startsWith("video/")) {
                const video = document.createElement("video");
                video.src = URL.createObjectURL(file);
                video.onloadeddata = () => {
                    video.currentTime = 1;
                };
                video.onseeked = () => {
                    const canvas = document.createElement("canvas");
                    canvas.width = video.videoWidth;
                    canvas.height = video.videoHeight;
                    canvas.getContext("2d").drawImage(video, 0, 0, canvas.width, canvas.height);
                    setPreview(canvas.toDataURL("image/png"));
                };
            }
        };

        generatePreview();

        // Cleanup function to revoke the object URL when the component unmounts or the file changes
        return () => {
            if (preview) {
                URL.revokeObjectURL(preview);
            }
        };
    }, [file]);

    const handleCompress = async () => {
        setIsCompressing(true);
        let quality = document.getElementById("file-upload-slider")?.value;
    
        const endpoint = file.type.startsWith("image/") ? "/api/compress/image/" : "/api/compress/video/";
        const formData = new FormData();
        formData.append("file", file);
        formData.append("quality", quality); // You can adjust the quality dynamically
    
        try {
            const response = await fetch("http://localhost:8082" + endpoint, {
                method: "POST",
                body: formData,
            });
    
            if (response.ok) {
                const { task_id } = await response.json();
    
                // WebSocket connection to listen for status updates
                const ws = new WebSocket(`http://localhost:8082/ws/compress/status/${task_id}`);
    
                ws.onmessage = async (event) => {
                    const message = JSON.parse(event.data);
    
                    if (message.state === "SUCCESS") {
                        const downloadResponse = await fetch(`http://localhost:8082/api/compress/downloadmedia/${task_id}`);
                        const blob = await downloadResponse.blob();
                        const url = URL.createObjectURL(blob);
    
                        if (file.type.startsWith("video/")) {
                            generateVideoThumbnail(url);  // Generate thumbnail for video
                        } else {
                            setPreview(url); // Update the preview with the new image file
                        }
    
                        setDownloadUrl(url);
                        
                        const originalSizeMB = (message.original_size / (1024 * 1024)).toFixed(2); // Convert bytes to MB
                        const compressedSizeMB = (message.compressed_size / (1024 * 1024)).toFixed(2); // Convert bytes to MB
                        const sizeReduction = (((message.original_size - message.compressed_size) / message.original_size) * 100).toFixed(2);

                        setCompressionInfo({
                            originalSizeMB,
                            compressedSizeMB,
                            sizeReduction,
                        });

                        ws.close();
                        setIsCompressing(false);
                    } else if (message.state === "FAILURE") {
                        console.error("Compression failed:", message.status);
                        ws.close();
                        setIsCompressing(false);
                    }
                };
            } else {
                console.error("Failed to start compression:", response.statusText);
                setIsCompressing(false);
            }
        } catch (error) {
            console.error("An error occurred during compression:", error);
            setIsCompressing(false);
        }
    };

    const handleSaveToCloud = () => {
        // Implement your save to cloud logic here
        console.log("Saving to cloud...");
        setIsSavedToCloud(true); // Update state to indicate the file has been saved to the cloud
    };
    
    const generateVideoThumbnail = (videoUrl) => {
        const video = document.createElement('video');
        video.src = videoUrl;
        video.crossOrigin = "anonymous"; // Ensure the video is accessible for canvas rendering
    
        video.addEventListener('loadeddata', () => {
            video.currentTime = 2; // Capture the thumbnail at the 2-second mark
        });
    
        video.addEventListener('seeked', () => {
            const canvas = document.createElement('canvas');
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    
            const imgDataUrl = canvas.toDataURL('image/png');
            setPreview(imgDataUrl); // Set the thumbnail as the preview image
        });
    };

    // Helper function to format file size
    const formatFileSize = (size) => {
        if (size >= 1073741824) { 
            return `${(size / 1073741824).toFixed(2)} GB`;
        } else if (size >= 1048576) { 
            return `${(size / 1048576).toFixed(2)} MB`;
        } else if (size >= 1024) { 
            return `${(size / 1024).toFixed(2)} KB`;
        } else {
            return `${size} bytes`;
        }
    };

    // Function to remove the file extension
    const removeFileExtension = (fileName) => {
        return fileName.replace(/\.[^/.]+$/, "");
    };
    
    const truncatedFileName = removeFileExtension(file.name).length > 20 
        ? removeFileExtension(file.name).substring(0, 17) + "..." 
        : removeFileExtension(file.name);

    return (
        <div className="file-card">
            <button className="file-card-remove" onClick={() => onRemove(file)} disabled={isCompressing}>Remove</button>
            <div className="file-card-image-container">
                {preview && <img src={preview} alt={file.name} className="file-card-image" />}
            </div>
            <div className="file-card-info">
                <p>File type: {file.type.split('/')[1].toUpperCase()}</p>
                <p>Name: {truncatedFileName}</p>
                <p>Size: {formatFileSize(file.size)}</p>
                
                {!downloadUrl && (
                    <button className="file-card-button file-card-compress" onClick={handleCompress} disabled={isCompressing}>
                        {isCompressing ? "Compressing..." : "Compress"}
                    </button>
                )}

                {downloadUrl && (
                    <>
                        <button 
                            className="file-card-button file-card-download" 
                            onClick={() => {
                                const a = document.createElement('a');
                                a.href = downloadUrl;
                                a.download = file.name;
                                a.click();
                            }}
                        >
                            Download
                        </button>

                        {!isSavedToCloud && (
                            <button className="file-card-button file-card-save-cloud" onClick={handleSaveToCloud}>
                                Save to Cloud
                            </button>
                        )}

                        {compressionInfo && (
                            <p>New size: {compressionInfo.compressedSizeMB}MB ({compressionInfo.sizeReduction}% less)</p>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default FileCard;
