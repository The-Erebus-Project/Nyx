<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ page import="java.util.stream.IntStream" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">

        <c:import url="elements/header.jsp" />
        <script src="/chart.js"></script>
        <script src="/chartjs-plugin-datalabels.js"></script>
        <script>Chart.register(ChartDataLabels);</script>
        <script type="module" src="/js/project/project_procedures.js"></script>

        <style>
            .runs-container {
                background: white;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                padding: 20px;
                margin-top: 20px;
            }
            
            .scrollable-table {
                max-height: calc(100vh - 300px);
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
            
            @keyframes pulse {
                0% { opacity: 1; }
                50% { opacity: 0.7; }
                100% { opacity: 1; }
            }
            
            .action-bar {
                margin-top: 20px;
                display: flex;
                justify-content: flex-end;
            }
        </style>

        <title>Nyx - ${projectName} project runs list</title>
    </head>
    <body>
        <div class="main-container">
            <c:import url="elements/navbar.jsp" />
            <!-- Main Content -->
            <div class="container-fluid mt-3">
                <!-- Breadcrumb -->
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb">
                        <li class="breadcrumb-item"><a href="/" >
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-house-door" viewBox="0 0 16 16">
                                <path d="M8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 7.5v7a.5.5 0 0 0 .5.5h4.5a.5.5 0 0 0 .5-.5v-4h2v4a.5.5 0 0 0 .5.5H14a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.146-.354L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293zM2.5 14V7.707l5.5-5.5 5.5 5.5V14H10v-4a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v4z"/>
                            </svg> Home</a>
                        </li>
                        <li class="breadcrumb-item"><a href="/project/${projectID}" >${projectName} page</a></li>
                        <li class="breadcrumb-item active" aria-current="page">${projectName} runs</li>
                    </ol>
                </nav>
                
                <!-- Runs Container -->
                <div class="runs-container">
                    <h4><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-list-check" viewBox="0 0 16 16">
                        <path fill-rule="evenodd" d="M5 11.5a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9a.5.5 0 0 1-.5-.5M3.854 2.146a.5.5 0 0 1 0 .708l-1.5 1.5a.5.5 0 0 1-.708 0l-.5-.5a.5.5 0 1 1 .708-.708L2 3.293l1.146-1.147a.5.5 0 0 1 .708 0m0 4a.5.5 0 0 1 0 .708l-1.5 1.5a.5.5 0 0 1-.708 0l-.5-.5a.5.5 0 1 1 .708-.708L2 7.293l1.146-1.147a.5.5 0 0 1 .708 0m0 4a.5.5 0 0 1 0 .708l-1.5 1.5a.5.5 0 0 1-.708 0l-.5-.5a.5.5 0 0 1 .708-.708l.146.147 1.146-1.147a.5.5 0 0 1 .708 0"/>
                    </svg> Test Runs</h4>
                    
                    <c:choose>
                        <c:when test="${not empty allProjectRunsData}">
                            <form method="post" action="/project/${projectID}/deleteRuns">
                                <div class="scrollable-table">
                                    <table class="table table-hover">
                                        <thead>
                                            <tr>
                                                <th scope="col">
                                                    <sec:authorize access="hasRole('ADMIN')">
                                                        <input type="checkbox" class="form-check-input" onclick="projectActions.processRunsTableHeaderCheckbox(this)"/>
                                                    </sec:authorize>
                                                </th>
                                                <th>Run ID</th>
                                                <th>Summary</th>
                                                <th>Details</th>
                                                <th>Start Time</th>
                                                <th>Finish Time</th>
                                                <th>Duration</th>
                                                <th>Status</th>
                                            </tr>
                                        </thead>
                                        <tbody id="all_runs_table_rows">
                                            <c:forEach items="${invertedProjectRunsData}" var="run">
                                                <tr>
                                                    <td>
                                                        <sec:authorize access="hasRole('ADMIN')">
                                                            <input type="checkbox" name="runId" value="${run.getId()}" />
                                                        </sec:authorize>
                                                    </td>
                                                    <th scope="row">
                                                        <a href="/${projectID}/${run.getId()}/index.html" >${run.getId()}</a>
                                                    </th>
                                                    <td>${run.getRunSummary()}</td>
                                                    <td style="width: 600px;">
                                                        <p class="d-inline-flex gap-1">
                                                            <a class="btn btn-primary" data-bs-toggle="collapse" href="#run_${run.getId()}_description" role="button" aria-expanded="false" aria-controls="run_${run.getId()}_description">
                                                              View/Collapse description
                                                            </a>
                                                        </p>
                                                        <div class="collapse" id="run_${run.getId()}_description">
                                                            <div class="card card-body" style="text-align: left;">
                                                                ${run.getRunDescription().replaceAll("\\n", "<br>")}
                                                            </div>
                                                        </div>
                                                    </td>
                                                    <td>${run.getFormattedStartTime()}</td>
                                                    <td>${run.getFormattedFinishTime()}</td>
                                                    <td>${run.getFormattedTotalRunTime()}</td>
                                                    <td>
                                                        <c:choose>
                                                            <c:when test="${run.getStatus()=='FINISHED'}">
                                                                <span class="badge text-bg-success">Finished</span>
                                                            </c:when>
                                                            <c:when test="${run.getStatus()=='CANCELLED'}">
                                                                <span class="badge text-bg-warning">Cancelled</span>
                                                            </c:when>  
                                                            <c:otherwise>
                                                                <span class="badge text-bg-secondary">${run.getStatus()}</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <td>
                                                        <a class="btn btn-primary" href="/${projectID}/${run.getId()}/report.zip">Download</a>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                                <sec:authorize access="hasRole('ADMIN')">
                                    <div class="action-bar">
                                        <button type="submit" class="btn btn-danger" onclick="projectActions.processRunDeletionEvent()">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-trash" viewBox="0 0 16 16">
                                                <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0z"/>
                                                <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4zM2.5 3h11V2h-11z"/>
                                            </svg> Delete Selected Runs
                                        </button>
                                    </div>
                                </sec:authorize>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <h5>No recorded runs found</h5>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </body>
</html>