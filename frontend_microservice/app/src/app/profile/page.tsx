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
    const [currentField, setCurrentField] = useState('');
    const [newInformation, setNewInformation] = useState('');

    useEffect(() => {
        if (isModalOpen) {
            // Disable scrolling when the modal is open
            document.body.style.overflow = 'hidden';
        } else {
            // Re-enable scrolling when the modal is closed
            document.body.style.overflow = '';
        }

        return () => {
            document.body.style.overflow = '';
        };
    }, [isModalOpen]);

    if (!canRender) {
        return (
            <div className='loader-wrapper'>
                <div className="loader" style={{ width: "50px", height: "50px" }}></div>
            </div>
        );
    }

    const handleOpenModal = (field) => {
        setCurrentField(field);
        setNewInformation(user[field] || '');
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setNewInformation('');
    };

    const handleSubmit = async () => {
        const updateUserDTO = {
            informationToUpdate: currentField,
            newInformation: newInformation,
            accountId: user.id,
        };

        try {
            const response = await fetch('http://localhost:8080/api/user/update', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${user.token}`
                },
                body: JSON.stringify(updateUserDTO),
                credentials: "include"
            });

            if (response.ok) {
                const updatedFieldValue = await response.text();

                const updatedUser = {
                    ...user,
                    [currentField]: updatedFieldValue,
                };

                const refreshedToken = response.headers.get('X-Token-Refreshed');
                if (refreshedToken) {
                    const newExpirationTime = Date.now() + 3 * 60 * 60 * 1000;

                    updatedUser.token = refreshedToken;
                    updatedUser.tokenExpirationTime = newExpirationTime;
                }


                setUser(updatedUser);


                sessionStorage.setItem('user', JSON.stringify(updatedUser));

                console.log('Information updated successfully');
            } else {
                console.error('Failed to update information');
            }
        } catch (error) {
            console.error('Error updating information:', error);
        }

        handleCloseModal();
    };



    const getModalTitle = () => {
        switch (currentField) {
            case 'email':
                return 'Edit Email';
            case 'username':
                return 'Edit Username';
            case 'displayName':
                return 'Edit Display Name';
            case 'password':
                return 'Edit Password';
            default:
                return 'Edit Information';
        }
    };

    return (
        <>
            <div className="profile-sub-menu">
                {user.role == "ADMIN" ? (<Link href='/profile/admin'>Admin</Link>) : (null)}
                <Link href="/profile/user-videos">My videos</Link>
            </div>
            <div className="profile-container">

                <div className="profile-info-container">
                    <div className="profile-info-field profile-email">
                        {user.email}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal('email')}>
                            {returnEditSvg()}
                        </div>
                    </div>
                    <div className="profile-info-field profile-username">
                        {user.username}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal('username')}>
                            {returnEditSvg()}
                        </div>
                    </div>
                    <div className="profile-info-field profile-display-name">
                        {user.displayName}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal('displayName')}>
                            {returnEditSvg()}
                        </div>
                    </div>
                    <div className="profile-info-field profile-password">
                        <button className="form-submit" onClick={() => handleOpenModal('password')}>Change password</button>
                    </div>
                </div>

                {isModalOpen && (
                    <div className="modal-overlay">
                        <div className="modal-content">
                            <h2>{getModalTitle()}</h2>
                            <input
                                type={currentField === 'password' ? 'password' : 'text'}
                                value={newInformation}
                                onChange={(e) => setNewInformation(e.target.value)}
                                placeholder={`Enter new ${currentField}`}
                            />
                            <div className="modal-actions">
                                <button className="form-submit" onClick={handleSubmit}>Submit</button>
                                <button className="form-clear" onClick={handleCloseModal}>Cancel</button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </>
    );
};

export default Profile;
