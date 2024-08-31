"use client"

import { useState, useEffect } from "react";
import { requireAdmin } from "@/utils/routeGuards";
import "./Admin.css";
import { useUser } from '@/context/UserContext';
import { useRouter } from 'next/navigation';


const Admin = () => {
    const { user } = useUser();
    const [searchTerm, setSearchTerm] = useState('');
    const [users, setUsers] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(false);
    const [shouldLoad, setShouldLoad] = useState(false);
    const canRender = requireAdmin();
    const router = useRouter();

    useEffect(() => {
        if (!canRender || !shouldLoad) return;

        loadMoreUsers();
        setShouldLoad(false);

        const handleScroll = () => {
            if (window.innerHeight + document.documentElement.scrollTop !== document.documentElement.offsetHeight) return;
            loadMoreUsers();
        };

        window.addEventListener('scroll', handleScroll);

        return () => window.removeEventListener('scroll', handleScroll);
    }, [searchTerm, canRender, hasMore, shouldLoad, page]);

    const handleSearch = () => {
        setUsers([]);
        setPage(0);
        setHasMore(true);
        setShouldLoad(true);
    };

    const fetchUserData = async (searchTerm, page = 0, size = 20) => {
        try {
            const response = await fetch('http://localhost:8080/api/user/searchusers', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${user.token}`,
                },
                body: JSON.stringify({
                    searchTerm: searchTerm,
                    page: page,
                    size: size,
                }),
                credentials: 'include',
            });

            if (response.ok) {
                const data = await response.json();
                return data.content;
            } else {
                console.error('Failed to fetch user data');
                return [];
            }
        } catch (error) {
            console.error('Error fetching user data:', error);
            return [];
        }
    };

    const loadMoreUsers = async () => {
        if (!hasMore) return;

        const newUsers = await fetchUserData(searchTerm, page, 20);
        if (newUsers.length < 20) {
            setHasMore(false);
        }

        setUsers((prevUsers) => [...prevUsers, ...newUsers]);
        setPage((prevPage) => prevPage + 1);
    };

    const handleRedirect = (user) => {
        sessionStorage.setItem(user.id, JSON.stringify(user));
        router.push(`/profile/admin/${user.id}`);
    };

    if (!canRender) {
        return (
            <div className='loader-wrapper'>
                <div className="loader" style={{ width: "50px", height: "50px" }}></div>
            </div>
        );
    }

    return (
        <div className="admin-component-container">
            <div className="user-search-box">
                <input
                    className="admin-search-input"
                    type="text"
                    placeholder="Search by Email, Username or ID"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                />
                <button className="form-submit" onClick={handleSearch}>Search</button>
            </div>

            <div className="foundUsers">
                <ul>
                    {users.map((foundUser, index) => (
                        <li className="found-user" key={index} onClick={() => handleRedirect(foundUser)}>
                            {foundUser.username} - {foundUser.email} - {foundUser.id}
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
};

export default Admin;
