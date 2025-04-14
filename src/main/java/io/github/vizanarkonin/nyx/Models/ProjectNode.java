package io.github.vizanarkonin.nyx.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.awaitility.Awaitility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.vizanarkonin.keres.core.grpc.ParamType;
import io.github.vizanarkonin.nyx.Controllers.ws.ProjectWSController;
import io.github.vizanarkonin.nyx.Handlers.ServiceNexus;
import io.github.vizanarkonin.keres.core.grpc.MessageResponse;
import io.github.vizanarkonin.keres.core.grpc.NodeControlCommand;
import io.github.vizanarkonin.keres.core.grpc.NodeControlRequest;
import io.github.vizanarkonin.keres.core.grpc.NodeStatus;
import io.grpc.stub.StreamObserver;

@Entity
@Table(name="project_nodes")
@Getter @Setter
@NoArgsConstructor
public class ProjectNode implements Comparable<ProjectNode> {
    // DB columns
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    private long projectId;
    @Column(unique=true)
    private String nodeId;
    private String description;
    // Service properties
    @Transient @JsonIgnore
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @Transient
    private NodeStatus status = NodeStatus.DISCONNECTED;
    // Observers
    @Transient @JsonIgnore
    private StreamObserver<MessageResponse> requestObserver;
    @Transient @JsonIgnore
    private StreamObserver<NodeControlRequest> responseObserver;
    @Transient
    private List<NodeParam> nodeParams = new ArrayList<>();
    @Transient
    private HashMap<String, AvailableScenario> availableScenarios = new HashMap<>();
    @Transient
    private HashMap<String, AvailableUserDefinition> availableUserDefinitions = new HashMap<>();
    @Transient @JsonIgnore  // Dirty hack - we use this as a marker that node parameters were received and processed by RPC handler.
    private boolean paramsUpdated = false;

    public ProjectNode(long projectId, String nodeId, String description) {
        this.projectId = projectId;
        this.nodeId = nodeId;
        this.description = description;
    }
    
    public void initNodeControlStream(StreamObserver<MessageResponse> requestObserver, StreamObserver<NodeControlRequest> responseObserver) {
        if (this.requestObserver != null && this.responseObserver != null) {
            log.warn("Attempted to re-initialize existing node control stream. Dropping incoming connection");
            requestObserver.onCompleted();
            responseObserver.onCompleted();

            return;
        }
        
        this.requestObserver = requestObserver;
        this.responseObserver = responseObserver;
        // We hard-set the status here, since we will be manually triggering nodeUpdated event once the full data fetch is done by GRPC controller.
        status = NodeStatus.IDLE;
    }

    public void fetchNodeParameters() {
        paramsUpdated = false;
        NodeControlRequest req = NodeControlRequest.newBuilder()
            .setCommand(NodeControlCommand.SEND_PARAMETERS_VALUES)
            .build();
        responseObserver.onNext(req);

        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> paramsUpdated);
    }

    public void pingNode() {
        if (status != NodeStatus.DISCONNECTED) {
            log.trace("Node " + nodeId + " is online. Sending PING request");
            NodeControlRequest request = NodeControlRequest.newBuilder()
                .setCommand(NodeControlCommand.PING)
                .build();
        
            responseObserver.onNext(request);
        } else {
            log.trace("Node " + nodeId + " is offline. Ignoring PING request");
        }
    }

    public void updateNodeParameters(String payload) {
        NodeControlRequest request = NodeControlRequest.newBuilder()
            .setCommand(NodeControlCommand.UPDATE_PARAMETERS_VALUES)
            .setParameter(payload)
            .build();
        
        paramsUpdated = false;
        responseObserver.onNext(request);
        // As a response, client will trigger sendArgumentsValues call, which will update the args and set the flag. Until it's done - we're waiting for it
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> paramsUpdated);
        
        // Once updated - we need to notify WS clients of the event
        ServiceNexus
            .getBean(ProjectWSController.class)
                .updateNode(this);
    }

    public void fetchAvailableScenarios() {
        NodeControlRequest request = NodeControlRequest.newBuilder()
            .setCommand(NodeControlCommand.SEND_SCENARIOS_LIST)
            .build();
        responseObserver.onNext(request);
    }

    public void fetchAvailableUserDefinitions() {
        NodeControlRequest request = NodeControlRequest.newBuilder()
            .setCommand(NodeControlCommand.SEND_USER_DEFS_LIST)
            .build();
        responseObserver.onNext(request);
    }

    public void reserveNode(boolean state) {
        if (state) {
            updateStatus(NodeStatus.RESERVED);
        } else {
            updateStatus(NodeStatus.IDLE);
        }
    }

    public void updateStatus(NodeStatus value) {
        status = value;

        // We notify every connected WS client of the change
        ServiceNexus
            .getBean(ProjectWSController.class)
                .updateNode(this);
    }

    public void startScenario(String runUUID) {
        if (status == NodeStatus.RUNNING) {
            log.error("Attempted to start a scenario from node that's already running. Ignoring command");
            return;
        }

        NodeControlRequest request = NodeControlRequest.newBuilder()
            .setCommand(NodeControlCommand.START_SCENARIO)
            .setParameter(runUUID)
            .build();

        responseObserver.onNext(request);        
    }

    public void stopScenario(String runUUID) {
        log.trace("stopScenario called for nodeId " + nodeId + ", runUUID - " + runUUID);
        if (status != NodeStatus.RUNNING) {
            log.error("Called Stop command on a node that's not currently running any scenarios. Ignoring command");
            return;
        }

        NodeControlRequest request = NodeControlRequest.newBuilder()
            .setCommand(NodeControlCommand.STOP_SCENARIO)
            .setParameter(runUUID)
            .build();

        responseObserver.onNext(request);
        log.trace("NodeControlRequest with STOP_SCENARIO command is sent");
    }

    public void terminateConnection() {
        if (requestObserver != null) {
            requestObserver = null;
        }

        if (responseObserver != null) {
            responseObserver = null;
        }

        nodeParams.clear();;

        updateStatus(NodeStatus.DISCONNECTED);
    }

    @Override
    public int compareTo(ProjectNode other) {
        if (id == 0) {
            return 0;
        }
        return Long.compare(id, other.id);
    }

    /**
     * A simple serializeable proxy - used instead of NodeParameter type (it's not serializeable)
     */
    @Getter @Setter
    @NoArgsConstructor
    public static class NodeParam {
        private String name;
        private ParamType type;
        private Object value;

        public NodeParam(String name, ParamType type, Object value) {
            this.name   = name;
            this.type   = type;
            this.value  = value;
        }
    }

    /**
     * A simple serializeable proxy - used instead of ScenarioData type (it's not serializeable)
     */
    @Getter @Setter
    @NoArgsConstructor
    public static class AvailableScenario {
        private String className;
        private String scenarioId;
        private String description;
        private String checksum;

        public AvailableScenario(String className, String scenarioId, String description, String checksum) {
            this.className      = className;
            this.scenarioId     = scenarioId;
            this.description    = description;
            this.checksum       = checksum;
        }
    }

    /**
     * A simple serializeable proxy - used instead of UserDefinitionData type (it's not serializeable)
     */
    @Getter @Setter
    @NoArgsConstructor
    public static class AvailableUserDefinition {
        private String className;
        private String userDefId;
        private String description;
        private String checksum;

        public AvailableUserDefinition(String className, String userDefId, String description, String checksum) {
            this.className      = className;
            this.userDefId      = userDefId;
            this.description    = description;
            this.checksum       = checksum;
        }
    }
}
