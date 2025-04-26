<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">

        <c:import url="elements/header.jsp" />
        <script src="/js/userSettings/user_settings.js"></script>

        <style>
            .settings-container {
                background: white;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                padding: 30px;
                margin-top: 20px;
                max-width: 600px;
            }
            
            .password-input-group {
                display: flex;
                align-items: center;
            }
            
            .password-input-group input {
                flex: 1;
                border-top-right-radius: 0;
                border-bottom-right-radius: 0;
            }
            
            .toggle-password-btn {
                height: 38px; /* Match input height */
                border: 1px solid #ced4da;
                border-left: none;
                border-top-right-radius: 4px;
                border-bottom-right-radius: 4px;
                background-color: #f8f9fa;
                color: #495057;
                padding: 0 12px;
                cursor: pointer;
                transition: all 0.15s ease;
            }
            
            .toggle-password-btn:hover {
                background-color: #e9ecef;
                color: #212529;
            }
            
            .password-strength {
                height: 4px;
                background: #e9ecef;
                margin-top: 5px;
                border-radius: 2px;
                overflow: hidden;
            }
            
            .strength-bar {
                height: 100%;
                width: 0%;
                transition: width 0.3s ease, background 0.3s ease;
            }
        </style>

        <title>Nyx - user settings</title>
    </head>
    <body>
        <c:import url="elements/navbar.jsp" />
        <!-- Main Content -->
        <div class="content-wrapper container-fluid mt-3">
            <!-- Breadcrumb -->
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item"><a href="/">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-house-door" viewBox="0 0 16 16">
                            <path d="M8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 7.5v7a.5.5 0 0 0 .5.5h4.5a.5.5 0 0 0 .5-.5v-4h2v4a.5.5 0 0 0 .5.5H14a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.146-.354L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293zM2.5 14V7.707l5.5-5.5 5.5 5.5V14H10v-4a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v4z"/>
                        </svg> Home</a></li>
                    <li class="breadcrumb-item active" aria-current="page">User Settings</li>
                </ol>
            </nav>
            
            <!-- Settings Container -->
            <div class="settings-container mx-auto">
                <h4 class="mb-4">Change Password</h4>
                
                <c:import url="elements/alerts.jsp" />
                <form id="passwordChangeForm" method="post">
                    <!-- New Password -->
                    <div class="mb-3">
                        <label for="newPassword" class="form-label">New Password</label>
                        <div class="password-input-group">
                            <input type="password" class="form-control" name="newPassword" id="newPassword" required 
                                pattern="(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{8,}" 
                                title="Must contain at least 8 characters, including uppercase, lowercase and numbers">
                            <button type="button" class="toggle-password-btn" data-target="newPassword">Show</button>
                        </div>
                        <div class="password-strength">
                            <div class="strength-bar" id="passwordStrength"></div>
                        </div>
                        <div class="form-text">Minimum 8 characters with uppercase, lowercase and numbers</div>
                    </div>
                    
                    <!-- Confirm Password -->
                    <div class="mb-4">
                        <label for="newPasswordConfirmation" class="form-label">Confirm New Password</label>
                        <div class="password-input-group">
                            <input type="password" class="form-control" name="newPasswordConfirmation" id="newPasswordConfirmation" required>
                            <button type="button" class="toggle-password-btn" data-target="newPasswordConfirmation">Show</button>
                        </div>
                        <div class="invalid-feedback" id="passwordMatchError">Passwords do not match</div>
                    </div>
                    
                    <!-- Submit Button -->
                    <div class="d-grid gap-2">
                        <button type="submit" class="btn btn-primary">
                            Change Password
                        </button>
                    </div>
                </form>
            </div>
        </div>
        <c:import url="elements/footer.jsp" />

        <script>
            document.addEventListener('DOMContentLoaded', function() {
                // Toggle password visibility
                document.querySelectorAll('.toggle-password-btn').forEach(button => {
                    button.addEventListener('click', function() {
                        const targetId = this.getAttribute('data-target');
                        const input = document.getElementById(targetId);
                        
                        if (input.type === 'password') {
                            input.type = 'text';
                            this.textContent = 'Hide';
                        } else {
                            input.type = 'password';
                            this.textContent = 'Show';
                        }
                    });
                });
                
                // Password strength indicator
                const newPassword = document.getElementById('newPassword');
                const strengthBar = document.getElementById('passwordStrength');
                
                newPassword.addEventListener('input', function() {
                    const password = this.value;
                    let strength = 0;
                    
                    if (password.length >= 8) strength += 1;
                    if (password.length >= 12) strength += 1;
                    if (/[A-Z]/.test(password)) strength += 1;
                    if (/[0-9]/.test(password)) strength += 1;
                    if (/[^A-Za-z0-9]/.test(password)) strength += 1;
                    
                    const width = (strength / 5) * 100;
                    strengthBar.style.width = width + '%';
                    
                    if (width < 40) {
                        strengthBar.style.backgroundColor = '#dc3545';
                    } else if (width < 70) {
                        strengthBar.style.backgroundColor = '#fd7e14';
                    } else {
                        strengthBar.style.backgroundColor = '#28a745';
                    }
                });
                
                // Password match validation
                const newPasswordConfirmation = document.getElementById('newPasswordConfirmation');
                const passwordMatchError = document.getElementById('passwordMatchError');
                
                newPasswordConfirmation.addEventListener('input', function() {
                    if (this.value !== newPassword.value) {
                        this.classList.add('is-invalid');
                        passwordMatchError.style.display = 'block';
                    } else {
                        this.classList.remove('is-invalid');
                        passwordMatchError.style.display = 'none';
                    }
                });
            });
        </script>
    </body>
</html>