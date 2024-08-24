import React from 'react';

interface LoadingProps {
    width: string;
    height: string;
}

const Loading: React.FC<LoadingProps> = ({ width, height }) => {
    return (
        <div className='loader-wrapper'>
            <div className="loader" style={{ width: "50px", height: "50px" }}></div>
        </div>
    );
}

export default Loading;
