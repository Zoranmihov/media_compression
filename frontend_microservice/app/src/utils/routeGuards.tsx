import { useRouter } from 'next/navigation';
import { useUser } from '@/context/UserContext';
import { useEffect } from 'react';

export function requireAdmin() {
  const { user, loading } = useUser();
  const router = useRouter();

  useEffect(() => {
    if (!loading && user?.role !== 'ADMIN') {
      router.push('/');
    }
  }, [user?.role, loading, router]);

  return user?.role === 'ADMIN';
}


export function requireAuth() {
  const { user, loading } = useUser();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user?.token) {
      router.push('/login');
    }
  }, [user?.token, loading, router]);

  return !!user?.token;
}



export function requireGuest() {
  const { user, loading } = useUser();
  const router = useRouter();

  useEffect(() => {
      if (!loading && user?.token) {
          router.push('/'); 
      }
  }, [user?.token, loading, router]);

  return !user?.token;
}