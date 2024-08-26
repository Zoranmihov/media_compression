'use client'

import { usePathname } from 'next/navigation'
import Link from "next/link"
import Image from 'next/image'
import "./nav.css"

// Context
import { useUser } from '@/context/UserContext'

const Navbar = () => {
    const pathname: string = usePathname()
    const { user, setUser } = useUser();

    const handleLogout = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/user/logout', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${user.token}` // Include the Authorization header
                },
                credentials: 'include', // Include cookies in the request
            });
    
            if (response.ok) {
                // Clear session storage
                sessionStorage.clear();
    
                // Clear user state
                setUser({});
    
                console.log('Logout successful');
            } else {
                console.error('Logout failed');
            }
        } catch (error) {
            console.error('Error during logout:', error);
        }
    };
    

    return (
        <nav className="main-nav">
            <div className="logo">
                <Image
                    src={`logo-light.svg`}
                    width={40}
                    height={40}
                    alt="Logo"
                />
            </div>
            <div className="links">
                <Link className={`link ${pathname === '/' ? 'active-nav-link' : ''}`} href="/">Dashboard</Link>
                {user.token ? (
                    <>
                        <Link className={`link ${pathname === '/profile' ? 'active-nav-link' : ''}`} href="/profile">Profile</Link>
                        <p onClick={handleLogout}>Logout</p>
                    </>
                ) : (
                    <>
                        <Link className={`link ${pathname === '/login' ? 'active-nav-link' : ''}`} href="/login">Login</Link>
                        <Link className={`link ${pathname === '/register' ? 'active-nav-link' : ''}`} href="/register">Register</Link>
                    </>
                )}
            </div>
        </nav>
    )
}

export default Navbar