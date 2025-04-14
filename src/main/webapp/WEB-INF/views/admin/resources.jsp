<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">

        <c:import url="../elements/header.jsp" />

        <style>
            .resources-container {
                background: white;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                padding: 20px;
                margin-top: 20px;
            }
            
            .disk-usage-section {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 20px;
                margin-bottom: 30px;
            }
            
            .disk-section {
                background: #f8f9fa;
                border-radius: 8px;
                padding: 15px;
                position: relative;
            }
            
            .disk-section h5 {
                border-bottom: 1px solid #dee2e6;
                padding-bottom: 10px;
                margin-bottom: 15px;
            }
            
            .disk-table {
                width: 100%;
            }
            
            .disk-table th, .disk-table td {
                padding: 8px 12px;
                text-align: left;
            }
            
            .disk-table tr:nth-child(even) {
                background-color: #f1f1f1;
            }
            
            .progress {
                height: 6px;
                margin-top: 5px;
            }
            
            .purge-btn {
                position: absolute;
                top: 15px;
                right: 15px;
            }
            
            @media (max-width: 768px) {
                .disk-usage-section {
                    grid-template-columns: 1fr;
                }
                .purge-btn {
                    position: static;
                    margin-top: 10px;
                    width: 100%;
                }
            }
        </style>

        <script>
            function processDeletionEvent() {
                if (confirm('Are you sure you want to purge the temp folder?')) {
                    return true;
                } else {
                    event.preventDefault();
                    return false;
                }
            }
        </script>

        <title>Nyx - Admin panel - Resources</title>
    </head>
    <body>
        <c:import url="../elements/navbar.jsp" />

        <div class="container-fluid mt-3">
            <!-- Breadcrumb -->
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item"><a href="/">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-house-door" viewBox="0 0 16 16">
                            <path d="M8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 7.5v7a.5.5 0 0 0 .5.5h4.5a.5.5 0 0 0 .5-.5v-4h2v4a.5.5 0 0 0 .5.5H14a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.146-.354L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293zM2.5 14V7.707l5.5-5.5 5.5 5.5V14H10v-4a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v4z"/>
                        </svg> Home</a></li>
                    <li class="breadcrumb-item active" aria-current="page">System resources</li>
                </ol>
            </nav>

            <c:import url="../elements/alerts.jsp" />

            <!-- Resources Container -->
            <div class="resources-container">
                <h4 class="mb-4">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-hdd" viewBox="0 0 16 16">
                        <path d="M4.5 11a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1M3 10.5a.5.5 0 1 1-1 0 .5.5 0 0 1 1 0"/>
                        <path d="M16 11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V9.51c0-.418.105-.83.305-1.197l2.472-4.531A1.5 1.5 0 0 1 4.094 3h7.812a1.5 1.5 0 0 1 1.317.782l2.472 4.53c.2.368.305.78.305 1.198zM3.655 4.26 1.592 8.043Q1.79 8 2 8h12q.21 0 .408.042L12.345 4.26a.5.5 0 0 0-.439-.26H4.094a.5.5 0 0 0-.44.26zM1 10v1a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-1a1 1 0 0 0-1-1H2a1 1 0 0 0-1 1"/>
                    </svg> Disk Usage
                </h4>
                
                <div class="disk-usage-section">
                    <!-- Data Folder Section -->
                    <div class="disk-section">
                        <h5>
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-folder" viewBox="0 0 16 16">
                                <path d="M.54 3.87.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.826a2 2 0 0 1-1.991-1.819l-.637-7a2 2 0 0 1 .342-1.31zM2.19 4a1 1 0 0 0-.996 1.09l.637 7a1 1 0 0 0 .995.91h10.348a1 1 0 0 0 .995-.91l.637-7A1 1 0 0 0 13.81 4zm4.69-1.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139q.323-.119.684-.12h5.396z"/>
                            </svg> Data Folder
                        </h5>
                        <table class="disk-table">
                            <tr>
                                <th>Folder Path:</th>
                                <td>${dataFolderPath}</td>
                            </tr>
                            <tr>
                                <th>Partition Total:</th>
                                <td>${dataFolderTotalDiskSpace} MB</td>
                            </tr>
                            <tr>
                                <th>Partition Free:</th>
                                <td>${dataFolderFreeDiskSpace} MB</td>
                            </tr>
                            <tr>
                                <th>Folder Size:</th>
                                <td>${dataFolderSize} MB</td>
                            </tr>
                            <tr>
                                <th>Disk used:</th>
                                <td>${dataFolderDiskUsagePercent}% 
                                    <c:choose>
                                        <c:when test="${dataFolderDiskUsagePercent >= 0 && dataFolderDiskUsagePercent < 50}">
                                            <div class="progress" role="progressbar" aria-label="${dataFolderDiskUsagePercent}%" aria-valuenow="${dataFolderDiskUsagePercent}" aria-valuemin="0" aria-valuemax="100">
                                                <div class="progress-bar progress-bar-striped progress-bar-animated bg-success" style="width: ${dataFolderDiskUsagePercent}%"></div>
                                            </div>
                                        </c:when>
                                        <c:when test="${dataFolderDiskUsagePercent >= 50 && dataFolderDiskUsagePercent < 85}">
                                            <div class="progress" role="progressbar" aria-label="${dataFolderDiskUsagePercent}%" aria-valuenow="${dataFolderDiskUsagePercent}" aria-valuemin="0" aria-valuemax="100">
                                                <div class="progress-bar progress-bar-striped progress-bar-animated bg-warning" style="width: ${dataFolderDiskUsagePercent}%"></div>
                                            </div>
                                        </c:when>
                                        <c:when test="${dataFolderDiskUsagePercent >= 85 && dataFolderDiskUsagePercent <= 100}">
                                            <div class="progress" role="progressbar" aria-label="${dataFolderDiskUsagePercent}%" aria-valuenow="${dataFolderDiskUsagePercent}" aria-valuemin="0" aria-valuemax="100">
                                                <div class="progress-bar progress-bar-striped progress-bar-animated bg-danger" style="width: ${dataFolderDiskUsagePercent}%"></div>
                                            </div>
                                        </c:when>
                                    </c:choose>
                                </td>
                            </tr>
                        </table>
                    </div>
                    
                    <!-- Temp Folder Section -->
                    <div class="disk-section">
                        <h5>
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-folder" viewBox="0 0 16 16">
                                <path d="M.54 3.87.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.826a2 2 0 0 1-1.991-1.819l-.637-7a2 2 0 0 1 .342-1.31zM2.19 4a1 1 0 0 0-.996 1.09l.637 7a1 1 0 0 0 .995.91h10.348a1 1 0 0 0 .995-.91l.637-7A1 1 0 0 0 13.81 4zm4.69-1.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139q.323-.119.684-.12h5.396z"/>
                            </svg> Temp Folder
                        </h5>
                        <form method="post" action="resources/purgeTemp" id="delete_form"></form>
                        <button class="btn btn-sm btn-danger purge-btn" id="purgeTempBtn" onclick="processDeletionEvent()" form="delete_form">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-trash" viewBox="0 0 16 16">
                                <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0z"/>
                                <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4zM2.5 3h11V2h-11z"/>
                            </svg> Purge Temp Folder
                        </button>
                        <table class="disk-table">
                            <tr>
                                <th>Folder Path:</th>
                                <td>${tempFolderPath}</td>
                            </tr>
                            <tr>
                                <th>Partition Total:</th>
                                <td>${tempFolderTotalDiskSpace} MB</td>
                            </tr>
                            <tr>
                                <th>Partition Free:</th>
                                <td>${tempFolderFreeDiskSpace} MB</td>
                            </tr>
                            <tr>
                                <th>Folder Size:</th>
                                <td>${tempFolderSize} MB</td>
                            </tr>
                            <tr>
                                <th>Disk used:</th>
                                <td>${tempFolderDiskUsagePercent}% 
                                    <c:choose>
                                        <c:when test="${tempFolderDiskUsagePercent >= 0 && tempFolderDiskUsagePercent < 50}">
                                            <div class="progress" role="progressbar" aria-label="${tempFolderDiskUsagePercent}%" aria-valuenow="${tempFolderDiskUsagePercent}" aria-valuemin="0" aria-valuemax="100">
                                                <div class="progress-bar progress-bar-striped progress-bar-animated bg-success" style="width: ${tempFolderDiskUsagePercent}%"></div>
                                            </div>
                                        </c:when>
                                        <c:when test="${tempFolderDiskUsagePercent >= 50 && tempFolderDiskUsagePercent < 85}">
                                            <div class="progress" role="progressbar" aria-label="${tempFolderDiskUsagePercent}%" aria-valuenow="${tempFolderDiskUsagePercent}" aria-valuemin="0" aria-valuemax="100">
                                                <div class="progress-bar progress-bar-striped progress-bar-animated bg-warning" style="width: ${tempFolderDiskUsagePercent}%"></div>
                                            </div>
                                        </c:when>
                                        <c:when test="${tempFolderDiskUsagePercent >= 85 && tempFolderDiskUsagePercent <= 100}">
                                            <div class="progress" role="progressbar" aria-label="${tempFolderDiskUsagePercent}%" aria-valuenow="${tempFolderDiskUsagePercent}" aria-valuemin="0" aria-valuemax="100">
                                                <div class="progress-bar progress-bar-striped progress-bar-animated bg-danger" style="width: ${tempFolderDiskUsagePercent}%"></div>
                                            </div>
                                        </c:when>
                                    </c:choose>
                                </td>
                            </tr>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </body>
</html>