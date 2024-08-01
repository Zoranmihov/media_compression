'use client'


import { createContext, useContext, useState, useEffect } from 'react';

const UserContext = createContext(null);

export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
    const [user, setUser] = useState({});

    // useEffect(() => {
    //     // Check if user is authenticated when the component mounts
    //     const fetchUser = async () => {
    //         try {
    //             const res = await fetch('/api/auth/user', { credentials: 'include' });
    //             if (res.ok) {
    //                 const data = await res.json();
    //                 setUser(data.user);
    //             }
    //         } catch (error) {
    //             console.error('Failed to fetch user:', error);
    //         }
    //     };

    //     fetchUser();
    // }, []);

    return (
        <UserContext.Provider value={{ user, setUser }}>
            {children}
        </UserContext.Provider>
    );
};
