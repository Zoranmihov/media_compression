export const fetchData = async (endpoint, method = 'GET', data = null, options = {}) => {
    endpoint = "http://localhost:8080" + endpoint;
    const fetchOptions = {
        method,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
        ...options,
    };

    if (data) {
        fetchOptions.body = JSON.stringify(data);
    }

    try {
        let res = await fetch(endpoint, fetchOptions);
        let data = await res.json();
    
        if (res.ok) {
            return data;
        } else {
            return {
                status: "fail",
                message: data.message || "An error occurred",
            };
        }
    } catch (error) {
        console.error('Error during fetch:', error);
        throw error;
    }
};

// export const fetchProtectedData = async (endpoint, method = 'GET', data = null, options = {}) => {
//     endpoint = "http://localhost:8080" + endpoint;
//     const fetchOptions = {
//         method,
//         headers: {
//             ...options.headers,
//         },
//         credentials: 'include', // Include cookies in requests
//         ...options,
//     };

//     if (data) {
//         // If data is a FormData object, don't stringify it
//         if (data instanceof FormData) {
//             fetchOptions.body = data;
//             // Automatically remove the Content-Type header to allow the browser to set it to multipart/form-data
//             delete fetchOptions.headers['Content-Type'];
//         } else {
//             fetchOptions.body = JSON.stringify(data);
//             fetchOptions.headers['Content-Type'] = 'application/json';
//         }
//     }

//     try {
//         const res = await fetch(endpoint, fetchOptions);

//         // Check for token refresh in response headers
//         const refreshedToken = res.headers.get('X-Token-Refreshed');

//         if (refreshedToken) {
//             const storedUser = JSON.parse(sessionStorage.getItem('user'));
//             const newUser = {
//                 ...storedUser,
//                 token: refreshedToken,
//                 tokenExpirationTime: /* new expiration time based on your backend */,
//             };

//             // Update the user context and session storage
//             setUser(newUser);
//             sessionStorage.setItem('user', JSON.stringify(newUser));
//         }

//         // Check if the response is supposed to be a file
//         const contentDisposition = res.headers.get('Content-Disposition');
//         if (contentDisposition && contentDisposition.includes('attachment')) {
//             // Handle file download
//             const blob = await res.blob();
//             return blob;
//         }

//         const responseData = await res.json();

//         if (res.ok) {
//             return responseData;
//         } else {
//             return {
//                 status: "fail",
//                 message: responseData.message || "An error occurred",
//             };
//         }
//     } catch (error) {
//         console.error('Error during fetch:', error);
//         throw error;
//     }
// };
