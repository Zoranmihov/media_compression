"use client"

import { useState } from "react";
import "./FileUpload.css";
import FileCard from "../FileCard/FileCard";

const FileUpload = () => {
    const [files, setFiles] = useState([]);

    const updateQuallityValue = (value) => {
        document.getElementById("quallity-value").innerHTML = `Quallity: ${value}`;
    };

    const handleFileUpload = (event) => {
        const allowedTypes = ['image/jpeg', 'image/png', 'video/mp4', 'video/quicktime'];
        const selectedFiles = Array.from(event.target.files);
        const isValid = selectedFiles.every(file => allowedTypes.includes(file.type));

        if (!isValid) {
            alert('One or more files have an unsupported format. Please upload only png, jpg, mp4, or mov files.');
        } else {
            setFiles(selectedFiles);
        }
    };

    const handleRemoveFile = (fileToRemove) => {
        setFiles(files.filter(file => file !== fileToRemove));
    };

    return (
        <div className="file-upload-container">
            <div className="file-upload-slider">
                <input
                    onChange={(e) => updateQuallityValue(e.target.value)}
                    type="range"
                    min="50"
                    max="100"
                    defaultValue={50}
                    id="file-upload-slider"
                />
                <p id="quallity-value">Quallity: 50</p>
            </div>
            {files.length <= 0 ? (
                <div className="file-upload-inputs-container">
                    <input
                        type="file"
                        id="media-file-upload"
                        multiple
                        onChange={handleFileUpload}
                    />
                    <button
                        onClick={() => document.getElementById("media-file-upload").click()}
                        className="file-upload-btn form-submit"
                    >
                        Upload files
                    </button>
                </div>
            ) : (
                    <div className="file-cards">
                        {files.map((file, index) => (
                           <FileCard 
                               key={index} 
                               file={file} 
                               onRemove={handleRemoveFile}
                           />
                        ))}
                    </div>
            )}
        </div>
    );
};

export default FileUpload;
