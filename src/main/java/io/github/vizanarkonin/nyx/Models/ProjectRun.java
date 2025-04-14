package io.github.vizanarkonin.nyx.Models;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.vizanarkonin.keres.core.processing.DataCollector;
import io.github.vizanarkonin.nyx.Controllers.ws.ProjectWSController;
import io.github.vizanarkonin.nyx.Handlers.FileFolderHandler;
import io.github.vizanarkonin.nyx.Handlers.ServiceNexus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.vizanarkonin.keres.core.grpc.NodeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Entity
@Table(name="project_runs")
@Getter @Setter
public class ProjectRun {
    @Transient @JsonIgnore
    private final Logger log = LogManager.getLogger(this.getClass().getName());
    private static final String TRIMMED_DATE_TIME_FORMAT = "yyyy.MM.dd : HH.mm.ss";

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    private String runUUID;
    private long projectId;
    @Getter
    private String runSummary;
    @Getter
    private String runDescription;
    private long startTime;
    private long finishTime;
    @Enumerated(EnumType.STRING)
    @Column
    private RunStatus status;

    @Transient @JsonIgnore
    private Path tempFolder;
    @Transient @JsonIgnore
    private HashMap<String, ProjectNode> nodes = new HashMap<>();
    @Transient @JsonIgnore
    private ScheduledExecutorService statusTickExecutor = Executors.newScheduledThreadPool(1);
    @Transient @JsonIgnore
    private HashMap<String, HashMap<String, Object[]>> ticksData = new HashMap<>();

    public ProjectRun() {}
    public ProjectRun(int projectId) {
        this.projectId = projectId;
    }
    public ProjectRun(int projectId, long startTime, long finishTime, String runSummary, String runDescription, RunStatus status) {
        this.projectId = projectId;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.runSummary = runSummary;
        this.runDescription = runDescription;
        this.status = status;
    }

    public void initNewRun(long projectID, String runSummary, String runDetails, HashMap<String, ProjectNode> nodes) {
        this.projectId = projectID;
        this.runSummary = runSummary;
        this.runDescription = runDetails;
        this.nodes = nodes;
        this.runUUID = UUID.randomUUID().toString();
        DataCollector.get(this.runUUID);
        this.tempFolder = FileFolderHandler.createTempFolder(this.runUUID);
    }

    public void start() {
        nodes.values().forEach(node -> node.startScenario(runUUID));
        status = RunStatus.RUNNING;
        startTime = Instant.now().toEpochMilli();

        ProjectWSController wsController = ServiceNexus.getBean(ProjectWSController.class);
        statusTickExecutor.scheduleAtFixedRate(() -> {
            if (status != RunStatus.RUNNING)
                statusTickExecutor.shutdown();

            synchronized(ticksData) {
                wsController.sendCurrentRunValues(projectId, runUUID, new JSONObject(ticksData).toString());
                ticksData.clear();
            }
            
        }, 1, 1, TimeUnit.SECONDS);
        
        wsController.sendRunStatus(projectId, runUUID, true);
    }

    public void abort() {
        log.info("abort called for runUUID " + runUUID + " , projectId - " + projectId);
        nodes.values().forEach(node -> node.stopScenario(runUUID));
        status = RunStatus.CANCELLED;
        Awaitility.await()
            .atMost(5, TimeUnit.MINUTES)
            .until(() -> isFinished());
    }

    public boolean isFinished() {
        return nodes.values().stream().allMatch(node -> node.getStatus() == NodeStatus.FINISHED || node.getStatus() == NodeStatus.DISCONNECTED);
    }

    public void submitResults(String nodeId, String logContents) {
        if (!nodes.containsKey(nodeId)) {
            log.error("Node ID " + nodeId + " is not associated with this run. Ignoring");
            return;
        }
        
        JSONObject logObject = new JSONObject(logContents);
        log.trace(logObject.toString());
        DataCollector.get(this.runUUID).processNodeResults(logObject);

        JSONObject reqLog = logObject.getJSONObject("requests_log");
        JSONArray usersLog = logObject.getJSONArray("users_timeline");

        HashMap<String, Object[]> entry = new HashMap<>();

        reqLog
            .keySet()
            .forEach(key -> {
                if (reqLog.isNull(key)) {
                    log.warn("(processNodeResults) Value with key " + key + " was null. Ignoring");
                    return;
                }
                
                JSONArray container = reqLog.getJSONArray(key);
                Long avgResponseTime = 0L, avgRPS = 0L, avgFailuresCount = 0L;
                
                //TODO: It might be neater to be done with streams. But this simple for-loop will do for now
                for (Object resultEntry : container) {
                    try {
                        if (resultEntry instanceof JSONArray) {
                            JSONArray resEntry = (JSONArray) resultEntry;
                            avgResponseTime += resEntry.getInt(2);
                            avgRPS++;
                            if (resEntry.getBoolean(3))
                                avgFailuresCount++;
                        }
                    } catch (Exception e) {
                        log.error(e);
                        log.error(ExceptionUtils.getStackTrace(e));
                    }
                }

                if (avgResponseTime > 0 && avgRPS > 0)
                    avgResponseTime = avgResponseTime / avgRPS;
                
                entry.put(key, new Object[] { avgResponseTime, avgRPS, avgFailuresCount });
            });

        if (usersLog.isEmpty()) {
            entry.put("users", new Object[] {0});
        } else {
            entry.put("users", new Object[] { usersLog.getJSONObject(0).getInt("logValue") });
        }

        synchronized(ticksData) {
            ticksData.put(nodeId, entry);
        }
    }

    public void finishAndGenerateReport(RunStatus status, long projectId) {
        this.status = status;
        nodes.values().forEach(node -> node.updateStatus(NodeStatus.IDLE));
        ServiceNexus.getBean(ProjectWSController.class).sendRunStatus(projectId, runUUID, false);
        finishTime = Instant.now().toEpochMilli();
        DataCollector.get(this.runUUID).generateReport(tempFolder, false);
        Path runFolder = FileFolderHandler.createProjectRunFolder(projectId, id);
        try {
            FileFolderHandler.zipFolder(tempFolder, Path.of(runFolder.toAbsolutePath().toFile().getAbsolutePath() + "/report.zip"));
            FileUtils.copyDirectory(
                tempFolder.toFile(),
                runFolder.toFile());
        } catch (IOException e) {
            log.error(e.toString());
            throw new RuntimeException(e);
        } finally {
            try {
                FileUtils.deleteDirectory(new File(tempFolder.toUri()));
            } catch (IOException e) {
                log.error(e.toString());
                throw new RuntimeException(e);
            }
        }
    }

    public String getFormattedStartTime() { return DateTimeFormatter.ofPattern(TRIMMED_DATE_TIME_FORMAT).withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(startTime)); }
    public String getFormattedFinishTime() { return DateTimeFormatter.ofPattern(TRIMMED_DATE_TIME_FORMAT).withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(finishTime)); }
    public String getFormattedTotalRunTime() {
        int seconds = 0; int minutes = 0; int hours = 0;
        long timeInSeconds = (finishTime - startTime) / 1000;
        while (timeInSeconds > 60) {
            minutes += 1;
            timeInSeconds -= 60;
            if (minutes == 60) {
                hours += 1;
                minutes = 0;
            }
        }
        seconds = (int)timeInSeconds;

        return String.format("%s:%s:%s",
                             hours >= 10 ? String.valueOf(hours) : "0" + String.valueOf(hours),
                             minutes >= 10 ? String.valueOf(minutes) : "0" + String.valueOf(minutes), 
                             seconds >= 10 ? String.valueOf(seconds) : "0" + String.valueOf(seconds));
    }
    public boolean tookLongerThan(ProjectRun otherRun) { return (finishTime - startTime) > (otherRun.finishTime - otherRun.startTime); }
    public String getFormattedExecutionTimeDeltaStringFrom(ProjectRun otherRun) {
        long myExecutionTime = (finishTime - startTime) / 1000;
        long otherExecutionTime = (otherRun.finishTime - otherRun.startTime) / 1000;
        long delta = myExecutionTime > otherExecutionTime ? myExecutionTime - otherExecutionTime : otherExecutionTime - myExecutionTime;

        int seconds = 0; int minutes = 0; int hours = 0;
        while (delta > 60) {
            minutes += 1;
            delta -= 60;
            if (minutes == 60) {
                hours += 1;
                minutes = 0;
            }
        }
        seconds = (int)delta;

        return String.format("%s%s:%s:%s",
                             myExecutionTime > otherExecutionTime ? "+" : "-",
                             hours >= 10 ? String.valueOf(hours) : "0" + String.valueOf(hours),
                             minutes >= 10 ? String.valueOf(minutes) : "0" + String.valueOf(minutes), 
                             seconds >= 10 ? String.valueOf(seconds) : "0" + String.valueOf(seconds));
    }

    public static enum RunStatus {
        RUNNING,
        FINISHED,
        CANCELLED
    }
}
