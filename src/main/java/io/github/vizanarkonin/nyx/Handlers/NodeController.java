package io.github.vizanarkonin.nyx.Handlers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.github.vizanarkonin.keres.core.utils.TimeUtils;
import io.github.vizanarkonin.nyx.Models.ProjectNode;
import io.github.vizanarkonin.nyx.Repositories.ProjectNodeRepository;
import io.github.vizanarkonin.keres.core.grpc.NodeStatus;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

/**
 * Main entry point for operating Node entities
 */
@Component
public class NodeController {
    private static final Logger log = LoggerFactory.getLogger(NodeController.class);

    // First key - Project ID, Second key - Node ID, Value - ProjectNode instance
    private static HashMap<Long, HashMap<String, ProjectNode>> nodes = new HashMap<Long, HashMap<String, ProjectNode>>();
    
    private static ProjectNodeRepository projectNodeRepository;
    @Autowired  // Static fields can't be autowired. To get around this, we use this fustercluck of PostConstruct call.
    private ProjectNodeRepository projectNodeRepositoryBuffer;
    // TODO: There has to be a more elegant way to initialize it. Rework it later
    private static boolean isInitialized = false;
    private static Thread pingThread;

    @PostConstruct
    public void init() {
        if (isInitialized) {
            log.warn("Node Controller is already initialized. Skipping");
            return;
        }

        projectNodeRepository = this.projectNodeRepositoryBuffer;

        log.info("Initializing Node Controller");
        for (ProjectNode node : projectNodeRepository.findAll()) {
            log.info(String.format("Processing node %s (project - %d)", node.getNodeId(), node.getProjectId()));
            if (!nodes.containsKey(node.getProjectId())) {
                log.info(String.format("No project map for project %d. Creating one", node.getProjectId()));
                HashMap<String, ProjectNode> projectMap = new HashMap<String, ProjectNode>();
                nodes.put(node.getProjectId(), projectMap);
            }

            log.info(String.format("Putting node %s into a nodes list", node.getNodeId()));
            nodes.get(node.getProjectId()).put(node.getNodeId(), node);
        }

        pingThread = new Thread() {
            @Override
            public void run() {
                while(true) {
                    nodes
                        .values()
                        .forEach(project -> {
                            project
                                .values()
                                .forEach(node -> {
                                    log.trace("Sending PING request to node " + node.getId());
                                    node.pingNode();
                                });
                        });
                    TimeUtils.waitFor(Duration.ofSeconds(30));
                }
            }
        };
        pingThread.setDaemon(true);
        pingThread.start();

        isInitialized = true;
        log.info("Initialization finished");
    }

    public static List<ProjectNode> getNodesForProject(long projectId) {
        if (nodes.containsKey(projectId)) {
            List<ProjectNode> val = new ArrayList<>(nodes.get(projectId).values());
            Collections.sort(val);

            return val;
        } else {
            return Collections.emptyList();
        }
    }

    public static ProjectNode getNode(long projectId, String nodeId) {
        if (nodeExists(projectId, nodeId)) {
            return nodes.get(projectId).get(nodeId);
        } else {
            throw new RuntimeException(String.format("Node with ID '%s' (project %d) doesn't exist", nodeId, projectId));
        }
    }

    public static boolean nodeExists(long projectId, String nodeId) {
        synchronized(nodes) {
            if (nodes.containsKey(projectId)) {
                return nodes.get(projectId).containsKey(nodeId);
            } else {
                return false;
            }
        }
    }

    public static boolean nodeIsOnline(long projectId, String nodeId) {
        if (nodeExists(projectId, nodeId)) {
            return nodes.get(projectId).get(nodeId).getStatus() != NodeStatus.DISCONNECTED;
        } else {
            return false;
        }
    }

    public static ProjectNode createNode(long projectId, String nodeId, String description) {
        synchronized (nodes) {
            if(!nodeExists(projectId, nodeId)) {
                ProjectNode node = new ProjectNode(projectId, nodeId, description);
                projectNodeRepository.save(node);

                node.setStatus(NodeStatus.DISCONNECTED);
                if (!nodes.containsKey(projectId)) {
                    nodes.put(projectId, new HashMap<String, ProjectNode>());
                }
                nodes.get(projectId).put(nodeId, node);
    
                return node;
            } else {
                log.info(String.format("Node with ID '%s' is already registered. Returning it", nodeId));
                return nodes.get(projectId).get(nodeId);
            }
        }
    }

    @Transactional
    public static void removeNode(long projectId, String nodeId) {
        synchronized (nodes) {
            if (nodeExists(projectId, nodeId)) {
                ProjectNode node = nodes.get(projectId).get(nodeId);
                node.terminateConnection();
                nodes.get(projectId).remove(nodeId);

                projectNodeRepository.delete(node);
            }
        }
    }
}
