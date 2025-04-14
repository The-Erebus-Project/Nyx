package io.github.vizanarkonin.nyx.Controllers.grpc;

import io.github.vizanarkonin.keres.core.grpc.ParametersListRequest;
import io.github.vizanarkonin.keres.core.grpc.CurrentNodeParameters;
import io.github.vizanarkonin.keres.core.grpc.ControlsSvcGrpc.ControlsSvcImplBase;
import io.github.vizanarkonin.nyx.Controllers.ws.ProjectWSController;
import io.github.vizanarkonin.nyx.Handlers.NodeController;
import io.github.vizanarkonin.nyx.Handlers.ServiceNexus;
import io.github.vizanarkonin.nyx.Handlers.ProjectRunController;
import io.github.vizanarkonin.nyx.Models.ProjectNode;
import io.github.vizanarkonin.nyx.Models.ProjectRun;
import io.github.vizanarkonin.nyx.Models.ProjectNode.NodeParam;
import io.github.vizanarkonin.keres.core.grpc.MessageResponse;
import io.github.vizanarkonin.keres.core.grpc.NodeParameter;
import io.github.vizanarkonin.keres.core.grpc.NodeControlRequest;
import io.github.vizanarkonin.keres.core.grpc.ResponseStatus;
import io.github.vizanarkonin.keres.core.grpc.ScenarioData;
import io.github.vizanarkonin.keres.core.grpc.RunLogRequest;
import io.github.vizanarkonin.keres.core.grpc.ScenariosListRequest;
import io.github.vizanarkonin.keres.core.grpc.UserDefinitionData;
import io.github.vizanarkonin.keres.core.grpc.UserDefinitionsListRequest;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

@GrpcService
public class ControlsServiceController extends ControlsSvcImplBase {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public StreamObserver<MessageResponse> streamControls(StreamObserver<NodeControlRequest> responseObserver) {
        return new StreamObserver<MessageResponse>() {
            ProjectNode node;

            @Override
            public void onNext(MessageResponse msg) {
                switch (msg.getStatus()) {
                    case ResponseStatus.ACKNOWLEDGED:
                        log.info("Received ACKNOWLEDGED from node. Registering it");
                        // We only expect this response on stream initialization.
                        // It is used to initialize the node object by sending project and node ID again.
                        JSONObject values = new JSONObject(msg.getDetails());
                        long projectId = values.getLong("projectId");
                        String nodeId = values.getString("nodeId");

                        if (NodeController.nodeIsOnline(projectId, nodeId)) {
                            log.warn(String.format("Attempted to connect Node ID '%s' (project %d), but it is already online. Dropping connection", nodeId, projectId));
                            onCompleted();
                            responseObserver.onCompleted();
                        }
                        
                        log.info("Initializing Node control stream");
                        node = NodeController.getNode(projectId, nodeId);
                        node.initNodeControlStream(this, responseObserver);
                        node.fetchAvailableScenarios();
                        node.fetchAvailableUserDefinitions();
                        node.fetchNodeParameters();

                        // Once everything is loaded - we send a nodeUpdated event to notify all WS clients that node is now online
                        ServiceNexus
                            .getBean(ProjectWSController.class)
                                .updateNode(node);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onCompleted() {
                if (node != null) {
                    node.terminateConnection();
                }
            }

            @Override
            public void onError(Throwable exception) {
                log.error(exception.getMessage());
                if(exception.getMessage().contains("CANCELLED: client cancelled")) {
                    log.warn("CANCELLED detected. Closing the stream and marking node as DISCONNECTED");
                    onCompleted();
                }
            }
        };
    }

    @Override
    public void sendParametersList(ParametersListRequest request, StreamObserver<MessageResponse> responseObserver) {
        MessageResponse res = MessageResponse
            .newBuilder()
                .setStatus(ResponseStatus.ACKNOWLEDGED)
                .setDetails("")
            .build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }

    @Override
    public void sendParameterValues(CurrentNodeParameters request, StreamObserver<MessageResponse> responseObserver) {
        long projectId = request.getProjectId();
        String nodeId = request.getNodeId();

        MessageResponse res;
        if (NodeController.nodeIsOnline(projectId, nodeId)) {
            ProjectNode node = NodeController.getNode(projectId, nodeId);
            // Following composition is a mean, dirty hack, but for the time being we really don't need no serializeable middlewares for trivial messages
            List<NodeParam> args = new ArrayList<>();
            for (NodeParameter arg : request.getParametersList()) {
                NodeParam entry = new NodeParam(
                    arg.getName(),
                    arg.getType(),
                    switch(arg.getType()) {
                        case SCENARIO               -> arg.getStr();
                        case SCENARIO_NAME          -> arg.getStr();
                        case USER_DEFINITION_NAME   -> arg.getStr();
                        case STRING                 -> arg.getStr();
                        case BOOL                   -> arg.getBool();
                        case INT                    -> arg.getInt();
                        case UNRECOGNIZED           -> throw new UnsupportedOperationException("Unimplemented case: " + arg.getType());
                        default                     -> throw new IllegalArgumentException("Unexpected value: " + arg.getType());
                    });
                args.add(entry);
            }
            node.setNodeParams(args);
            node.setParamsUpdated(true);

            res = MessageResponse
                .newBuilder()
                    .setStatus(ResponseStatus.ACKNOWLEDGED)
                    .setDetails("")
                .build();
        } else {
            res = MessageResponse
                .newBuilder()
                    .setStatus(ResponseStatus.FAILURE)
                    .setDetails("Given node is not registered or is not online")
                .build();
        }
        
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }

    @Override
    public void sendAvailableScenariosList(ScenariosListRequest request, StreamObserver<MessageResponse> responseObserver) {
        long projectId = request.getProjectId();
        String nodeId = request.getNodeId();

        MessageResponse res;
        if (NodeController.nodeIsOnline(projectId, nodeId)) {
            ProjectNode node = NodeController.getNode(projectId, nodeId);
            HashMap<String, ProjectNode.AvailableScenario> data = new HashMap<>();
            for (ScenarioData scenarioEntry : request.getScenariosList()) {
                data.put(scenarioEntry.getClassName(), new ProjectNode.AvailableScenario(scenarioEntry.getClassName(), scenarioEntry.getScenarioId(), scenarioEntry.getDescription(), scenarioEntry.getChecksum()));
            }
            node.setAvailableScenarios(data);

            res = MessageResponse
                .newBuilder()
                    .setStatus(ResponseStatus.ACKNOWLEDGED)
                    .setDetails("")
                .build();
        } else {
            res = MessageResponse
                .newBuilder()
                    .setStatus(ResponseStatus.FAILURE)
                    .setDetails("Given node is not registered or is not online")
                .build();
        }

        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }

    @Override
    public void sendAvailableUserDefinitionsList(UserDefinitionsListRequest request, StreamObserver<MessageResponse> responseObserver) {
        long projectId = request.getProjectId();
        String nodeId = request.getNodeId();

        MessageResponse res;
        if (NodeController.nodeIsOnline(projectId, nodeId)) {
            ProjectNode node = NodeController.getNode(projectId, nodeId);
            HashMap<String, ProjectNode.AvailableUserDefinition> data = new HashMap<>();
            for (UserDefinitionData userDefEntry : request.getUserDefinitionsList()) {
                data.put(userDefEntry.getClassName(), new ProjectNode.AvailableUserDefinition(userDefEntry.getClassName(), userDefEntry.getUserDefId(), userDefEntry.getDescription(), userDefEntry.getChecksum()));
            }
            node.setAvailableUserDefinitions(data);

            res = MessageResponse
                .newBuilder()
                    .setStatus(ResponseStatus.ACKNOWLEDGED)
                    .setDetails("")
                .build();
        } else {
            res = MessageResponse
                .newBuilder()
                    .setStatus(ResponseStatus.FAILURE)
                    .setDetails("Given node is not registered or is not online")
                .build();
        }

        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }

    @Override
    public void submitResults(RunLogRequest request, StreamObserver<MessageResponse> responseObserver) {
        ProjectRun run = ProjectRunController.getCurrentRun(request.getProjectId());
        if (run == null) {
            responseObserver
                .onNext(MessageResponse.newBuilder()
                    .setStatus(ResponseStatus.FAILURE)
                    .setDetails("Project " + request.getProjectId() + " have no active runs. Unable to process data submission.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        run.submitResults(request.getNodeId(), request.getLogContents());

        responseObserver
            .onNext(MessageResponse.newBuilder()
                .setStatus(ResponseStatus.ACKNOWLEDGED)
                .build());
        responseObserver.onCompleted();
    }
}
