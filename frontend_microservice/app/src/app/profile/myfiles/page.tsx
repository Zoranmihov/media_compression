"use client";

import { requireAuth } from "@/utils/routeGuards";
import { useUser } from "@/context/UserContext";
import { useEffect, useState } from "react";
import MyFileCard from "@/components/myFileCard/myFileCard";
import "./myFiles.css";

const MyFiles = () => {
    const canRender = requireAuth(); // Ensure user is authenticated
    const { user } = useUser();
    const [isLoading, setIsLoading] = useState(true);
    const [files, setFiles] = useState([]);
    const [error, setError] = useState(null);

    const fetchFiles = async () => {
        setIsLoading(true);
        setError(null);

        try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/storage/getuserfiles/`, {
                method: "GET",
                credentials: "include",
                headers: {
                    Authorization: `Bearer ${user.token}`,
                },
            });

            if (response.ok) {
                const data = await response.json();
                setFiles(data);
            } else {
                const errorMessage = await response.text();
                setError(`Failed to fetch files: ${errorMessage}`);
            }
        } catch (err) {
            setError(`Error fetching files: ${err.message}`);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (user?.token) {
            fetchFiles();
        }

    }, [user]);

    if (!canRender) {
        return (
            <div className="loader-wrapper">
                <div className="loader" style={{ width: "50px", height: "50px" }}></div>
            </div>
        );
    }

    return (
        <div className="myvids">
            <div className="myvids-container">
                <h1>My Files</h1>

                {isLoading && <p>Loading files...</p>}
                {error && <p style={{ color: "red" }}>{error}</p>}

                {!isLoading && !error && files.length === 0 && <p>No files found.</p>}

                {!isLoading && files.length > 0 && (
                    <div className="myfiles-container">
                        {files.map((file) => (
                            <MyFileCard
                                key={file._id}
                                file={file}
                            />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default MyFiles;
