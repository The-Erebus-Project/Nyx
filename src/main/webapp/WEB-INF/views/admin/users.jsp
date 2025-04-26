<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">

        <c:import url="../elements/header.jsp" />

        <title>Nyx - Admin panel - Users list</title>
    </head>
    <body>
        <c:import url="../elements/navbar.jsp" />

        <!-- Main Content -->
        <div class="content-wrapper container-fluid mt-3">
            <!-- Breadcrumb -->
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item"><a href="/">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-house-door" viewBox="0 0 16 16">
                            <path d="M8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 7.5v7a.5.5 0 0 0 .5.5h4.5a.5.5 0 0 0 .5-.5v-4h2v4a.5.5 0 0 0 .5.5H14a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.146-.354L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293zM2.5 14V7.707l5.5-5.5 5.5 5.5V14H10v-4a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v4z"/>
                        </svg> Home</a>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">Users</li>
                </ol>
            </nav>

            <c:import url="../elements/alerts.jsp" />
            
            <!-- Users Container -->
            <div class="users-container">
                <div class="action-bar">
                    <button class="btn btn-primary" onclick="location.href='/admin/users/create'">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-plus-circle" viewBox="0 0 16 16">
                            <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"/>
                            <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4"/>
                        </svg> Create New User
                    </button>
                </div>
                
                <div class="table-responsive">
                    <table class="table table-hover users-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Username</th>
                                <th>EMail</th>
                                <th>First name</th>
                                <th>Last name</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${usersList.isEmpty()}">
                                    <tr>
                                        <td colspan="5">No users found</td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${usersList}" var="user">
                                        <tr>
                                            <th scope="row">
                                                <a href="users/edit/${user.getId()}" >${user.getId()}</a>
                                            </th>
                                            <td>${user.getUsername()}</td>
                                            <td>${user.getEmail()}</td>
                                            <td>${user.getFirstName()}</td>
                                            <td>${user.getLastName()}</td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <c:import url="../elements/footer.jsp" />
    </body>
</html>