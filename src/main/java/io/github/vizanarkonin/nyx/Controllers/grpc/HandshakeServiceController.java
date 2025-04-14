package io.github.vizanarkonin.nyx.Controllers.grpc;

import io.github.vizanarkonin.keres.core.grpc.HandshakeRequest;
import io.github.vizanarkonin.keres.core.grpc.HandshakeSvcGrpc.HandshakeSvcImplBase;
import io.github.vizanarkonin.nyx.Handlers.NodeController;
import io.github.vizanarkonin.nyx.Repositories.ProjectNodeRepository;
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
    ProjectNodeRepository projectNodeRepository;
    
    @Override
    public void connect(HandshakeRequest request, StreamObserver<MessageResponse> responseObserver) {
        log.info(String.format("New connection request from Node '%s' (Project ID - %d)", request.getNodeId(), request.getProjectId()));

        MessageResponse.Builder res = MessageResponse.newBuilder();
        if (NodeController.nodeExists(request.getProjectId(), request.getNodeId())) {
            if (NodeController.nodeIsOnline(request.getProjectId(), request.getNodeId())) {
                log.warn(String.format("Node with ID '%s' and project ID %d is already connected. Responding with FAILURE", request.getNodeId(), request.getProjectId()));;
                res
                    .setStatus(ResponseStatus.FAILURE)
                    .setDetails(String.format("Node with ID '%s' is already connected", request.getNodeId()))
                    .setTimestamp(Instant.now().toEpochMilli());
            } else {
                log.info("Node exists and not currently online. Processing");
                res
                    .setStatus(ResponseStatus.SUCCESS)
                    .setTimestamp(Instant.now().toEpochMilli());
            }
        } else {
            log.warn(String.format("Node with ID '%s' and project ID %d is not registered. Responding with FAILURE", request.getNodeId(), request.getProjectId()));
            res
                .setStatus(ResponseStatus.FAILURE)
                .setDetails(String.format("Node with ID '%s' is not allowed", request.getNodeId()))
                .setTimestamp(Instant.now().toEpochMilli());
        }
        
        responseObserver.onNext(res.build());
        responseObserver.onCompleted();
    }
}
