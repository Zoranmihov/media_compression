"use client";

import { useForm } from "react-hook-form";
import { useUser } from '@/context/UserContext'
import { fetchData } from "@/utils/fetchUtil";

const Login = () => {
    const { register, handleSubmit, formState: { errors } } = useForm();
    const { user, setUser } = useUser();

    const loginSubmit = async (data) => {
        try {
            const responseData = await fetchData('/api/user/login', 'POST', data);
            setUser(responseData);
        } catch (error) {
            console.error('Error during fetch:', error);
        }
    };
    
    return (
        <div className="form-container">
            <form className="login-form" onSubmit={handleSubmit(loginSubmit)}>
                <h2 className="form-title">Sign in</h2>
                <label htmlFor="login" className="form-label">Username or Email:</label>
                <input
                    type="text"
                    name="login"
                    className="form-input logn-input"
                    {...register('login', {
                        required: "Login is required",
                        minLength: {
                            value: 4,
                            message: "Login must be at least 4 characters long"
                        },
                        maxLength: {
                            value: 30,
                            message: "Login must be at most 30 characters long"
                        }
                    })}
                />
                {errors.login && <p className="form-error">{errors.login.message}</p>}

                <label htmlFor="password" className="form-label">Password:</label>
                <input
                    type="password"
                    name="password"
                    className="form-input password-input"
                    {...register('password', {
                        required: "Password is required",
                        minLength: {
                            value: 8,
                            message: "Password must be at least 8 characters long"
                        },
                        maxLength: {
                            value: 16,
                            message: "Password must be at most 16 characters long"
                        }
                    })}
                />
                {errors.password && <p className="form-error">{errors.password.message}</p>}

                <button className="form-submit" type="submit">Login</button>
            </form>
        </div>
    );
};

export default Login;
