"use client";

import { useState } from 'react';
import { useUser } from '@/context/UserContext';
import { requireAdmin } from "@/utils/routeGuards";
import { returnEditSvg } from "@/utils/returnEditSVG";
import "./FoundUser.css";

const FoundUser = ({ params }) => {
    const { user } = useUser();
    const [foundUser, setFoundUser] = useState(() => {
        const storedUser = sessionStorage.getItem(params.user);
        if (storedUser) {
            const userObject = JSON.parse(storedUser);
            sessionStorage.removeItem(params.user);
            return userObject;
        }
        return null;
    });
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentField, setCurrentField] = useState('');
    const [newInformation, setNewInformation] = useState('');
    const canRender = requireAdmin();

    if (!canRender || !foundUser) {
        return (
            <div className='loader-wrapper'>
                <div className="loader" style={{ width: "50px", height: "50px" }}></div>
            </div>
        );
    }

    const handleOpenModal = (field) => {
        setCurrentField(field);
        if (field === 'role') {
            // Pre-select the radio button based on foundUser's current role
            setNewInformation(foundUser.role === 'ADMIN' ? 'Admin' : 'User');
        } else {
            setNewInformation(foundUser[field] || '');
        }
        setIsModalOpen(true);
        document.body.style.overflow = 'hidden';
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setNewInformation('');
        document.body.style.overflow = ''; 
    };

    const handleSubmit = async () => {
        try {
            let response;

            if (currentField === 'role') {
                // Send the role as uppercase (USER, ADMIN)
                const updateRoleDTO = {
                    accountId: foundUser.id,
                    newRole: newInformation.toUpperCase(),
                };

                response = await fetch('http://localhost:8080/api/user/updaterole', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${user.token}`,
                    },
                    body: JSON.stringify(updateRoleDTO),
                    credentials: "include"
                });

                if (response.ok) {
                    const updatedUser = {
                        ...foundUser,
                        role: newInformation.toUpperCase(),
                    };

                    setFoundUser(updatedUser);

                    console.log('Role updated successfully');
                } else {
                    console.error('Failed to update role');
                }
            } else {
                const updateUserDTO = {
                    informationToUpdate: currentField,
                    newInformation: newInformation,
                    accountId: foundUser.id,
                };

                response = await fetch('http://localhost:8080/api/user/update', {
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
                        ...foundUser,
                        [currentField]: updatedFieldValue,
                    };

                    setFoundUser(updatedUser);

                    console.log('Information updated successfully');
                } else {
                    console.error('Failed to update information');
                }
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
            case 'role':
                return 'Edit Role';
            default:
                return 'Edit Information';
        }
    };

    return (
        <>
            <div className="found-user-container">
                <div className="found-user-info-container">
                    <div className="found-user-info-field found-user-email">
                        {"Email: " + foundUser.email}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal('email')}>
                            {returnEditSvg()}
                        </div>
                    </div>
                    <div className="found-user-info-field found-user-username">
                        {"Username: " + foundUser.username}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal('username')}>
                            {returnEditSvg()}
                        </div>
                    </div>
                    <div className="found-user-info-field found-user-display-name">
                        {"Display name: " + foundUser.displayName}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal('displayName')}>
                            {returnEditSvg()}
                        </div>
                    </div>
                    <div className="found-user-info-field found-user-role">
                        {"Role: " + foundUser.role}
                        <div className="edit-svg-icon" onClick={() => handleOpenModal('role')}>
                            {returnEditSvg()}
                        </div>
                    </div>
                </div>

                {isModalOpen && (
                    <div className="modal-overlay">
                        <div className="modal-content">
                            <h2>{getModalTitle()}</h2>
                            {currentField === 'role' ? (
                                <div className='role-select'>
                                    <label>
                                        <input
                                            type="radio"
                                            value="User"
                                            checked={newInformation === 'User'}
                                            onChange={(e) => setNewInformation(e.target.value)}
                                        />
                                        User
                                    </label>
                                    <label>
                                        <input
                                            type="radio"
                                            value="Admin"
                                            checked={newInformation === 'Admin'}
                                            onChange={(e) => setNewInformation(e.target.value)}
                                        />
                                        Admin
                                    </label>
                                </div>
                            ) : (
                                <input
                                    type='text'
                                    value={newInformation}
                                    onChange={(e) => setNewInformation(e.target.value)}
                                    placeholder={`Enter new ${currentField}`}
                                />
                            )}
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

export default FoundUser;
