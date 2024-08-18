'use client';

import { createContext, useContext, useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

const UserContext = createContext(null);

export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
    const [user, setUser] = useState({});
    const [loading, setLoading] = useState(true);
    const router = useRouter();

    const updateUser = (userData) => {
        setUser(userData);
        sessionStorage.setItem('user', JSON.stringify(userData));
    };

    useEffect(() => {
        const storedUser = sessionStorage.getItem('user');
        if (storedUser) {
            const user = JSON.parse(storedUser);
            const currentTime = Date.now();

            if (currentTime > user.tokenExpirationTime) {
                console.log("Token has expired. Redirecting to login...");
                sessionStorage.removeItem('user');
                setUser({});
                router.push('/login');
            } else {
                setUser(user);
            }
        }
        setLoading(false);
    }, [router]);

    return (
        <UserContext.Provider value={{ user, setUser: updateUser, loading }}>
            {children}
        </UserContext.Provider>
    );
};
