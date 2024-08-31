"use client"

import React, { useEffect, useState } from "react";
import "./FileCard.css";

const FileCard = ({ file, onRemove, onCompress }) => {
    const [preview, setPreview] = useState("");
    const [isCompressing, setIsCompressing] = useState(false);
    const [downloadUrl, setDownloadUrl] = useState("");
    const [isSavedToCloud, setIsSavedToCloud] = useState(false);
    const [compressionInfo, setCompressionInfo] = useState(null);

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
        formData.append("quality", quality);
    
        try {
            const response = await fetch("http://localhost:8080" + endpoint, {
                method: "POST",
                body: formData,
                credentials: "include",
                headers: {
                    'Authorization': `Bearer ${user.token}`,
                }
            });
    
            if (response.ok) {
                const { task_id } = await response.json();
    
                const ws = new WebSocket(`ws://localhost:8082/ws/compress/status/${task_id}?token=${user.token}`);
    
                ws.onmessage = async (event) => {
                    const message = JSON.parse(event.data);
    
                    if (message.state === "SUCCESS") {
                        const downloadResponse = await fetch(`http://localhost:8080/api/compress/downloadmedia/${task_id}`, {
                            credentials: "include",
                            headers: {
                                'Authorization': `Bearer ${user.token}`, 
                            }
                        });
                        const blob = await downloadResponse.blob();
                        const url = URL.createObjectURL(blob);
    
                        if (file.type.startsWith("video/")) {
                            generateVideoThumbnail(url);  
                        } else {
                            setPreview(url);
                        }
    
                        setDownloadUrl(url);
    
                        const originalSizeMB = (message.original_size / (1024 * 1024)).toFixed(2);
                        const compressedSizeMB = (message.compressed_size / (1024 * 1024)).toFixed(2);
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
        console.log("Saving to cloud...");
        setIsSavedToCloud(true);
    };
    
    const generateVideoThumbnail = (videoUrl) => {
        const video = document.createElement('video');
        video.src = videoUrl;
        video.crossOrigin = "anonymous"; 
    
        video.addEventListener('loadeddata', () => {
            video.currentTime = 2; 
        });
    
        video.addEventListener('seeked', () => {
            const canvas = document.createElement('canvas');
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    
            const imgDataUrl = canvas.toDataURL('image/png');
            setPreview(imgDataUrl);
        });
    };

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
