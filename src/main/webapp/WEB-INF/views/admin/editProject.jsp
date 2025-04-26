<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">

        <c:import url="../elements/header.jsp" />
        <script>
            function processDeletionEvent() {
                if (confirm('Are you sure you want to delete this project?')) {
                    return true;
                } else {
                    event.preventDefault();
                    return false;
                }
            }
        </script>

        <title>Nyx - Admin panel - Edit project</title>
    </head>
    <body>
        <c:import url="../elements/navbar.jsp" />

        <div class="content-wrapper container-fluid mt-3">
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item"><a href="/" >
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-house-door" viewBox="0 0 16 16">
                            <path d="M8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 7.5v7a.5.5 0 0 0 .5.5h4.5a.5.5 0 0 0 .5-.5v-4h2v4a.5.5 0 0 0 .5.5H14a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.146-.354L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293zM2.5 14V7.707l5.5-5.5 5.5 5.5V14H10v-4a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v4z"/>
                        </svg> Home</a>
                    </li>
                    <li class="breadcrumb-item"><a href="/admin/projects">Projects</a></li>
                    <li class="breadcrumb-item active" aria-current="page">Edit project</li>
                </ol>
            </nav>
    
            <!-- Project Form Container -->
            <div class="project-form-container mx-auto">
                <h4 class="mb-4">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-pencil" viewBox="0 0 16 16">
                        <path d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708l-10 10a.5.5 0 0 1-.168.11l-5 2a.5.5 0 0 1-.65-.65l2-5a.5.5 0 0 1 .11-.168zM11.207 2.5 13.5 4.793 14.793 3.5 12.5 1.207zm1.586 3L10.5 3.207 4 9.707V10h.5a.5.5 0 0 1 .5.5v.5h.5a.5.5 0 0 1 .5.5v.5h.293zm-9.761 5.175-.106.106-1.528 3.821 3.821-1.528.106-.106A.5.5 0 0 1 5 12.5V12h-.5a.5.5 0 0 1-.5-.5V11h-.5a.5.5 0 0 1-.468-.325"/>
                    </svg> Edit project
                </h4>
    
                <c:import url="../elements/alerts.jsp" />
                
                <form method="post" id="edit_form"></form>
                <form method="post" action="/admin/projects/delete/${project.getId()}" id="delete_form"></form>
                <!-- Project Name -->
                <div class="mb-4">
                    <label for="projectName" class="form-label">Project Name</label>
                    <input type="text" class="form-control" id="projectName" name="projectName" form="edit_form" value="${project.getName()}" required>
                </div>
                
                <!-- Project Description -->
                <div class="mb-4">
                    <label for="projectDescription" class="form-label">Project Description</label>
                    <textarea class="form-control" id="projectDescription" name="projectDescription" name="description" form="edit_form" rows="4">${project.getDescription()}</textarea>
                </div>

                <!-- Strict mode switch-->
                <div class="mb-4">
                    <label for="isStrict" class="form-label">
                        Is Strict
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-info-circle" viewBox="0 0 16 16" data-bs-toggle="tooltip" data-bs-placement="right" title="If the project is set to strict mode - only registered nodes can connect to it. If not - Nyx will either register it itself, or if no node ID is provided - assign one on it's own">
                            <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"/>
                            <path d="m8.93 6.588-2.29.287-.082.38.45.083c.294.07.352.176.288.469l-.738 3.468c-.194.897.105 1.319.808 1.319.545 0 1.178-.252 1.465-.598l.088-.416c-.2.176-.492.246-.686.246-.275 0-.375-.193-.304-.533zM9 4.5a1 1 0 1 1-2 0 1 1 0 0 1 2 0"/>
                        </svg>
                    </label>
                    <div class="form-check form-switch">
                        <input class="form-check-input" type="checkbox" id="isStrict" name="isStrict" form="edit_form" role="switch" <c:if test="${project.isStrict()}">checked</c:if>>
                    </div>
                </div>
                
                <!-- Form Actions -->
                <div class="form-actions">
                    <button type="submit" class="btn btn-danger" form="delete_form" onclick="processDeletionEvent()">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-x-lg" viewBox="0 0 16 16">
                            <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8z"/>
                        </svg> Delete project
                    </button>
                    <a href="/admin/projects" class="btn btn-outline-secondary">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-x-lg" viewBox="0 0 16 16">
                            <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8z"/>
                        </svg> Cancel
                    </a>
                    <button type="submit" class="btn btn-primary" form="edit_form">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-save" viewBox="0 0 16 16">
                            <path d="M2 1a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1H9.5a1 1 0 0 0-1 1v7.293l2.646-2.647a.5.5 0 0 1 .708.708l-3.5 3.5a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L7.5 9.293V2a2 2 0 0 1 2-2H14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h2.5a.5.5 0 0 1 0 1z"/>
                        </svg> Save changes
                    </button>
                </div>
            </div>
        </div>
        <c:import url="../elements/footer.jsp" />
        <script>
            // Initialize tooltips
            $(document).ready(function(){
                $('[data-bs-toggle="tooltip"]').tooltip();
            });
        </script>
    </body>
</html>