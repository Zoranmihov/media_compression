"use client";

import { useRouter } from 'next/navigation';
import { useForm } from "react-hook-form";
import { fetchData } from "@/utils/fetchUtil";
import { requireGuest } from "@/utils/routeGuards";



const Register = () => {

    const canRender = requireGuest();


    const router = useRouter();
    const { register, handleSubmit, formState: { errors } } = useForm();

    const registerSubmit = async (data) => {
        const responseData = await fetchData('/api/user/register', 'POST', data);

        console.log(responseData);

        if (responseData.status == "fail") {
            // We show res card
            console.log(responseData.message)
        } else {
            router.push("/login");
        }
    };

    const clearForm = () => {
        document.querySelector(".register-form").reset();
    }

    if (!canRender) {
        return (
            <div className='loader-wrapper'>
                <div className="loader" style={{ width: "50px", height: "50px" }}></div>
            </div>
        )

    } else {
        return (
            <div className="form-container">
                <form className="register-form" onSubmit={handleSubmit(registerSubmit)}>
                    <h2 className="form-title">Sign up</h2>
                    <label htmlFor="email" className="form-label">Email:</label>
                    <input
                        type="email"
                        name="email"
                        className="form-input email-input"
                        {...register('email', {
                            required: "Email is required",
                            pattern: {
                                value: /^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$/,
                                message: "Invalid email address",
                            },
                        })}
                    />
                    {errors.email && <p className="form-error">{errors.email.message}</p>}

                    <label htmlFor="username" className="form-label">Username:</label>
                    <input
                        type="text"
                        name="username"
                        className="form-input username-input"
                        {...register('username', {
                            required: "Username is required",
                            minLength: {
                                value: 4,
                                message: "Username must be at least 4 characters long",
                            },
                            maxLength: {
                                value: 20,
                                message: "Username must be at most 20 characters long",
                            },
                        })}
                    />
                    {errors.username && <p className="form-error">{errors.username.message}</p>}

                    <label htmlFor="displayName" className="form-label">Display Name:</label>
                    <input
                        type="text"
                        name="displayName"
                        className="form-input display-name-input"
                        {...register('displayName', {
                            required: "Display name is required",
                            minLength: {
                                value: 4,
                                message: "Display name must be at least 4 characters long",
                            },
                            maxLength: {
                                value: 50,
                                message: "Display name must be at most 50 characters long",
                            },
                        })}
                    />
                    {errors.displayName && <p className="form-error">{errors.displayName.message}</p>}

                    <label htmlFor="password" className="form-label">Password:</label>
                    <input
                        type="password"
                        name="password"
                        className="form-input password-input"
                        {...register('password', {
                            required: "Password is required",
                            minLength: {
                                value: 8,
                                message: "Password must be at least 8 characters long",
                            },
                            maxLength: {
                                value: 16,
                                message: "Password must be at most 16 characters long",
                            },
                            pattern: {
                                value: /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,16}$/,
                                message: "Password must contain at least one uppercase letter, one number, and one special character",
                            },
                        })}
                    />
                    {errors.password && <p className="form-error">{errors.password.message}</p>}

                    <button className="form-submit" type="submit">Register</button>
                    <button className="form-clear" type="button" onClick={clearForm}>Clear</button>
                </form>
            </div>
        );
    }


};

export default Register;
