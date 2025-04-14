import { ProjectNode } from "/js/project/ProjectNode.js"
import { tickResults, resetState } from "/js/project/RunDetails.js"

var socket = null;
var nodes = new Map();

// ##########################################################################################
// WS Functions
// ##########################################################################################

/**
 * Page entry point - connects it to Socket-IO server.
 * @param {Int} projectId       - Project ID
 * @param {string} sessionId    - JSESSIONID token string
 */
export function connect(projectId, sessionId) {
    if (socket === null) {
        socket = io(
            window.location.hostname + ':8081/projects', 
            { 
                query: { sessionId: sessionId},
                transports: ["websocket"]
            }
        );

        socket.on("connect", function() {
            joinRoom(projectId);
            populateNodes(projectId);
        });

        socket.on("disconnect", function() {
            socket.disconnect();
            createAndDisplayToastMessage(TOAST_TYPES.WARN, "You were disconnected from server. Refresh the page to re-connect", false);
        });

        socket.on("nodeCreated", function(data) {
            let node = ProjectNode.fromObject(data);
            let row = node.toNodesListEntry();
            $("#nodes_table_rows").append(row);
            createAndDisplayToastMessage(TOAST_TYPES.INFO, "Node with ID '" + data.nodeId + "' was created");
        });

        socket.on("nodesRemoved", function(data) {
            data.forEach((node) => deleteNodeRowEntry(node));
            createAndDisplayToastMessage(TOAST_TYPES.INFO, "Nodes '" + data + "' were removed");
        });

        socket.on("nodeUpdated", function(data) {
            let node = ProjectNode.fromObject(data);
            nodes.set(node.nodeId, node);

            let existingRow = document.getElementById(node.nodeId + "_row");
            if (existingRow === null) {
                let row = node.toNodesListEntry();
                $("#nodes_table_rows").append(row);
                createAndDisplayToastMessage(TOAST_TYPES.INFO, "Node with ID '" + data.nodeId + "' was updated");
            } else {
                existingRow.replaceWith(node.toNodesListEntry());
                createAndDisplayToastMessage(TOAST_TYPES.INFO, "Node with ID '" + data.nodeId + "' was updated");
            }
            regenerateRunDescription();
        });

        socket.on("runStarted", function(data) {
            createAndDisplayToastMessage(TOAST_TYPES.INFO, "Run '" + data + "' is started");
            resetState();
        });

        socket.on("runResultsTick", function(data) {
            tickResults(JSON.parse(data));
        });

        socket.on("runFinished", function(data) {
            createAndDisplayToastMessage(TOAST_TYPES.INFO, "Run '" + data + "' have finished", false);
        });

        socket.on("runSummaryUpdated", function(data) {
            $("#runSummary").val(data);
        });
    }
}

/**
 * Joins current socket to a given room - in our case it is project's room.
 * @param {Int} projectId - Project ID
 */
function joinRoom(projectId) {
    socket.emit(
        "joinRoom", 
        projectId,
        function(response) {
            if (response !== 3) {
                createAndDisplayToastMessage(TOAST_TYPES.ERROR, "Failed to join room " + projectId + ". Response from server: " + response);
            } else {
                createAndDisplayToastMessage(TOAST_TYPES.INFO, "Joined room for project ID " + projectId);
            }
        }
    );
}

// ##########################################################################################
// Processing functions
// ##########################################################################################

/**
 * Initial state set-up for nodes table - fetches the values and creates table entries for each one.
 * NOTE: Updates and additions/removals are handled dynamically by other signals
 * @param {Int} projectId - ID of a project
 */
function populateNodes(projectId) {
    socket.emit(
        "getNodes", 
        JSON.stringify({projectId: parseInt(projectId)}),
        function(response) {
            nodes.clear();

            $("#nodes_table_rows").empty();
            if (response.length > 0) {
                response.forEach(nodeData => {
                    let node = ProjectNode.fromObject(nodeData);
                    nodes.set(node.nodeId, node);

                    let row = node.toNodesListEntry();
                    $("#nodes_table_rows").append(row);
                });

                regenerateRunDescription();
            } else {
                let empty_row = document.createElement("tr");
                empty_row.classList.add("nodes_placeholder");
                empty_row.innerHTML = '<td colspan="6">No nodes registered</td>';

                $("#nodes_table_rows").append(empty_row);
            }
        }
    ) 
}

/**
 * Node creation event processor - sends a request to create a node and processes the response
 * @param {Int} projectId - Project ID
 * @param {string} nodeId - Node ID
 * @param {string} description - Node description (Optional)
 */
function createNode(projectId, nodeId, description) {
    keepAlive();
    
    socket.emit(
        "createNode",
        JSON.stringify({projectId: projectId, nodeId: nodeId, description: description}),
        function(response) {
            if (response === 3) {
                $("#node_id_value").val("");
                $("#description_value").val("");

                $("#node_creation_modal").modal("hide");
            } else {
                $("#node_creation_alert_container").html(
                    [
                        '<div class="alert alert-danger alert-dismissible" role="alert">',
                        '   <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>',
                        response,
                        '</div>'
                    ].join("")
                )
            }
        }
    )
}


// ##########################################################################################
// Event handlers
// ##########################################################################################

/**
 * Trigger function - initiates node creation call, using input values
 * @param {Int} projectId - Project ID
 */
export function triggerNodeCreation(projectId) {
    let nodeId = $("#node_id_value").val();
    let description = $("#description_value").val();

    createNode(projectId, nodeId, description);
}

/**
 * Populates the node deletion modal window with entries for each existing node
 * @param {Int} projectId - Project ID
 */
export function populateNodeDeletionList(projectId) {
    keepAlive();

    clearNodeDeletionList();
    socket.emit(
        "getNodes",
        JSON.stringify({projectId: parseInt(projectId)}),
        function(response) {
            response.forEach(node => {
                let row = document.createElement("tr");
                row.innerHTML = `
                    <td><input type="checkbox" class="form-check-input node_id_checkbox" value="${node.nodeId}"></td>
                    <td>${node.nodeId}</td>
                `

                $("#node_deletion_nodes_list").append(row);
            });
        }
    )
}

/**
 * Executes the call to delete the nodes. Handles possible errors (non-3 response frame)
 * @param {Int} projectId - Project ID 
 */
export function deleteNodes(projectId) {
    keepAlive();

    let nodes = [];
    $("#node_deletion_nodes_list input:checked").each(function() {
        nodes.push($(this).val());
    });

    socket.emit(
        "removeNodes",
        JSON.stringify({projectId: projectId, nodes: nodes}),
        function(response) {
            clearNodeDeletionList();
            $("#node_deletion_modal").modal("hide");
            if (response !== 3) {
                createAndDisplayToastMessage(TOAST_TYPES.ERROR, response);
            }
        }
    );
}

function clearNodeDeletionList() {
    $("#node_deletion_nodes_list").empty();
}

/**
 * Used by "Select all" node checkbox - automatically checks all existing checkboxes for nodes deletion modal window
 * @param {Element} element - checkbox to attach event to
 */
export function processAllNodesSelectionDeselection(element) {
    if (element.checked == false) {
        $(".node_id_checkbox").each(function() {
            this.checked = false;
        });
    } else {
        $(".node_id_checkbox").each(function() {
            this.checked = true;
        });
    }
}

/**
 * Populates Edit Node Parameters modal window with current values and opens the modal window itself
 * @param {String} nodeId - Node ID to load
 */
export function openArgsEditModal(nodeId) {
    let modal_window = $("#node_args_edit_modal");
    let values_container = $("#node_args_edit_content");
    let node = nodes.get(nodeId);

    values_container.empty();
    values_container.html(node.toArgsEditModalWindowContent());
    modal_window.modal("show");

    // Bootstrap tooltips initialization
    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    });
}

/**
 * Sends a reservation call, marking node as "reserved" for upcoming run.
 * @param {*} element   - Node element
 * @param {*} projectId - Project ID
 */
export function processNodeSelection(element, projectId) {
    let nodeId = element.value;
    let state = element.checked;

    keepAlive();
    socket.emit(
        "updateNodeReservation",
        JSON.stringify({ projectId: projectId, nodeId: nodeId, state: state }),
        function(response) {
            if (response !== 3) {
                createAndDisplayToastMessage(TOAST_TYPES.ERROR, "Failed to update node '" + nodeId + " reservation.'\nReason: " + response);
            }
        }
    );
}

/**
 * Event processor for Node Args change modal window.
 * @param {Int} projectId - Project ID
 */
export function submitNodeParamChanges(projectId) {
    let nodeId = $("#args_edit_modal_nodeId").val();
    let data = {
        projectId: projectId,
        nodeId: nodeId,
        args: []
    };

    $('div[id$="_arg_container"]').each(function () {
        let name = $(this).find("input[id$=_arg_name]").val();
        let type = $(this).find("input[id$=_arg_type]").val();
        let val_element;
        if (type === "SCENARIO_NAME" || type === "USER_DEFINITION_NAME") {
            val_element = $(this).find("select[id$=_param_value]");
        } else if (type === "SCENARIO") {
            val_element = $(this).find("textarea[id$=_param_value]");
        } else {
            val_element = $(this).find("input[id$=_param_value]");
        }
        let value;

        switch (type) {
            case "SCENARIO":
            case "SCENARIO_NAME":
            case "USER_DEFINITION_NAME":
            case "STRING": {
                value = val_element.val(); 
                break;
            } 
            case "INT": {
                value = parseInt(val_element.val()); 
                break;
            }
            case "BOOL": {
                value = val_element[0].checked; 
                break;
            }
        }

        data.args.push({
            name: name,
            type: type,
            value: value
        });
    });

    keepAlive();
    socket.emit(
        "updateNodeParams",
        JSON.stringify(data),
        function(response) {
            if (response === 3) {
                createAndDisplayToastMessage(TOAST_TYPES.INFO, "Parameters for node '" + nodeId + "' were successfully updated");
            } else {
                createAndDisplayToastMessage(TOAST_TYPES.ERROR, "Failed to update parameters for node '" + nodeId + "'\nReason: " + response);
            }
        }
    );

    $("#node_args_edit_modal").modal("hide");
}

/**
 * Event handler for Run Summary text field - automatically updates the value for all connected clients
 * @param {Int} projectId - Project ID
 */
export function updateRunSummary(projectId) {
    let newValue = $("#runSummary").val();

    socket.emit(
        "updateRunSummary",
        JSON.stringify({ projectId: projectId, newValue: newValue })
    )
}

/**
 * Reads current list of selected nodes, and based on their list re-creates the text description for Run Stats text field
 */
function regenerateRunDescription() {
    $("#runStats").val("");
    let description = [];

    $(".node-select:checked").each(function(index) {
        let nodeId = $(this).val();
        let node = nodes.get(nodeId);

        description.push('Node - ' + nodeId + '\n');
        description.push('\n');
        description.push('Scenario data:\n');

        let nodeScenario = node.nodeParams.find((entry) => entry.type === "SCENARIO");
        if (nodeScenario !== undefined && nodeScenario.value !== "") {
            description.push('Scenario:\n');
            description.push("------------------------------------------\n")
            description.push(nodeScenario.value + "\n")
            description.push("------------------------------------------\n")
        } else {
            let nodeScenarioName = node.nodeParams.find((entry) => entry.type === "SCENARIO_NAME").value;
            let scenarioData = node.availableScenarios[nodeScenarioName];
            description.push('Scenario ID - ' + scenarioData.scenarioId + '\n');
            description.push('Scenario class - ' + scenarioData.className + '\n');
            description.push('Scenario description - ' + scenarioData.description + '\n');
            description.push('Scenario checksum - ' + scenarioData.checksum + '\n');
        }
        description.push('\n');
        
        let nodeUserDef = node.nodeParams.find((entry) => entry.type === "USER_DEFINITION_NAME").value;
        let userDefData = node.availableUserDefinitions[nodeUserDef];
        description.push('User definition data:\n');
        description.push('User definition ID - ' + userDefData.userDefId + '\n');
        description.push('User definition class - ' + userDefData.className + '\n');
        description.push('User definition description - ' + userDefData.description + '\n');
        description.push('User definition checksum - ' + userDefData.checksum + '\n');
        description.push('\n');
        description.push('Node parameters:\n');
        node.nodeParams.forEach((entry) => {
            if (entry.type !== "SCENARIO" && entry.type !== "SCENARIO_NAME" && entry.type !== "USER_DEFINITION_NAME")
                description.push(entry.name + ' - ' + entry.value + '\n');
        });
        description.push('-------------------------------------------------------\n\n');
    });

    $("#runStats").val(description.join(""));
}

/**
 * Run start button event handler - sends a request to start a test run
 * @param {Int} projectId - Project ID
 */
export function startRun(projectId) {
    let nodes = [];
    $("input[class*=node-select]:checked").each(function() { nodes.push($(this).val()); });

    let msg = {
        projectId: projectId,
        summary: $("#runSummary").val(),
        details: $("#runStats").val(),
        nodes: nodes
    };

    keepAlive();
    socket.emit(
        "startRun",
        JSON.stringify(msg),
        function(response) {
            let res = JSON.parse(response);
            if (res.status === 1) {
                createAndDisplayToastMessage(TOAST_TYPES.INFO, res.details);
            } else {
                createAndDisplayToastMessage(TOAST_TYPES.ERROR, res.details);
            }
        }
    );
}

/**
 * Run stop button event handler - sends a request to stop current test run
 * @param {Int} projectId - Project ID
 */
export function abortRun(projectId) {
    keepAlive();
    socket.emit(
        "abortRun",
        projectId.toString(),
        function(response) {
            if (response !== undefined && response !== null) {
                let res = JSON.parse(response);
                if (res.status === 1) {
                    createAndDisplayToastMessage(TOAST_TYPES.INFO, res.details);
                } else {
                    createAndDisplayToastMessage(TOAST_TYPES.ERROR, res.details);
                }
            }
        }
    );
}

export function processRunsTableHeaderCheckbox(element) {
    let checkboxes = document.getElementsByName("runId");
    if (element.checked) {
        checkboxes.forEach(checkbox => {
            checkbox.checked = true;
        });
    } else {
        checkboxes.forEach(checkbox => {
            checkbox.checked = false;
        });
    }
}

export function processRunDeletionEvent() {
    var checkedBoxes = document.querySelectorAll('input[name=runId]:checked');

    if (checkedBoxes.length === 0) {
        alert("You need to check at least 1 run checkbox before requesting deletion");
        event.preventDefault();

        return false;
    } else {
        if (confirm('Are you sure you want to delete ' + checkedBoxes.length + ' test runs?')) {
            return true;
        } else {
            event.preventDefault();
            return false;
        }
    }
}

export function verifyFileIsSelected() {
    if(document.getElementById("newRunFileInput").value === ""){
        alert("You need to specify the file first.\nInput accepts generated Allure report, packed in .zip archive");
        event.preventDefault();

        return false;
    }
    
    return true;
}

// ##########################################################################################
// Service methods
// ##########################################################################################
const TOAST_TYPES = {
    INFO: {
        label: "Information",
        color: "#007aff"
    },
    WARN: {
        label: "Warning",
        color: "#fce303"
    },
    ERROR: {
        label: "Error",
        color: "#fc3103"
    }
}

function deleteNodeRowEntry(nodeId) {
    let row = document.getElementById(nodeId + "_row");
    if (row !== null) {
        row.remove();
    }
}

/**
 * Fires a request to keep-alive end-point.
 * Called when user performs an important action not bound to server's HTTP context, that way keeping the session alive.
 * NOTE: We do not wait for response, since we're only interested in sending a request and extending token's life. No post-processing required.
 */
function keepAlive() {
    fetch("/keep-alive", { method: "GET", credentials: "include"});
}

function createAndDisplayToastMessage(toast_type, message, auto_hide=true) {
    let element = $('<div class="toast" role="alert" aria-live="assertive" aria-atomic="true"' + (auto_hide === true ? '' : 'data-bs-autohide="false"') + '></div>').append([
        '<div class="toast-header">',
        '   <svg class="bd-placeholder-img rounded me-2" width="20" height="20" ><rect width="100%" height="100%" fill="' + toast_type.color + '"></rect></svg>',
        '   <strong class="me-auto">' + toast_type.label + '</strong>',
        '   <small>' + new Date(Date.now()).toLocaleString() + '</small>',
        '   <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>',
        '</div>',
        '<div class="toast-body">',
        '   ' + message,
        '</div>'
    ].join(""));

    $("#toasts_container").append(element);
    element.toast("show");
}

// Since this file is used as module - we need to forward required functions to window context. Otherwise we won't be able to call any of them
window.projectActions = {
    connect                             : connect,
    triggerNodeCreation                 : triggerNodeCreation,
    populateNodeDeletionList            : populateNodeDeletionList,
    deleteNodes                         : deleteNodes,
    processAllNodesSelectionDeselection : processAllNodesSelectionDeselection,
    openArgsEditModal                   : openArgsEditModal,
    updateRunSummary                    : updateRunSummary,
    processNodeSelection                : processNodeSelection,
    submitNodeParamChanges              : submitNodeParamChanges,
    startRun                            : startRun,
    abortRun                            : abortRun,
    processRunsTableHeaderCheckbox      : processRunsTableHeaderCheckbox,
    processRunDeletionEvent             : processRunDeletionEvent,
    verifyFileIsSelected                : verifyFileIsSelected,
}