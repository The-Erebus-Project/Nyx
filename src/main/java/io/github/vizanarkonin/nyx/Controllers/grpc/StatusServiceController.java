package io.github.vizanarkonin.nyx.Controllers.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.vizanarkonin.keres.core.grpc.ClientStatusMessage;
import io.github.vizanarkonin.keres.core.grpc.MessageResponse;
import io.github.vizanarkonin.keres.core.grpc.NodeStatus;
import io.github.vizanarkonin.keres.core.grpc.ResponseStatus;
import io.github.vizanarkonin.keres.core.grpc.StatusSvcGrpc.StatusSvcImplBase;
import io.github.vizanarkonin.nyx.Handlers.NodeController;
import io.github.vizanarkonin.nyx.Handlers.ProjectRunController;
import io.github.vizanarkonin.nyx.Models.ProjectRun.RunStatus;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class StatusServiceController extends StatusSvcImplBase {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    
    @Override
    public void sendStatusUpdate(ClientStatusMessage request, StreamObserver<MessageResponse> responseObserver) {
        long projectId = request.getProjectId();
        String nodeId = request.getNodeId();
        NodeStatus status = request.getStatus();
        log.info(String.format("Received status update from node '%s' (project %d). New value - %s", nodeId, projectId, status));

        NodeController.getNode(projectId, nodeId).updateStatus(status);
        // TODO: Handle specific cases and errors/exceptions

        MessageResponse response = MessageResponse.newBuilder()
            .setStatus(ResponseStatus.ACKNOWLEDGED)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // Leaving multiple if-else layers in case we need to incorporate negative scenarios in the future
        if (ProjectRunController.projectHasRunActive(projectId)) {
            if (ProjectRunController.runHasAllRunnersFinished(projectId)) {
                if (ProjectRunController.getCurrentRun(projectId).getStatus() != RunStatus.CANCELLED) {
                    ProjectRunController.finalizeRunAndGenerateReport(RunStatus.FINISHED, projectId);
                }
            }
        }
    }
}
