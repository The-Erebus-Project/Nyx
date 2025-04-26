package io.github.vizanarkonin.nyx.Controllers.grpc;

import io.github.vizanarkonin.keres.core.grpc.HandshakeRequest;
import io.github.vizanarkonin.keres.core.grpc.HandshakeSvcGrpc.HandshakeSvcImplBase;
import io.github.vizanarkonin.nyx.Handlers.NodeController;
import io.github.vizanarkonin.nyx.Models.Project;
import io.github.vizanarkonin.nyx.Models.ProjectNode;
import io.github.vizanarkonin.nyx.Repositories.ProjectNodeRepository;
import io.github.vizanarkonin.nyx.Repositories.ProjectRepository;
import io.github.vizanarkonin.keres.core.grpc.MessageResponse;
import io.github.vizanarkonin.keres.core.grpc.ResponseStatus;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class HandshakeServiceController extends HandshakeSvcImplBase {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    ProjectNodeRepository projectNodeRepository;
    
    @Override
    public void connect(HandshakeRequest request, StreamObserver<MessageResponse> responseObserver) {
        log.info(String.format("New connection request from Node '%s' (Project ID - %d)", request.getNodeId(), request.getProjectId()));

        Project project = projectRepository.findById((int)request.getProjectId());
        long projectId = request.getProjectId();
        String nodeId = request.getNodeId();
        MessageResponse.Builder res = MessageResponse.newBuilder();
        if (project.isStrict()) {
            log.trace("Project is running in strict mode. Processing normally");
            processStrict(projectId, nodeId, res);
        } else {
            log.trace("Project is not running in strict mode.");
            if (nodeId.equals("")) {
                log.trace("NodeId is not specified. Creating a temporary node and assigning it to a client");
                ProjectNode node = NodeController.createNode(projectId, nodeId, "Temporary node");
                res
                    .setStatus(ResponseStatus.SUCCESS)
                    .setTimestamp(Instant.now().toEpochMilli())
                    .setDetails(node.getNodeId());
            } else {
                processStrict(projectId, nodeId, res);
            }
        }
        
        responseObserver.onNext(res.build());
        responseObserver.onCompleted();
    }

    private void processStrict(long projectId, String nodeId, MessageResponse.Builder res) {
        if (NodeController.nodeExists(projectId, nodeId)) {
            if (NodeController.nodeIsOnline(projectId, nodeId)) {
                log.warn(String.format("Node with ID '%s' and project ID %d is already connected. Responding with FAILURE", nodeId, projectId));;
                res
                    .setStatus(ResponseStatus.FAILURE)
                    .setDetails(String.format("Node with ID '%s' is already connected", nodeId))
                    .setTimestamp(Instant.now().toEpochMilli());
            } else {
                log.info("Node exists and not currently online. Processing");
                res
                    .setStatus(ResponseStatus.SUCCESS)
                    .setTimestamp(Instant.now().toEpochMilli());
            }
        } else {
            log.warn(String.format("Node with ID '%s' and project ID %d is not registered. Responding with FAILURE", nodeId, projectId));
            res
                .setStatus(ResponseStatus.FAILURE)
                .setDetails(String.format("Node with ID '%s' is not allowed", nodeId))
                .setTimestamp(Instant.now().toEpochMilli());
        }
    }
}
