<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">

        <c:import url="../elements/header.jsp" />

        <style>
            .log-container {
                background: white;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                padding: 20px;
                margin-top: 20px;
            }
            
            .scrollable-table {
                max-height: calc(100vh - 250px);
                overflow-y: auto;
            }
            
            table {
                width: 100%;
            }
            
            th {
                position: sticky;
                top: 0;
                background: white;
                z-index: 10;
                box-shadow: 0 2px 2px -1px rgba(0, 0, 0, 0.1);
            }
            
            .timestamp {
                white-space: nowrap;
            }
        </style>

        <title>Nyx - Admin panel - User activity Log</title>
    </head>
    <body>
        <c:import url="../elements/navbar.jsp" />

        <!-- Main Content -->
        <div class="container-fluid mt-3">
            <!-- Breadcrumb -->
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item"><a href="/">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-house-door" viewBox="0 0 16 16">
                            <path d="M8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 7.5v7a.5.5 0 0 0 .5.5h4.5a.5.5 0 0 0 .5-.5v-4h2v4a.5.5 0 0 0 .5.5H14a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.146-.354L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293zM2.5 14V7.707l5.5-5.5 5.5 5.5V14H10v-4a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v4z"/>
                        </svg> Home</a>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">User activity log</li>
                </ol>
            </nav>

            <!-- Log Container -->
        <div class="log-container">
            <h4 class="mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-clock-history" viewBox="0 0 16 16">
                    <path d="M8.515 1.019A7 7 0 0 0 8 1V0a8 8 0 0 1 .589.022zm2.004.45a7 7 0 0 0-.985-.299l.219-.976q.576.129 1.126.342zm1.37.71a7 7 0 0 0-.439-.27l.493-.87a8 8 0 0 1 .979.654l-.615.789a7 7 0 0 0-.418-.302zm1.834 1.79a7 7 0 0 0-.653-.796l.724-.69q.406.429.747.91zm.744 1.352a7 7 0 0 0-.214-.468l.893-.45a8 8 0 0 1 .45 1.088l-.95.313a7 7 0 0 0-.179-.483m.53 2.507a7 7 0 0 0-.1-1.025l.985-.17q.1.58.116 1.17zm-.131 1.538q.05-.254.081-.51l.993.123a8 8 0 0 1-.23 1.155l-.964-.267q.069-.247.12-.501m-.952 2.379q.276-.436.486-.908l.914.405q-.24.54-.555 1.038zm-.964 1.205q.183-.183.35-.378l.758.653a8 8 0 0 1-.401.432z"/>
                    <path d="M8 1a7 7 0 1 0 4.95 11.95l.707.707A8.001 8.001 0 1 1 8 0z"/>
                    <path d="M7.5 3a.5.5 0 0 1 .5.5v5.21l3.248 1.856a.5.5 0 0 1-.496.868l-3.5-2A.5.5 0 0 1 7 9V3.5a.5.5 0 0 1 .5-.5"/>
                </svg> User Activity Log
            </h4>
            
            <div class="scrollable-table">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Entry ID</th>
                            <th>Action</th>
                            <th>Details</th>
                            <th>Done By</th>
                            <th>Timestamp</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${logEntries.isEmpty()}">
                                <tr>
                                    <td colspan="5">No data</td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach items="${logEntries}" var="entry">
                                    <tr>
                                        <th>${entry.getId()}</th>
                                        <td>${entry.getAction()}</td>
                                        <td>${entry.getDetails()}</td>
                                        <td>${entry.user == null ? 'Deleted User' : entry.getUser().getUsername()}</td>
                                        <td class="timestamp">${entry.getTimeStampFormatted()}</td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
        </div>
    </body>
</html>