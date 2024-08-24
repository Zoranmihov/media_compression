'use client';

import React from "react";
import Navbar from "@/components/navbar/Navbar";
import { UserProvider, useUser } from "@/context/UserContext";
import { Inter } from "next/font/google";
import "@/globalcss/globals.css";

const inter = Inter({ subsets: ["latin"] });

const LayoutContent = ({ children }: { children: React.ReactNode }) => {
  const { loading } = useUser();

  if (loading) {
    return <div className='loader-wrapper'>
      <div className="loader" style={{ width: "50px", height: "50px" }}></div>
    </div>
  }

  return (
    <>
      <Navbar />
      {children}
    </>
  );
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <UserProvider>
      <html lang="en">
        <body className={inter.className}>
          <LayoutContent>{children}</LayoutContent>
        </body>
      </html>
    </UserProvider>
  );
}
