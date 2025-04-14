/**
 * ProjectNode object representation. Encapsulates the logic we might expect from the entity
 */
export class ProjectNode {

    constructor({ id, projectId, nodeId, description, status = "DISCONNECTED", nodeParams = [], availableScenarios = {}, availableUserDefinitions = {} }) {
        this.id = id;
        this.projectId = projectId;
        this.nodeId = nodeId;
        this.description = description;
        this.status = status;
        this.nodeParams = nodeParams;
        this.availableScenarios = availableScenarios;
        this.availableUserDefinitions = availableUserDefinitions;
    }

    static fromObject(json) {
        return new ProjectNode(json);
    }

    /**
     * Generates a <tr> row entry for Nodes list table based on the current values
     * @returns <tr> element
     */
    toNodesListEntry() {
        let row = document.createElement("tr");
        row.id = this.nodeId + "_row";
        row.innerHTML = `
            <td><input class="node-select form-check-input mt-0" type="checkbox" onclick="projectActions.processNodeSelection(this, ${this.projectId});" value="${this.nodeId}" ${(this.status === "RESERVED" ? 'checked ' : '') + (!["IDLE", "RESERVED"].includes(this.status)  ? 'disabled ' : '')}></td>
            <td>${this.id}</td>
            <td>${this.nodeId}</td>
            <td>${this.description}</td>
            <td style="width: 420px;">${(this.nodeParams.length > 0 ? this.#generateArgsContents() : "N/A")}</td>
            <td>${this.#getStatusBadgeInnerHTML()}</td>
        `;
        /*
        row.innerHTML = [
            '<td><input class="node-select form-check-input mt-0" type="checkbox" onclick="projectActions.processNodeSelection(this, ' + this.projectId + ');" value="' + this.nodeId + '" ' + (this.status === "RESERVED" ? 'checked ' : '') + (!["IDLE", "RESERVED"].includes(this.status)  ? 'disabled ' : '') + '></td>',
            '<td>' + this.id + '</td>',
            '<td>' + this.nodeId + '</td>',
            '<td>' + this.description + '</td>',
            '<td style="width: 420px;">' + (this.nodeParams.length > 0 ? this.#generateArgsContents() : "N/A") + '</td>',
            '<td>' + this.#getStatusBadgeInnerHTML() + '</td>',
        ].join("");
        */
        return row;
    }

    /**
     * Generates "Edit Args" modal window contents based on current node state.
     * @returns HTML string with args list. It is to be put into existing modal window element as InnerHTML
     */
    toArgsEditModalWindowContent() {
        let content = [
            `<input type="text" id="args_edit_modal_nodeId" style="display: none;" value="${this.nodeId}" disabled>`
        ];
    
        this.nodeParams.forEach((param) => {
            switch (param.type) {
                case "SCENARIO_NAME":
                    content.push(`
                        <div class="param-row" id="${(param.name + '_arg_container')}">
                            <input id="${(param.name + '_arg_name')}" type="text" style="display: none;" value="${param.name}">
                            <input id="${(param.name + '_arg_type')}" type="text" style="display: none;" value="${param.type}">
                            <label class="param-label">${param.name}</label>
                            <div class="param-control">
                                <select class="form-select form-select-sm" id="${(param.name + '_param_value')}" aria-label="Select test scenario to run">
                        `);
                    /*
                    content.push('  <div id="' + (param.name + '_arg_container') + '" class="form-floating mb-3" style="display: flex;">');
                    content.push('      <input id="' + (param.name + '_arg_name') + '" type="text" style="display: none;" value="' + param.name + '">');
                    content.push('      <input id="' + (param.name + '_arg_type') + '" type="text" style="display: none;" value="' + param.type + '">');
                    content.push('      <span class="input-group-text" style="width: 150px">' + param.name + '</span>');
                    content.push('      <select class="form-select" id="' + (param.name + '_param_value') + '" aria-label="Select test scenario to run">');
                    */

                    for (const key of Object.keys(this.availableScenarios)) {
                        content.push(`<option ${(param.value === key ? 'selected' : '')} value="${key}">${key}</option>`);
                    };

                    content.push(`
                                </select>
                            </div>
                        </div>
                    `);
                    /*
                    content.push('      </select>');
                    content.push('  </div>');
                    */
                    break;
                case "USER_DEFINITION_NAME":
                    content.push(`
                        <div class="param-row" id="${(param.name + '_arg_container')}">
                            <input id="${(param.name + '_arg_name')}" type="text" style="display: none;" value="${param.name}">
                            <input id="${(param.name + '_arg_type')}" type="text" style="display: none;" value="${param.type}">
                            <label class="param-label">${param.name}</label>
                            <div class="param-control">
                                <select class="form-select" id="${(param.name + '_param_value')}" aria-label="Select user definition">
                        `);
                    /*
                    content.push('  <div id="' + (param.name + '_arg_container') + '" class="form-floating mb-3" style="display: flex;">');
                    content.push('      <input id="' + (param.name + '_arg_name') + '" type="text" style="display: none;" value="' + param.name + '">');
                    content.push('      <input id="' + (param.name + '_arg_type') + '" type="text" style="display: none;" value="' + param.type + '">');
                    content.push('      <span class="input-group-text" style="width: 150px">' + param.name + '</span>');
                    content.push('      <select class="form-select" id="' + (param.name + '_param_value') + '" aria-label="Select user definition">');
                    */

                    for (const key of Object.keys(this.availableUserDefinitions)) {
                        content.push(`<option ${(param.value === key ? 'selected' : '')} value="${key}">${key}</option>`);
                    };
    
                    content.push(`
                                </select>
                            </div>
                        </div>
                    `);
                    /*
                    content.push('      </select>');
                    content.push('  </div>');
                    */
                    break;
                case "SCENARIO":
                    content.push(`
                        <div class="param-row" id="${(param.name + '_arg_container')}">
                            <input id="${(param.name + '_arg_name')}" type="text" style="display: none;" value="${param.name}">
                            <input id="${(param.name + '_arg_type')}" type="text" style="display: none;" value="${param.type}">
                            <label class="param-label">${param.name}
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-info-circle" viewBox="0 0 16 16" data-toggle="tooltip" data-placement="top" title="This scenario will override selected scenario class and be used instead of it. Leave it blank if you would like to use scenario class instead. Click this icon to open the Scenario Builder documentation" onclick="window.open(\'/ScenarioBuilderReadme.html\', \'_blank\');">
                                    <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"></path>
                                    <path d="m8.93 6.588-2.29.287-.082.38.45.083c.294.07.352.176.288.469l-.738 3.468c-.194.897.105 1.319.808 1.319.545 0 1.178-.252 1.465-.598l.088-.416c-.2.176-.492.246-.686.246-.275 0-.375-.193-.304-.533zM9 4.5a1 1 0 1 1-2 0 1 1 0 0 1 2 0"></path>
                                </svg>
                            </label>
                            <div class="param-control">
                                <textarea id="${(param.name + '_param_value')}" class="form-control form-control-sm">${param.value}</textarea>
                            </div>
                        </div>
                        `);
                    /*
                    content.push('  <div id="' + (param.name + '_arg_container') + '" class="input-group-sm mb-3" style="display: flex;">');
                    content.push('      <input id="' + (param.name + '_arg_name') + '" type="text" style="display: none;" value="' + param.name + '">');
                    content.push('      <input id="' + (param.name + '_arg_type') + '" type="text" style="display: none;" value="' + param.type + '">');
                    content.push('      <span class="input-group-text" style="width: 150px">' + param.name + ' ');
                    content.push('          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-info-circle" viewBox="0 0 16 16" data-toggle="tooltip" data-placement="top" title="This scenario will override selected scenario class and be used instead of it. Leave it blank if you would like to use scenario class instead. Click this icon to open the Scenario Builder documentation" onclick="window.open(\'/ScenarioBuilderReadme.html\', \'_blank\');">');
                    content.push('              <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"></path>');
                    content.push('              <path d="m8.93 6.588-2.29.287-.082.38.45.083c.294.07.352.176.288.469l-.738 3.468c-.194.897.105 1.319.808 1.319.545 0 1.178-.252 1.465-.598l.088-.416c-.2.176-.492.246-.686.246-.275 0-.375-.193-.304-.533zM9 4.5a1 1 0 1 1-2 0 1 1 0 0 1 2 0"></path>');
                    content.push('          </svg>');
                    content.push('      </span>');
                    content.push('      <textarea id="' + (param.name + '_param_value') + '" class="form-control" rows="3">' + param.value + '</textarea>');
                    content.push('  </div>')
                    */
                    break;
                case "STRING":
                case "INT": {
                    content.push(`
                        <div class="param-row" id="${(param.name + '_arg_container')}">
                            <input id="${(param.name + '_arg_name')}" type="text" style="display: none;" value="${param.name}">
                            <input id="${(param.name + '_arg_type')}" type="text" style="display: none;" value="${param.type}">
                            <label class="param-label">${param.name}</label>
                            <div class="param-control">
                                <input id="${(param.name + '_param_value')}" type="text" class="form-control form-control-sm" value="${param.value}">
                            </div>
                        </div>
                        `);
                    /*
                    content.push('  <div id="' + (param.name + '_arg_container') + '" class="input-group-sm mb-3" style="display: flex;">');
                    content.push('      <input id="' + (param.name + '_arg_name') + '" type="text" style="display: none;" value="' + param.name + '">');
                    content.push('      <input id="' + (param.name + '_arg_type') + '" type="text" style="display: none;" value="' + param.type + '">');
                    content.push('      <span class="input-group-text" style="width: 150px">' + param.name + '</span>');
                    content.push('      <input id="' + (param.name + '_param_value') + '" type="text" class="form-control" value="' + param.value + '">');
                    content.push('  </div>')
                    */
                    break;
                }
                case "BOOL": {
                    content.push(`
                        <div class="param-row" id="${(param.name + '_arg_container')}">
                            <input id="${(param.name + '_arg_name')}" type="text" style="display: none;" value="${param.name}">
                            <input id="${(param.name + '_arg_type')}" type="text" style="display: none;" value="${param.type}">
                            <label class="param-label">${param.name}</label>
                            <div class="param-control">
                                <div class="form-check form-switch">
                                    <input id="${(param.name + '_param_value')}" class="form-check-input" type="checkbox" ${(param.value === true ? 'checked ' : '')}>
                                </div>
                            </div>
                        </div>
                        `);
                    /*
                    content.push('  <div id="' + (param.name + '_arg_container') + '" class="input-group-sm mb-3" style="display: flex;">');
                    content.push('      <input id="' + (param.name + '_arg_name') + '" type="text" style="display: none;" value="' + param.name + '">');
                    content.push('      <input id="' + (param.name + '_arg_type') + '" type="text" style="display: none;" value="' + param.type + '">');
                    content.push('      <span class="input-group-text" style="width: 150px">' + param.name + '</span>');
                    content.push('      <div class="input-group-text" style="display: flex; justify-content: center;">');
                    content.push('          <div class="form-check form-switch" style="display: flex; justify-content: center; align-items: center;">');
                    content.push('              <input id="' + (param.name + '_param_value') + '" class="form-check-input mt-0" type="checkbox" ' + (param.value === true ? 'checked ' : '') + '>');
                    content.push('          </div>');
                    content.push('      </div>');
                    content.push('  </div>');
                    */
                    break;
                }
            }
        });
        
        return content.join("");
    }

    /**
     * Generates the InnerHTML content for node's Args column.
     * NOTE: Payload for Edit modal window is generated by generate_args_edit_window_contents function
     * @returns - String payload for column's InnerHTML
     */
    #generateArgsContents() {
        return `
            <div>
                <button type="button" class="btn btn-info" onclick="projectActions.openArgsEditModal(\'${this.nodeId}\')">View and Edit</button>
            </div>
        `;
        /*
        return [
            '<div>',
            '  <button type="button" class="btn btn-info" onclick="projectActions.openArgsEditModal(\'' + this.nodeId +  '\')">View and Edit</button>',
            '</div>'
        ].join("");
        */
    }

    /**
     * Generates a status badge element, based on status value.
     * @returns - DOM element string, to be inserted as InnerHTML
     */
    #getStatusBadgeInnerHTML() {
        switch (this.status) {
            case "DISCONNECTED":
                return '<span class="badge rounded-pill bg-danger">Disconnected</span>';
            case "IDLE":
                return '<span class="badge rounded-pill bg-success">Idle</span>';
            case "PREPARING":
                return '<span class="badge rounded-pill bg-warning">Preparing</span>';
            case "RUNNING":
                return '<span class="badge rounded-pill bg-info">Running</span>';
            case "STOPPING":
                return '<span class="badge rounded-pill bg-warning">Running</span>';
            case "FINISHED":
                return '<span class="badge rounded-pill bg-success">Finished</span>';
            case "RESERVED":
                return '<span class="badge rounded-pill bg-warning">Reserved</span>';
            default:
                return '<span class="badge rounded-pill bg-dark">UNKNOWN</span>';
        }
    }
}