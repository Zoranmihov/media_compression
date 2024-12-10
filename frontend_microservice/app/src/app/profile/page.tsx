"use client";

import { returnEditSvg } from "@/utils/returnEditSVG";
import Link from "next/link";
import { useUser } from '@/context/UserContext';
import { requireAuth } from "@/utils/routeGuards";
import { useState, useEffect } from 'react';
import "./Profile.css";

const Profile = () => {
    const canRender = requireAuth();
    const { user, setUser } = useUser();

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [currentField, setCurrentField] = useState("");
    const [newInformation, setNewInformation] = useState("");

    useEffect(() => {
        if (isModalOpen || isDeleteModalOpen) {
            document.body.style.overflow = "hidden";
        } else {
            document.body.style.overflow = "";
        }

        return () => {
            document.body.style.overflow = "";
        };
    }, [isModalOpen, isDeleteModalOpen]);

    if (!canRender) {
        return (
            <div className="loader-wrapper">
                <div className="loader" style={{ width: "50px", height: "50px" }}></div>
            </div>
        );
    }

    const handleOpenModal = (field) => {
        setCurrentField(field);
        setNewInformation(user[field] || "");
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setNewInformation("");
    };

    const handleOpenDeleteModal = () => {
        setIsDeleteModalOpen(true);
    };

    const handleCloseDeleteModal = () => {
        setIsDeleteModalOpen(false);
    };

    const handleSubmit = async () => {
        const updateUserDTO = {
            informationToUpdate: currentField,
            newInformation: newInformation,
            accountId: user.id,
        };

        try {
            const response = await fetch(
                `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/user/update`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${user.token}`,
                    },
                    body: JSON.stringify(updateUserDTO),
                    credentials: "include",
                }
            );

            if (response.ok) {
                const updatedFieldValue = await response.text();
                const updatedUser = { ...user, [currentField]: updatedFieldValue };
                const refreshedToken = response.headers.get("X-Token-Refreshed");
                if (refreshedToken) {
                    updatedUser.token = refreshedToken;
                    updatedUser.tokenExpirationTime =
                        Date.now() + 3 * 60 * 60 * 1000;
                }

                setUser(updatedUser);
                sessionStorage.setItem("user", JSON.stringify(updatedUser));
                console.log("Information updated successfully");
            } else {
                console.error("Failed to update information");
            }
        } catch (error) {
            console.error("Error updating information:", error);
        }

        handleCloseModal();
    };

    const handleDeleteAccount = async () => {
    
        try {
            // Prepare the requests for both endpoints
            const accountDeletionRequest = fetch(
                `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/user/delete/${user.id}`,
                {
                    method: "DELETE",
                    headers: {
                        Authorization: `Bearer ${user.token}`,
                        UserId: user.id,
                    },
                }
            );
    
            const fileDeletionRequest = fetch(
                `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/storage/deleteuserfiles/`,
                {
                    method: "DELETE",
                    headers: {
                        Authorization: `Bearer ${user.token}`,
                        UserId: user.id,
                    },
                }
            );
    
            // Execute both requests in parallel
            const [accountResponse, filesResponse] = await Promise.all([
                accountDeletionRequest,
                fileDeletionRequest,
            ]);
    
            // Handle account deletion response
            if (!accountResponse.ok) {
                const errorText = await accountResponse.text();
                console.error("Failed to delete account:", errorText);
                throw new Error("Account deletion failed.");
            }
            console.log("Account deletion successful:", await accountResponse.text());
    
            // Handle file deletion response
            if (!filesResponse.ok) {
                const errorText = await filesResponse.text();
                console.error("Failed to delete files:", errorText);
                throw new Error("File deletion failed.");
            }
            console.log("File deletion successful:", await filesResponse.json());    
        } catch (error) {
            console.error("An error occurred during the delete process:", error);
            alert("An error occurred while deleting the account. Please try again.");
        } finally {
            sessionStorage.removeItem("user");
            handleCloseDeleteModal();
            window.location.href = "/"
        }
    };
    

    return (
        <>
            <div className="profile-sub-menu">
                {user.role == "ADMIN" ? <Link href="/profile/admin">Admin</Link> : null}
                <Link href="/profile/myfiles">My Files</Link>
            </div>
            <div className="profile-container">
                <div className="profile-info-container">
                    <div className="profile-info-field profile-email">
                        {user.email}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal("email")}>
                            {returnEditSvg()}
                        </div>
                    </div>
                    <div className="profile-info-field profile-username">
                        {user.username}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal("username")}>
                            {returnEditSvg()}
                        </div>
                    </div>
                    <div className="profile-info-field profile-display-name">
                        {user.displayName}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal("displayName")}>
                            {returnEditSvg()}
                        </div>
                    </div>
                    <div className="profile-info-field profile-password">
                        <button className="form-submit" onClick={() => handleOpenModal("password")}>
                            Change password
                        </button>
                    </div>
                    <div className="profile-info-field profile-password">
                        <button className="form-submit" onClick={handleOpenDeleteModal}>
                            Delete account
                        </button>
                    </div>
                </div>

                {isModalOpen && (
                    <div className="modal-overlay">
                        <div className="modal-content">
                            <h2>{`Edit ${currentField}`}</h2>
                            <input
                                type={currentField === "password" ? "password" : "text"}
                                value={newInformation}
                                onChange={(e) => setNewInformation(e.target.value)}
                                placeholder={`Enter new ${currentField}`}
                            />
                            <div className="modal-actions">
                                <button className="form-submit" onClick={handleSubmit}>
                                    Submit
                                </button>
                                <button className="form-clear" onClick={handleCloseModal}>
                                    Cancel
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {isDeleteModalOpen && (
                    <div className="modal-overlay">
                        <div className="modal-content">
                            <h2>Confirm Account Deletion</h2>
                            <p>Are you sure you want to delete your account? This action cannot be undone.</p>
                            <div className="modal-actions">
                                <button className="form-submit" onClick={handleDeleteAccount}>
                                    Confirm
                                </button>
                                <button className="form-clear" onClick={handleCloseDeleteModal}>
                                    Cancel
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </>
    );
};

export default Profile;
