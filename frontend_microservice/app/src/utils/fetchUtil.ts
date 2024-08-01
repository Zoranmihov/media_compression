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
