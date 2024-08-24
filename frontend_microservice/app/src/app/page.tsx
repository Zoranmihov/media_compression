"use client"

import { useUser } from '@/context/UserContext'
import FileUpload from '@/components/FileUpload/FileUpload';

export default function Home() {

  const { user } = useUser();

  return (
    user.token ? (
      <FileUpload />
    ) : (
      <div className='welcome-text-container'><p>Please log in in order to compress media files</p></div>
    )
  );
}
