<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <c:import url="elements/header.jsp" />

        <style>
            .login-container {
                max-width: 400px;
                margin: 0 auto;
                padding: 40px;
                background: white;
                border-radius: 8px;
                box-shadow: 0 2px 15px rgba(0,0,0,0.1);
                margin-top: 5vh;
            }
            
            .login-logo {
                text-align: center;
                margin-bottom: 30px;
                color: #0d6efd;
            }
            
            .login-logo i {
                font-size: 3.5rem;
            }
            
            .form-floating:focus-within {
                z-index: 2;
            }
            
            input[type="text"] {
                margin-bottom: -1px;
                border-bottom-right-radius: 0;
                border-bottom-left-radius: 0;
            }
            
            input[type="password"] {
                margin-bottom: 20px;
                border-top-left-radius: 0;
                border-top-right-radius: 0;
                padding-right: 45px; /* Space for toggle button */
            }
            
            .password-toggle {
                position: absolute;
                right: 10px;
                top: 50%;
                transform: translateY(-50%);
                background: none;
                border: none;
                color: #6c757d;
                z-index: 3;
            }
            
            .password-toggle:hover {
                color: #495057;
            }
            
            .password-container {
                position: relative;
            }
            
            .footer-links {
                text-align: center;
                margin-top: 20px;
            }

            /* Animation for messages */
            .alert-success .alert-danger {
                animation: fadeIn 0.5s ease-in-out;
            }
            
            @keyframes fadeIn {
                from { opacity: 0; transform: translateY(-10px); }
                to { opacity: 1; transform: translateY(0); }
            }
        </style>

        <title>Nyx -Login page</title>
    </head>
    <body class="bg-light">
        <div class="container">
            <div class="login-container">
                <div class="login-logo">
                    <img src="/res/logo.png" alt="Logo" height="128px">
                </div>

                <c:if test="${param.logout ne null}">
                    <div class="alert alert-success">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-check-circle" viewBox="0 0 16 16">
                            <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"/>
                            <path d="m10.97 4.97-.02.022-3.473 4.425-2.093-2.094a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-1.071-1.05"/>
                        </svg> You have been logged out successfully
                    </div>
                </c:if>

                <c:if test="${param.error ne null}">
                    <div class="alert alert-danger mt-3">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-exclamation-circle" viewBox="0 0 16 16">
                            <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"/>
                            <path d="M7.002 11a1 1 0 1 1 2 0 1 1 0 0 1-2 0M7.1 4.995a.905.905 0 1 1 1.8 0l-.35 3.507a.552.552 0 0 1-1.1 0z"/>
                        </svg> Invalid username or password
                    </div>
                </c:if>
                
                <form method="post" action="/login">
                    <div class="form-floating mb-3">
                        <input type="text" class="form-control" id="username" name="username" 
                               placeholder="Username" required autofocus>
                        <label for="username">Username</label>
                    </div>
                    
                    <div class="form-floating mb-3 password-container">
                        <input type="password" class="form-control" id="password" name="password" 
                               placeholder="Password" required>
                        <label for="password">Password</label>
                        <button type="button" class="password-toggle" id="togglePassword">
                            Show
                        </button>
                    </div>
                    
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    
                    <button class="w-100 btn btn-lg btn-primary" type="submit">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-box-arrow-in-right" viewBox="0 0 16 16">
                            <path fill-rule="evenodd" d="M6 3.5a.5.5 0 0 1 .5-.5h8a.5.5 0 0 1 .5.5v9a.5.5 0 0 1-.5.5h-8a.5.5 0 0 1-.5-.5v-2a.5.5 0 0 0-1 0v2A1.5 1.5 0 0 0 6.5 14h8a1.5 1.5 0 0 0 1.5-1.5v-9A1.5 1.5 0 0 0 14.5 2h-8A1.5 1.5 0 0 0 5 3.5v2a.5.5 0 0 0 1 0z"/>
                            <path fill-rule="evenodd" d="M11.854 8.354a.5.5 0 0 0 0-.708l-3-3a.5.5 0 1 0-.708.708L10.293 7.5H1.5a.5.5 0 0 0 0 1h8.793l-2.147 2.146a.5.5 0 0 0 .708.708z"/>
                        </svg> Log In
                    </button>
                </form>
            </div>
        </div>
        
        <!-- Password Toggle Script -->
        <script>
            document.addEventListener('DOMContentLoaded', function() {
                const togglePassword = document.querySelector('#togglePassword');
                const password = document.querySelector('#password');
                
                togglePassword.addEventListener('click', function() {
                    // Toggle the type attribute
                    const type = password.getAttribute('type') === 'password' ? 'text' : 'password';
                    password.setAttribute('type', type);
                    
                    // Toggle the button text
                    this.textContent = type === 'password' ? 'Show' : 'Hide';
                });
            });
        </script>
    </body>
</html>