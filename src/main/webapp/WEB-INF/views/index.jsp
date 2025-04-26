<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">

        <c:import url="elements/header.jsp" />

        <title>Nyx - home page</title>
    </head>
    <body>
        <c:import url="elements/navbar.jsp" />
        <!-- Main Content -->
        <div class="content-wrapper container mt-4">
            <div class="row mb-4">
                <div class="row mb-4">
                    <div class="col">
                        <h1>
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-kanban" viewBox="0 0 16 16">
                                <path d="M13.5 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1zm-11-1a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2z"/>
                                <path d="M6.5 3a1 1 0 0 1 1-1h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1zm-4 0a1 1 0 0 1 1-1h1a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1zm8 0a1 1 0 0 1 1-1h1a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1z"/>
                            </svg> Projects
                        </h1>
                    </div>
                </div>
            </div>
            
            <!-- Projects Grid -->
            <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
                <c:choose>
                    <c:when test="${projectsList.isEmpty()}">
                        <tr>
                            <h3>No projects found</h3>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach items="${projectsList}" var="project">
                            <div class="col">
                                <div class="card project-card">
                                    <div class="card-body position-relative">
                                        <span class="project-id">ID: ${project.getId()}</span>
                                        <h5 class="card-title">
                                            <a href="project/${project.getId()}" class="text-decoration-none">${project.getName()}</a>
                                        </h5>
                                        <p class="card-text">${project.getDescription()}</p>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <c:import url="elements/footer.jsp" />
    </body>
</html>