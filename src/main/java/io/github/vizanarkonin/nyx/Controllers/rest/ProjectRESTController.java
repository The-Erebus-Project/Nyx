package io.github.vizanarkonin.nyx.Controllers.rest;

import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Preconditions;

import io.github.vizanarkonin.keres.core.grpc.NodeStatus;
import io.github.vizanarkonin.nyx.Handlers.NodeController;
import io.github.vizanarkonin.nyx.Handlers.ProjectRunController;
import io.github.vizanarkonin.nyx.Models.ProjectNode;
import io.github.vizanarkonin.nyx.Models.ProjectRun;
import io.github.vizanarkonin.nyx.Repositories.ProjectNodeRepository;
import io.github.vizanarkonin.nyx.Repositories.ProjectRunRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/project")
public class ProjectRESTController {

    @Autowired
    ProjectNodeRepository projectNodeRepository;
    @Autowired
    ProjectRunRepository projectRunRepository;
    
    @GetMapping("/{projectId}/getNodes")
    public List<ProjectNode> getAllNodes(@PathVariable int projectId) {
        return NodeController.getNodesForProject(projectId);
    }

    @GetMapping("/{projectId}/getAllRuns")
    public List<ProjectRun> getAllRuns(@PathVariable int projectId) {
        return projectRunRepository.findAllByProjectIdOrderByIdDesc(projectId);
    }

    @PostMapping("/{projectId}/createNode")
    public String registerNode(@PathVariable long projectId, @RequestBody String reqBody, HttpServletResponse response) {
        Preconditions.checkNotNull(reqBody);
        JSONObject request = new JSONObject(reqBody);
        String nodeId = request.getString("nodeId");
        String description = request.getString("description");
        
        if (projectNodeRepository.existsByNodeIdAndProjectId(nodeId, projectId)) {
            response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
            return "{\"message\": \"Node with this ID already exists\"}";
        }
        NodeController.createNode(projectId, nodeId, description);

        return "{\"message\": \"Success\"}";
    }

    @PostMapping("/{projectId}/deleteNodes")
    @Transactional
    public String deleteNodes(@PathVariable int projectId, @RequestBody String reqBody, HttpServletResponse response) {
        Preconditions.checkNotNull(reqBody);
        JSONArray request = new JSONObject(reqBody).getJSONArray("nodes");
    
        for (int index = 0; index < request.length(); index++) {
            String nodeId = request.getString(index);

            if (NodeController.nodeExists(projectId, nodeId)) {
                NodeController.removeNode(projectId, nodeId);
            } else {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                return String.format("{\"message\": \"Node with ID '%s' doesn't exist in this project\"}", nodeId);
            }
        }

        return "{\"message\": \"Success\"}";
    }

    @PostMapping("/{projectId}/updateNodeParams")
    public String updateNodeParams(@PathVariable int projectId, @RequestBody String reqBody, HttpServletResponse response) {
        Preconditions.checkNotNull(reqBody);
        JSONObject data = new JSONObject(reqBody);

        String nodeId = data.getString("nodeId");
        String values = data.getJSONArray("args").toString();

        NodeController.getNode(projectId, nodeId).updateNodeParameters(values);

        return "{\"message\": \"Success\"}";
    }

    @PostMapping("/{projectId}/nodeReservation")
    public String nodeReservation(@PathVariable int projectId, @RequestBody String reqBody, HttpServletResponse response) {
        Preconditions.checkNotNull(reqBody);
        JSONObject req = new JSONObject(reqBody);

        String nodeId = req.getString("nodeId");
        boolean state = req.getBoolean("state");

        NodeController.getNode(projectId, nodeId).reserveNode(state);

        return "{\"message\": \"Success\"}";
    }

    @PostMapping("/{projectId}/startRun")
    public String startRun(@PathVariable int projectId, @RequestBody String reqBody, HttpServletResponse response) {
        String status = "", details = "";
        int code = HttpServletResponse.SC_OK;
        JSONObject req = new JSONObject(reqBody);
        JSONArray reqNodes = req.getJSONArray("nodes");

        if (ProjectRunController.projectHasRunActive(projectId)) {
            status = "Failed";
            details = "Project already have an unfinished run. Wait for it to finish or stop it in order to initiate another run";
            code = HttpServletResponse.SC_PRECONDITION_FAILED;

            response.setStatus(code);
            return "{\"status\": \"" + status + "\", \"details\": \"" + details + "\"}";
        }

        HashMap<String, ProjectNode> nodes = new HashMap<>();
        nodesLoop:
        for (int index = 0; index < reqNodes.length(); index++) { 
            String nodeId = reqNodes.getString(index);
            ProjectNode node = NodeController.getNode(projectId, nodeId);
            switch(node.getStatus()) {
                case NodeStatus.DISCONNECTED: {
                    status = "Failed";
                    details = details + "Node " + nodeId + " is offline and cannot be used for test run.\n";
                    code = HttpServletResponse.SC_PRECONDITION_FAILED;
                    break nodesLoop;
                }
                case NodeStatus.PREPARING:
                case NodeStatus.RUNNING: {
                    status = "Failed";
                    details = details + "Node " + nodeId + " is already busy and cannot be used for test run.\n";
                    code = HttpServletResponse.SC_PRECONDITION_FAILED;
                    break nodesLoop;
                }
                case NodeStatus.STOPPING:
                case NodeStatus.STOPPED: {
                    status = "Failed";
                    details = details + "Node " + nodeId + " is already stopping/stopped and cannot be used for test run.\n";
                    code = HttpServletResponse.SC_PRECONDITION_FAILED;
                    break nodesLoop;
                }
                default: {
                    nodes.put(nodeId, node);
                    break;
                }
            }
        }

        if (nodes.isEmpty()) {
            status = "Failed";
            details = details + "No nodes were available for execution. Aborting run";
            code = HttpServletResponse.SC_PRECONDITION_FAILED;
        } else {
            String summary = req.getString("summary");
            String detes = req.getString("details");
            ProjectRun run = ProjectRunController.startNewRun(projectId, summary, detes, nodes);

            status = "Success";
            details = "Run UUID - " + run.getRunUUID();
        }

        response.setStatus(code);
        return "{\"status\": \"" + status + "\", \"details\": \"" + details + "\"}";
    }

    @GetMapping("/{projectId}/abortRun")
    public String abortRun(@PathVariable int projectId, HttpServletResponse response) {
        String status = "", details = "";
        int code = HttpServletResponse.SC_OK;

        if (!ProjectRunController.projectHasRunActive(projectId)) {
            status="N/A";
            details = "No active runs found for project " + projectId + " . Nothing to abort";
        } else {
            String runUUID = ProjectRunController.getCurrentRun(projectId).getRunUUID();
            ProjectRunController.abortRun(projectId);
            status="Success";
            details = "Successfully stopped test run with UUID " + runUUID;
        }

        response.setStatus(code);
        return "{\"status\": \"" + status + "\", \"details\": \"" + details + "\"}";
    }
}
