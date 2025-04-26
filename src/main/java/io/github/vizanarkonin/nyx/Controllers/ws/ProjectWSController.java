package io.github.vizanarkonin.nyx.Controllers.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import io.github.vizanarkonin.keres.core.grpc.NodeStatus;
import io.github.vizanarkonin.nyx.Handlers.NodeController;
import io.github.vizanarkonin.nyx.Handlers.ProjectRunController;
import io.github.vizanarkonin.nyx.Handlers.WebSocketSessionController;
import io.github.vizanarkonin.nyx.Models.ProjectNode;
import io.github.vizanarkonin.nyx.Models.ProjectRun;
import io.github.vizanarkonin.nyx.Repositories.ProjectNodeRepository;
import io.github.vizanarkonin.nyx.Repositories.ProjectRunRepository;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

@Component
public class ProjectWSController {
    private final Logger log = LogManager.getLogger(this.getClass().getName());
    protected final SocketIOServer socketServer;

    @Autowired
    ProjectNodeRepository projectNodeRepository;
    @Autowired
    ProjectRunRepository projectRunRepository;
    @Autowired
    WebSocketSessionController webSocketSessionController;
    @Autowired
    private BlockingWSProjectCalls blockingWSProjectCalls;

    public ProjectWSController(SocketIOServer socketServer) {
        this.socketServer = socketServer;
        
        SocketIONamespace projectsNamespace = this.socketServer.addNamespace("/projects");
        projectsNamespace.addEventListener("getNodes",                  String.class,   getNodes);
        projectsNamespace.addEventListener("getAllRuns",                String.class,   getAllRuns);
        projectsNamespace.addEventListener("joinRoom",                  String.class,   joinRoom);
        projectsNamespace.addEventListener("createNode",                String.class,   createNode);
        projectsNamespace.addEventListener("removeNodes",               String.class,   removeNodes);
        projectsNamespace.addEventListener("updateNodeParams",          String.class,   updateNodeParams);
        projectsNamespace.addEventListener("updateNodeReservation",     String.class,   updateNodeReservation);
        projectsNamespace.addEventListener("updateRunSummary",          String.class,   updateRunSummary);
        projectsNamespace.addEventListener("startRun",                  String.class,   startRun);
        projectsNamespace.addEventListener("abortRun",                  String.class,   abortRun);
    }

    // ######################################################################################################################
    // External accessors
    // ######################################################################################################################

    /**
     * Broadcasts a nodeUpdate event to everyone in node's project room. Used to update existing node entry without
     * fetching entire nodes list and re-drawing the entire table.
     * @param node - ProjectNode instance to update
     */
    public void updateNode(ProjectNode node) {
        try {
            socketServer
                .getRoomOperations(String.valueOf(node.getProjectId()))
                    .sendEvent("nodeUpdated", node);
        } catch (Exception e) {
            log.error(e);
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Broadcasts a nodeRemoved event to everyone in node's project room.
     * Used to remove temporary node from the list upon it's disconnection.
     * @param node - ProjectNode instance to remove
     */
    public void removeNode(ProjectNode node) {
        try {
            socketServer
                .getRoomOperations(String.valueOf(node.getProjectId()))
                    .sendEvent("nodeRemoved", node.getNodeId());
        } catch (Exception e) {
            log.error(e);
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Broadcasts a project run status change to a given room.
     * @param projectId     - Project ID. Used to aquire the room handler.
     * @param runUUID       - Run UUID.
     * @param isActive      - Run status - true for started, false for finished.
     */
    public void sendRunStatus(long projectId, String runUUID, boolean isActive) {
        try {
            if (isActive) {
                socketServer
                    .getRoomOperations(String.valueOf(projectId))
                        .sendEvent("runStarted", runUUID);
            } else {
                socketServer
                    .getRoomOperations(String.valueOf(projectId))
                        .sendEvent("runFinished", runUUID);
            }
        } catch (Exception e) {
            log.error(e);
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Broadcasts current run telemetry (response times, RPS, failures). Used to compose state charts front-end-side
     * @param projectId - Project ID. Used to aquire the room handler.
     * @param runUUID   - Run UUID.
     * @param payload   - Run data.
     */
    public void sendCurrentRunValues(long projectId, String runUUID, Object payload) {
        try {
            socketServer
                .getRoomOperations(String.valueOf(projectId))
                    .sendEvent("runResultsTick", payload);
        } catch (Exception e) {
            log.error(e);
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    // ######################################################################################################################
    // Event listeners
    // ######################################################################################################################

    private DataListener<String> joinRoom = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }
            
            client.joinRoom(request);
            log.debug("User " + client.getSessionId() + " have joined room " + request);
            log.debug("Number of clients in room " + request + ": " + socketServer.getRoomOperations(request).getClients().size());
            ackRequest.sendAckData(3);    // 3 - ACK frame
        }
    };
    
    private DataListener<String> getNodes = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }

            //webSocketSessionController.refreshSession(client);
            JSONObject jsonObject = new JSONObject(request);
            ackRequest.sendAckData(NodeController.getNodesForProject(jsonObject.getInt("projectId")));
        }
    };

    private DataListener<String> getAllRuns = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }

            //webSocketSessionController.refreshSession(client);
            JSONObject jsonObject = new JSONObject(request);
            ackRequest.sendAckData(projectRunRepository.findAllByProjectIdOrderByIdDesc(jsonObject.getInt("projectId")));
        }
    };

    private DataListener<String> createNode = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }

            //webSocketSessionController.refreshSession(client);
            JSONObject req = new JSONObject(request);
            long projectId = req.getLong("projectId");
            String nodeId = req.getString("nodeId");
            String description = req.getString("description");
            
            if (projectNodeRepository.existsByNodeIdAndProjectId(nodeId, projectId)) {
                ackRequest.sendAckData("Node with ID " + nodeId + " already exists");
                return;
            }
            ProjectNode node = NodeController.createNode(projectId, nodeId, description);

            socketServer.getRoomOperations(String.valueOf(projectId)).sendEvent("nodeCreated", node);
            ackRequest.sendAckData(3);    // 3 - ACK frame
        }
    };

    private DataListener<String> removeNodes = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }

            //webSocketSessionController.refreshSession(client);
            JSONObject req = new JSONObject(request);
            long projectId = req.getLong("projectId");
            JSONArray nodes = req.getJSONArray("nodes");
            ArrayList<String> deletedNodes = new ArrayList<>();
            ArrayList<String> unprocessedNodes = new ArrayList<>();
        
            for (int index = 0; index < nodes.length(); index++) {
                String nodeId = nodes.getString(index);

                if (NodeController.nodeExists(projectId, nodeId)) {
                    NodeController.removeNode(projectId, nodeId);
                    deletedNodes.add(nodeId);
                } else {
                    unprocessedNodes.add(nodeId);
                }
            }

            socketServer.getRoomOperations(String.valueOf(projectId)).sendEvent("nodesRemoved", deletedNodes);
            if (unprocessedNodes.size() == 0)
                ackRequest.sendAckData(3);    // 3 - ACK frame
            else
                ackRequest.sendAckData("Failed to remove nodes: " + unprocessedNodes);
        }
    };

    private DataListener<String> updateNodeParams = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }

            try {
                JSONObject data = new JSONObject(request);

                long projectId = data.getLong("projectId");
                String nodeId = data.getString("nodeId");
                String values = data.getJSONArray("args").toString();
                ProjectNode node = NodeController.getNode(projectId, nodeId);
                node.updateNodeParameters(values);

                ackRequest.sendAckData(3);    // 3 - ACK frame
            } catch (Exception e) {
                ackRequest.sendAckData(e.toString());
            }
        }
    };

    private DataListener<String> updateNodeReservation = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }

            try {
                JSONObject req = new JSONObject(request);

                long projectId = req.getLong("projectId");
                String nodeId = req.getString("nodeId");
                boolean state = req.getBoolean("state");

                NodeController.getNode(projectId, nodeId).reserveNode(state);

                ackRequest.sendAckData(3);    // 3 - ACK frame
            } catch (Exception e) {
                ackRequest.sendAckData(e.toString());
            }
        }
    };

    private DataListener<String> updateRunSummary = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }

            JSONObject req = new JSONObject(request);

            long projectId = req.getLong("projectId");
            String newValue = req.getString("newValue");
            // This is a simple broadcast event - we redirect new value to all connected clients. No acknowledgment is required
            socketServer.getRoomOperations(String.valueOf(projectId)).sendEvent("runSummaryUpdated", newValue);
        }
    };

    private DataListener<String> startRun = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }

            int status = 1; // 1 for success, 0 for failure
            String details = "";
            JSONObject req = new JSONObject(request);
            long projectId = req.getLong("projectId");
            JSONArray reqNodes = req.getJSONArray("nodes");

            if (ProjectRunController.projectHasRunActive(projectId)) {
                status = 0;
                details = "Project already have an unfinished run. Wait for it to finish or stop it in order to initiate another run";

                ackRequest.sendAckData("{\"status\": \"" + status + "\", \"details\": \"" + details + "\"}");
                return;
            }

            HashMap<String, ProjectNode> nodes = new HashMap<>();
            nodesLoop:
            for (int index = 0; index < reqNodes.length(); index++) { 
                String nodeId = reqNodes.getString(index);
                ProjectNode node = NodeController.getNode(projectId, nodeId);
                switch(node.getStatus()) {
                    case NodeStatus.DISCONNECTED: {
                        status = 0;
                        details = details + "Node " + nodeId + " is offline and cannot be used for test run.\n";
                        break nodesLoop;
                    }
                    case NodeStatus.PREPARING:
                    case NodeStatus.RUNNING: {
                        status = 0;
                        details = details + "Node " + nodeId + " is already busy and cannot be used for test run.\n";
                        break nodesLoop;
                    }
                    case NodeStatus.STOPPING:
                    case NodeStatus.STOPPED: {
                        status = 0;
                        details = details + "Node " + nodeId + " is already stopping/stopped and cannot be used for test run.\n";
                        break nodesLoop;
                    }
                    default: {
                        nodes.put(nodeId, node);
                        break;
                    }
                }
            }

            if (nodes.isEmpty()) {
                status = 0;
                details = details + "No nodes were available for execution. Aborting run";
            } else {
                String summary = req.getString("summary");
                String detes = req.getString("details");
                ProjectRun run = ProjectRunController.startNewRun(projectId, summary, detes, nodes);

                status = 1;
                details = "Run UUID - " + run.getRunUUID();
            }

            ackRequest.sendAckData("{\"status\": " + status + ", \"details\": \"" + details + "\"}");
        }
    };


    private DataListener<String> abortRun = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String request, AckRequest ackRequest) {
            if (!webSocketSessionController.isClientAuthenticated(client)) {
                client.sendEvent("disconnect", "Session expired");
                return;
            }
            
            long projectId = Long.parseLong(request);

            // Call the async method and send the response back when it's done
            blockingWSProjectCalls.abortRunAsync(projectId).thenAccept(response -> {
                ackRequest.sendAckData(response);
            });
        }
    };

    /**
     * Some calls are blocking. If they are called - they will block the client for any other actions.
     * To go around it - we move all blocking logic into a sub-class and mark them async
     */
    @Component
    public static class BlockingWSProjectCalls {

        @Async
        public CompletableFuture<String> abortRunAsync(long projectId) {
            if (!ProjectRunController.projectHasRunActive(projectId)) {
                return CompletableFuture.completedFuture(
                    "{\"status\": 0, \"details\": \"No active runs found for project " + projectId + " . Nothing to abort\"}"
                );
            } 

            String runUUID = ProjectRunController.getCurrentRun(projectId).getRunUUID();
            ProjectRunController.abortRun(projectId);

            return CompletableFuture.completedFuture(
                "{\"status\": 1, \"details\": \"Successfully stopped test run with UUID " + runUUID + "\"}"
            );
        }
    }
}
