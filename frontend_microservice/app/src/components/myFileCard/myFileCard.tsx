"use client";

import React, { useState } from "react";
import "./MyFileCard.css";

const MyFileCard = ({ file }) => {
    const [isEditing, setIsEditing] = useState(false);
    const [newName, setNewName] = useState(file?.filename || "");
    const [filename, setFilename] = useState(file?.filename || ""); // State to reflect the updated name
    const [statusMessage, setStatusMessage] = useState("");
    const [isDeleted, setIsDeleted] = useState(false); // State to track deletion

    const formatFileSize = (size) => {
        if (!size) return "Unknown size";
        if (size >= 1e9) return `${(size / 1e9).toFixed(2)} GB`;
        if (size >= 1e6) return `${(size / 1e6).toFixed(2)} MB`;
        if (size >= 1e3) return `${(size / 1e3).toFixed(2)} KB`;
        return `${size} bytes`;
    };

    const handleNameChange = async () => {
        if (!newName.trim() || newName === filename) {
            console.error("New name is invalid or identical to the current name.");
            setStatusMessage("Invalid or unchanged name.");
            return;
        }

        try {
            const formData = new FormData();
            formData.append("file_id", file.file_id);
            formData.append("new_filename", newName.trim());

            const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/storage/updatefilename/`, {
                method: "PUT",
                body: formData,
            });

            if (response.ok) {
                const data = await response.json();
                if (data.status === "success" || data.status === "File name is being updated") {
                    console.log("Filename update initiated successfully:", data);

                    setFilename(newName.trim());
                    setStatusMessage("Filename updated successfully.");
                    setIsEditing(false);
                } else {
                    console.error("Failed to initiate filename update:", data.message);
                    setStatusMessage("Failed to update filename.");
                }
            } else {
                console.error("Failed to initiate filename update:", response.statusText);
                setStatusMessage("Server error while updating filename.");
            }
        } catch (error) {
            console.error("An error occurred while updating the filename:", error);
            setStatusMessage("An error occurred while updating the filename.");
        }
    };

    const handleDelete = async () => {
        try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/storage/deletefile/${file.file_id}`, {
                method: "DELETE",
            });
    
            if (response.ok) {
                const data = await response.json();
                if (data.message === "File deleted") {
                    console.log("File deleted successfully:", data);
    
                    setIsDeleted(true); // Mark the file as deleted to remove the card
                    setStatusMessage("File deleted successfully.");
                } else {
                    console.error("Failed to delete the file:", data.message);
                    setStatusMessage("Failed to delete the file.");
                }
            } else {
                console.error("Failed to delete the file:", response.statusText);
                setStatusMessage("Server error while deleting the file.");
            }
        } catch (error) {
            console.error("An error occurred while deleting the file:", error);
            setStatusMessage("An error occurred while deleting the file.");
        }
    };
    
    

    if (isDeleted) {
        return null; // Don't render the card if the file has been deleted
    }

    return (
        <div className="my-file-card" role="region" aria-labelledby={`file-${file?._id}`}>
            <div className="my-file-card-image-container">
                {file?.thumbnail_url ? (
                    <img
                        src={file.thumbnail_url}
                        alt={filename || "File Thumbnail"}
                        className="my-file-card-image"
                    />
                ) : (
                    <div className="placeholder-thumbnail">No Image</div>
                )}
            </div>
            <div className="my-file-card-info">
                <p id={`file-${file?._id}`}>File type: {file?.content_type?.split("/")[1]?.toUpperCase() || "Unknown"}</p>
                {isEditing ? (
                    <div className="my-file-card-edit">
                        <input
                            type="text"
                            value={newName}
                            onChange={(e) => setNewName(e.target.value)}
                            className="my-file-card-input"
                            aria-label="Edit file name"
                        />
                        <button className="my-file-card-button" onClick={handleNameChange}>
                            Save
                        </button>
                        <button
                            className="my-file-card-button cancel-button"
                            onClick={() => {
                                setIsEditing(false);
                                setNewName(filename);
                            }}
                        >
                            Cancel
                        </button>
                    </div>
                ) : (
                    <p>Name: {filename || "Untitled"}</p>
                )}
                <p>Size: {formatFileSize(file?.size)}</p>
                <p>Uploaded: {file?.upload_date ? new Date(file.upload_date).toLocaleString() : "Unknown"}</p>
                <div className="my-file-card-actions">
                    {!isEditing && (
                        <button className="my-file-card-button" onClick={() => setIsEditing(true)}>
                            Change Name
                        </button>
                    )}
                    <button className="my-file-card-button delete-button" onClick={handleDelete}>
                        Delete
                    </button>
                </div>
                {statusMessage && <p className="status-message">{statusMessage}</p>}
            </div>
        </div>
    );
};

export default MyFileCard;
