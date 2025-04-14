var overallDetailsSection = null;
var nodeDetailsSections = new Map();

/**
 * Resets both overall and per-node sections content, effectively resetting the page to it's initial state.
 */
export function resetState() {
    if (overallDetailsSection) {
        overallDetailsSection.resetState();
    }

    for (const [nodeId, section] of nodeDetailsSections) {
        section.resetState();
    }
}

/**
 * Entry point for telemetry data processing - parses data packet from server and updates existing charts/tables.
 * NOTE: If both telemetry checkboxes are unchecked - function will not perform at all, saving CPU time.
 * @param {Object} resultsObject - Data object, taken from WS packet.
 */
export function tickResults(resultsObject) {
    let overallTelemetryToggle = document.getElementById("overallTelemetryToggle");
    let perNodeTelemetryToggle = document.getElementById("nodeTelemetryToggle");

    if (!overallTelemetryToggle.checked && !perNodeTelemetryToggle.checked) {
        return;
    }

    let timestamp = Date.now();

    let totalValues = new Map();
    let nodeValues = new Map();

    for (const [nodeId, nodeValue] of Object.entries(resultsObject)) {
        
        if(!nodeValues.has(nodeId)) {
            nodeValues.set(nodeId, new Map());
        }

        for (const [requestName, requestValue] of Object.entries(nodeValue)) {
            if (!totalValues.has(requestName)) {
                if (requestName !== "users")
                    totalValues.set(requestName, { averageResponseTime: 0, RPS: 0, FailuresCount: 0 });
                else 
                    totalValues.set(requestName, { amount: 0 });
            }
            if (!nodeValues.get(nodeId).has(requestName)) {
                if (requestName !== "users")
                    nodeValues.get(nodeId).set(requestName, { averageResponseTime: 0, RPS: 0, FailuresCount: 0 });
                else 
                    nodeValues.get(nodeId).set(requestName, { amount: 0 });
            }

            let totalEntry = totalValues.get(requestName);
            if (requestName === "users") {
                totalEntry.amount += requestValue[0];
                nodeValues.get(nodeId).get(requestName).amount = requestValue[0];
            } else {
                totalEntry.averageResponseTime += requestValue[0];
                nodeValues.get(nodeId).get(requestName).averageResponseTime = requestValue[0];
                totalEntry.RPS += requestValue[1];
                nodeValues.get(nodeId).get(requestName).RPS = requestValue[1];
                totalEntry.FailuresCount += requestValue[2];
                nodeValues.get(nodeId).get(requestName).FailuresCount = requestValue[2];
            }
        }
    }

    // Once everything's populated - we need to calculate correct average response time for all requests, since now they are accumulated values
    for (const [key, value] of totalValues) {
        if (key !== "users") {
            let newValue = value.averageResponseTime / nodeValues.size;
            if (newValue !== NaN)
                value.averageResponseTime = newValue;
            else
                value.averageResponseTime = 0;
        }
    }

    if (overallTelemetryToggle.checked) {
        if (overallDetailsSection === null) {
            overallDetailsSection = new OverallDetailsSection(document.getElementById("overall_stats_container"));
        }
        overallDetailsSection.processInput(totalValues, timestamp);
    }

    if (perNodeTelemetryToggle.checked) {
        for (const [nodeId, values] of nodeValues) {
            if (!nodeDetailsSections.has(nodeId)) {
                nodeDetailsSections.set(nodeId, new NodeDetailsSection(nodeId, document.getElementById("node_stats_container")));
            }
            nodeDetailsSections.get(nodeId).processInput(values, timestamp);
        }
    }
}

/**
 * Base class for telemetry sections - provides base functional logic for both overall and per-node containers.
 * NOTE: Methods in this class rely on existance of certain variables (namely - charts and tables), so make sure to initialize
 * them in sub-class constructors
 */
class DetailsSection {

    /**
     * Main workhorse of the section - processes the data and creates/updates data entries.
     * @param {Map} dataMap         - Map with processed data (passed down from tickResults top-level function)
     * @param {number} timestamp    - Epoch timestamp.
     */
    processInput(dataMap, timestamp) {
        for (const [key, value] of dataMap) {
            if (key === "users") {
                if (this.usersChart.data.datasets.length == 0) {
                    const datasetIndex = this.usersChart.data.datasets.length;
                    this.usersChart.data.datasets.push({
                        label: "Total Users",
                        data: [],
                        fill: 'origin',
                        borderColor: getNextColor(datasetIndex),
                        backgroundColor: getNextColor(datasetIndex)
                    });
                }
                this.usersChart.data.datasets[0].data.push({ x: timestamp, y: value.amount });
            } else {
                if (!this.avgResponseTimesChart.data.datasets.some(entry => entry.label === key)) {
                    const datasetIndex = this.avgResponseTimesChart.data.datasets.length;
                    this.avgResponseTimesChart.data.datasets.push({
                        label: key,
                        data: [],
                        fill: 'origin',
                        borderColor: getNextColor(datasetIndex),
                        backgroundColor: getNextColor(datasetIndex)
                    });
                }
                this.avgResponseTimesChart.data.datasets.find(entry => entry.label === key).data.push({ x: timestamp, y: value.averageResponseTime });
    
                if (!this.totalRPSChart.data.datasets.some(entry => entry.label === key)) {
                    const datasetIndex = this.totalRPSChart.data.datasets.length;
                    this.totalRPSChart.data.datasets.push({
                        label: key,
                        data: [],
                        fill: 'origin',
                        borderColor: getNextColor(datasetIndex),
                        backgroundColor: getNextColor(datasetIndex)
                    });
                }
                this.totalRPSChart.data.datasets.find(entry => entry.label === key).data.push({ x: timestamp, y: value.RPS });
                this.totalRequests += value.RPS;
    
                if (!this.totalFailuresChart.data.datasets.some(entry => entry.label === key)) {
                    const datasetIndex = this.totalFailuresChart.data.datasets.length;
                    this.totalFailuresChart.data.datasets.push({
                        label: key,
                        data: [],
                        fill: 'origin',
                        borderColor: getNextColor(datasetIndex),
                        backgroundColor: getNextColor(datasetIndex)
                    });
                }
                this.totalFailuresChart.data.datasets.find(entry => entry.label === key).data.push({ x: timestamp, y: value.FailuresCount });
                this.totalFailures += value.FailuresCount;

                if (!this.totalRequestsAndFailuresByRequest.has(key)) {
                    this.totalRequestsAndFailuresByRequest.set(key, new DetailsSectionTableRow(this.dataTableBody, key));
                }
                this.totalRequestsAndFailuresByRequest.get(key).submitAndUpdate(value);
            }            
        }

        this.successDoughnutChart.data.datasets[0].data[0] = this.totalRequests;
        this.successDoughnutChart.data.datasets[0].data[1] = this.totalFailures;

        this.avgResponseTimesChart.update();
        this.totalRPSChart.update();
        this.totalFailuresChart.update();
        this.usersChart.update();
        this.successDoughnutChart.update();
    }

    /**
     * Resets contents state - all charts and tables
     */
    resetState() {
        this.totalRequests = 0;
        this.totalFailures = 0;

        if (this.avgResponseTimesChart.data.datasets.length > 0)
            this.avgResponseTimesChart.data.datasets = [];
        if (this.totalRPSChart.data.datasets.length > 0)
            this.totalRPSChart.data.datasets = [];
        if (this.totalFailuresChart.data.datasets.length > 0)
            this.totalFailuresChart.data.datasets = [];
        if (this.usersChart.data.datasets.length > 0)
            this.usersChart.data.datasets = [];
        if (this.successDoughnutChart.data.datasets.length > 0)
            this.successDoughnutChart.data = {
                labels: ["Passed", "Failed"],
                datasets: [{
                    data: [0, 0],
                    backgroundColor: ["#97cc64", "#fd5a3e"],
                    hoverOffset: 4
                }]
            }
    }
    
    /**
     * Line chart initializer - creates a ChartJS instance for given canvas.
     * @param {Element} container   - Canvas element
     * @param {String} title        - Chart title
     * @param {String} metricName   - Active metric (Y-axis) name.
     * @returns                     - Chart instance
     */
    _initLineChart(container, title, metricName) {
        return new Chart(
            container,
            {
                type: "line",
                data: {
                    datasets: []
                },
                options: {
                    maintainAspectRatio: false,
                    responsive: true,
                    interaction: {
                        mode: 'nearest',
                        axis: 'xy',
                        intersect: true
                    },
                    plugins: {
                        title: {
                            display: true,
                            text: title
                        },
                        legend: {
                            display: false,
                        },
                        tooltip: {
                            enabled: true,
    
                        },
                        datalabels: {
                            display: false
                        },
                        tooltip: {
                            enabled: true
                        }
                    },
                    scales: {
                        x: {
                            type: 'time',
                            display: false,
                            time: {
                                unit: 'second',
                                displayFormats: {
                                    second: 'yyyy.MM.DD : HH.mm.ss'
                                },
                                tooltipFormat: 'yyyy.MM.DD : HH.mm.ss'
                            },
                            title: {
                                display: true,
                                text: 'Timestamp'
                            }
                        },
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            title: {
                                display: false,
                                text: metricName
                            }
                        }
                    }
                }
            }
        );
    }
    
    /**
     * Success rate doughnut chart initializer - creates a ChartJS instance for given canvas.
     * @param {Element} container   - Canvas element
     * @returns                     - Chart instance
     */
    _initSuccessRateDoughnutChart(container) {
        return new Chart(
            container,
            {
                type: 'doughnut',
                options: {
                    maintainAspectRatio: true,
                    plugins: {
                        datalabels: {
                            formatter: (value, ctx) => {
                                const datapoints = ctx.chart.data.datasets[0].data
                                const total = datapoints.reduce((total, datapoint) => total + datapoint, 0)
                                const percentage = value / total * 100
                                return percentage.toFixed(2) + "%";
                            },
                            color: '#fff',
                        },
                        legend: {
                            display: false
                        },
                        title: {
                            display: true,
                            text: "Requests success rate, %"
                        },
                    }
                },
                data: {
                    labels: ["Passed", "Failed"],
                    datasets: [{
                        data: [0, 0],
                        backgroundColor: ["#97cc64", "#fd5a3e"],
                        hoverOffset: 4
                    }]
                }
            }
        )
    }
}

/**
 * Overall details section implementation - provides an object representation for overall test run data section.
 * Handles charts and tables creation and their subsequent updates
 */
class OverallDetailsSection extends DetailsSection {
    constructor(sectionContainer) {
        super();
        this.totalRequests = 0;
        this.totalFailures = 0;
        this.totalRequestsAndFailuresByRequest = new Map();

        sectionContainer.innerHTML = `
            <!-- Top: 2x2 Line Charts Grid -->
            <div class="line-charts-grid">
                <div class="chart-container">
                    <canvas id="responseTimeChart"></canvas>
                </div>
                <div class="chart-container">
                    <canvas id="rpsChart"></canvas>
                </div>
                <div class="chart-container">
                    <canvas id="failuresChart"></canvas>
                </div>
                <div class="chart-container">
                    <canvas id="usersChart"></canvas>
                </div>
            </div>
            
            <!-- Bottom: Doughnut + Table -->
            <div class="bottom-stats-row">
                <div class="doughnut-wrapper">
                    <div class="doughnut-container">
                        <canvas id="successRateChart"></canvas>
                    </div>
                </div>
                <div class="requests-table-wrapper">
                    <div class="scrollable-table-sm">
                        <table class="table table-sm">
                            <thead>
                                <tr>
                                    <th>Request</th>
                                    <th>Passed</th>
                                    <th>Failed</th>
                                    <th>Fail rate, %</th>
                                </tr>
                            </thead>
                            <tbody id="overall_data_table_body"></tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        this.dataTableBody          = document.getElementById("overall_data_table_body");
        this.avgResponseTimesChart  = this._initLineChart(document.getElementById("responseTimeChart"), "Average response times", "Response time, ms");;
        this.totalRPSChart          = this._initLineChart(document.getElementById("rpsChart"), "Responses per second", "Requests per second");
        this.totalFailuresChart     = this._initLineChart(document.getElementById("failuresChart"), "Failures amount", "Failures");
        this.usersChart             = this._initLineChart(document.getElementById("usersChart"), "Users amount", "Users");
        this.successDoughnutChart   = this._initSuccessRateDoughnutChart(document.getElementById("successRateChart"));
    }
}

/**
 * Per-node data container implementation - handles the data for individual node containers.
 */
class NodeDetailsSection extends DetailsSection {
    constructor(nodeId, sectionContainer) {
        super();
        this.nodeId = nodeId;
        this.nodeStippedName = nodeId.replace(/\s+/g, '');

        this.totalRequests = 0;
        this.totalFailures = 0;
        this.totalRequestsAndFailuresByRequest = new Map();

        let sectionWrapper = document.createElement("div");
        sectionWrapper.id = this.nodeStippedName + "_section_wrapper";
        
        let sectionContents = `
            <div class="node-stat-card">
                <h5>
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-hdd-network" viewBox="0 0 16 16">
                        <path d="M4.5 5a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1M3 4.5a.5.5 0 1 1-1 0 .5.5 0 0 1 1 0"/>
                        <path d="M0 4a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v1a2 2 0 0 1-2 2H8.5v3a1.5 1.5 0 0 1 1.5 1.5h5.5a.5.5 0 0 1 0 1H10A1.5 1.5 0 0 1 8.5 14h-1A1.5 1.5 0 0 1 6 12.5H.5a.5.5 0 0 1 0-1H6A1.5 1.5 0 0 1 7.5 10V7H2a2 2 0 0 1-2-2zm1 0v1a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1H2a1 1 0 0 0-1 1m6 7.5v1a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5"/>
                    </svg> ${nodeId}</h5>
                <div class="node-stats-scrollable">
                    <div class="chart-container">
                        <canvas id="${this.nodeStippedName}ResponseTime"></canvas>
                    </div>
                    <div class="chart-container">
                        <canvas id="${this.nodeStippedName}Rps"></canvas>
                    </div>
                    <div class="chart-container">
                        <canvas id="${this.nodeStippedName}Failures"></canvas>
                    </div>
                    <div class="chart-container">
                        <canvas id="${this.nodeStippedName}Users"></canvas>
                    </div>
                    <div class="doughnut-container">
                        <canvas id="${this.nodeStippedName}SuccessRate"></canvas>
                    </div>
                    <div class="scrollable-table-sm">
                        <table class="table table-sm">
                            <thead>
                                <tr>
                                    <th>Request</th>
                                    <th>Passed</th>
                                    <th>Failed</th>
                                    <th>Fail rate, %</th>
                                </tr>
                            </thead>
                            <tbody id="${this.nodeStippedName}DetailsTable"></tbody>
                        </table>
                    </div>
                </div>
            </div>`;
        sectionWrapper.innerHTML = sectionContents;
        sectionContainer.append(sectionWrapper);
        this.dataTableBody = document.getElementById(`${this.nodeStippedName}DetailsTable`);

        this.avgResponseTimesChart  = this._initLineChart(document.getElementById(`${this.nodeStippedName}ResponseTime`), "Average response times", "Response time, ms");;
        this.totalRPSChart          = this._initLineChart(document.getElementById(`${this.nodeStippedName}Rps`), "Responses per second", "Requests per second");
        this.totalFailuresChart     = this._initLineChart(document.getElementById(`${this.nodeStippedName}Failures`), "Failures amount", "Failures");
        this.usersChart             = this._initLineChart(document.getElementById(`${this.nodeStippedName}Users`), "Users amount", "Users");
        this.successDoughnutChart   = this._initSuccessRateDoughnutChart(document.getElementById(`${this.nodeStippedName}SuccessRate`));
    }
}

/**
 * Small object abstraction for individual data table rows. Makes it easier to create and update entries.
 */
class DetailsSectionTableRow {
    constructor(tbodyElement, requestName) {
        this.requestName = requestName;

        this.requests           = 0;
        this.failures           = 0;
        this.failurePercentage  = 0.0;

        let row = document.createElement("tr");
        this.nameElement                = document.createElement("td");
        this.nameElement.textContent    = requestName;
        row.appendChild(this.nameElement);
        this.requestsElement            = document.createElement("td");
        row.appendChild(this.requestsElement);
        this.failuresElement            = document.createElement("td");
        row.appendChild(this.failuresElement);
        this.failuresPercentageElement  = document.createElement("td");
        row.appendChild(this.failuresPercentageElement);
        tbodyElement.appendChild(row);
    }

    submitAndUpdate(data) {
        this.requests += data.RPS;
        this.failures += data.FailuresCount;

        let newPercentile       = Math.floor((this.failures / this.requests) * 100);
        this.failurePercentage  = (isNaN(newPercentile) ? 0 : newPercentile);

        this.requestsElement.textContent            = this.requests;
        this.failuresElement.textContent            = this.failures;
        this.failuresPercentageElement.textContent  = this.failurePercentage;
    }
}

// #######################################################################
// Misc and utilities
// #######################################################################

function getNextColor(index) {
    return colors[index % colors.length];
}

const colors = [
    'rgba(255, 159, 64, 0.2)',      // Soft orange
    'rgba(0, 139, 139, 0.2)',       // Dark cyan
    'rgba(54, 162, 235, 0.2)',      // Sky blue
    'rgba(255, 99, 132, 0.2)',      // Coral pink
    'rgba(153, 102, 255, 0.2)',     // Lavender
    'rgba(255, 205, 86, 0.2)',      // Golden yellow
    'rgba(75, 192, 192, 0.2)',      // Teal
    'rgba(201, 203, 207, 0.2)',     // Light gray
    'rgba(255, 140, 0, 0.2)',       // Dark orange
    'rgba(255, 69, 0, 0.2)',        // Bright red-orange
    'rgba(0, 128, 128, 0.2)',       // Dark teal
    'rgba(30, 144, 255, 0.2)',      // Dodger blue
    'rgba(147, 112, 219, 0.2)',     // Medium purple
    'rgba(220, 20, 60, 0.2)',       // Crimson
];