package io.github.vizanarkonin.nyx.Handlers;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.vizanarkonin.nyx.Models.Project;
import io.github.vizanarkonin.nyx.Models.ProjectNode;
import io.github.vizanarkonin.nyx.Models.ProjectRun;
import io.github.vizanarkonin.nyx.Models.ProjectRun.RunStatus;
import io.github.vizanarkonin.nyx.Repositories.ProjectRepository;
import io.github.vizanarkonin.nyx.Repositories.ProjectRunRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static access point, used to handle and control test runs execution.
 * It creates the context, handles the node statuses, aggregates the results 
 * and handles the file transfer to project folder and DB records
 */
@Component
public class ProjectRunController {
    private static final Logger log = LoggerFactory.getLogger("ProjectRunController");
    // Key is projectId, value is active run
    private static final ConcurrentHashMap<Long, ProjectRun> activeRuns = new ConcurrentHashMap<>();

    private static ProjectRunRepository projectrunRepository;
    private static ProjectRepository projectRepository;
    @Autowired  // Static fields can't be autowired. To get around this, we use this fustercluck of PostConstruct call.
    private ProjectRunRepository projectRunRepositoryBuffer;
    @Autowired  // Static fields can't be autowired. To get around this, we use this fustercluck of PostConstruct call.
    private ProjectRepository projectRepositoryBuffer;
    // TODO: There has to be a more elegant way to initialize it. Rework it later
    private static boolean isInitialized = false;

    @PostConstruct
    public void init() {
        if (isInitialized) {
            log.warn("Project Run Controller is already initialized. Skipping");
            return;
        }

        projectrunRepository = this.projectRunRepositoryBuffer;
        projectRepository = this.projectRepositoryBuffer;
        log.info("Initialization finished");
    }

    public static boolean projectHasRunActive(long projectId) { return activeRuns.containsKey(projectId); }

    public static ProjectRun startNewRun(long projectId, String summary, String details, HashMap<String, ProjectNode> nodes) {
        ProjectRun run = new ProjectRun();
        run.initNewRun(projectId, summary, details, nodes);
        run.start();
        activeRuns.put(projectId, run);
        projectrunRepository.save(run);

        return run;
    }

    public static ProjectRun getCurrentRun(long projectId) {
        return activeRuns.get(projectId);
    }

    public static ProjectRun getRunForTheNode(String nodeId) {
        Optional<ProjectRun> runOpt = activeRuns
            .values()
            .stream()
                .filter(run -> run.getNodes().containsKey(nodeId))
                .findFirst();

        if (runOpt.isPresent()) {
            return runOpt.get();
        } else {
            return null;
        }
    }

    public static boolean runHasAllRunnersFinished(long projectId) {
        if (activeRuns.containsKey(projectId)) {
            if (activeRuns.get(projectId).isFinished()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static synchronized void finalizeRunAndGenerateReport(RunStatus status, long projectId) {
        log.info("finalizeRunAndGenerateReport called for projectId " + projectId);
        if (activeRuns.containsKey(projectId)) {
            ProjectRun run = activeRuns.get(projectId);
            Project project = projectRepository.findById((int)run.getProjectId());
            run.finishAndGenerateReport(status, project.getId());
            projectrunRepository.save(run);

            activeRuns.remove(projectId);
        }
    }

    public static void abortRun(long projectId) {
        log.info("abortRun called for projectId " + projectId);
        if (activeRuns.containsKey(projectId)) {
            ProjectRun run = activeRuns.get(projectId);
            Project project = projectRepository.findById((int)run.getProjectId());
            run.abort();
            run.finishAndGenerateReport(RunStatus.CANCELLED, project.getId());
            projectrunRepository.save(run);

            activeRuns.remove(projectId);
        }
    }
}
