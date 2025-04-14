package io.github.vizanarkonin.nyx.Controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.github.vizanarkonin.nyx.Handlers.FileFolderHandler;
import io.github.vizanarkonin.nyx.Models.Project;
import io.github.vizanarkonin.nyx.Models.ProjectRun;
import io.github.vizanarkonin.nyx.Models.ProjectRun.RunStatus;
import io.github.vizanarkonin.nyx.Repositories.ProjectRepository;
import io.github.vizanarkonin.nyx.Repositories.ProjectRunRepository;
import io.github.vizanarkonin.nyx.Utils.StringUtils;
import io.github.vizanarkonin.nyx.Utils.UserActivities;

@Controller
@RequestMapping("/project")
public class ProjectController extends ControllerBase {
    private final Logger log = LogManager.getLogger("ProjectController");

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectRunRepository projectRunRepository;

    @GetMapping("/{projectId}")
    public String openProject(@PathVariable int projectId, Model model) {
        Project project = projectRepository.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Project with ID %d not found", projectId));
        }

        model.addAttribute("projectID", project.getId());
        model.addAttribute("projectName", project.getName());
        model.addAttribute("sessionId", getSessionId());

        return "projectPage";
    }
    @GetMapping("/{projectId}/projectRuns")
    public String openProjectRuns(@PathVariable int projectId, Model model) {
        Project project = projectRepository.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Project with ID %d not found", projectId));
        }

        ProjectRun latestProjectRun = projectRunRepository.findTopByProjectIdOrderByIdDesc(projectId);
        if (latestProjectRun != null) {
            model.addAttribute("latestProjectRunData", latestProjectRun);
            List<ProjectRun> allRuns = projectRunRepository.findAllByProjectId(projectId);
            List<ProjectRun> allRunsInverted = allRuns.stream().sorted(Comparator.comparing(ProjectRun::getId).reversed()).collect(Collectors.toList());
            // If there are more than 1 run - then we can set the changes icons in the latest run section
            if (allRunsInverted.size() > 1) {
                model.addAttribute("previousRunData", allRunsInverted.get(1));
            }
            model.addAttribute("allProjectRunsData", allRuns);
            model.addAttribute("invertedProjectRunsData", allRunsInverted);
        }

        model.addAttribute("projectID", project.getId());
        model.addAttribute("projectName", project.getName());

        return "projectRunsPage";
    }

    /**
     * Run results upload handler - receives provided ZIP file, unpack it, traverses until in finds index and app files,
     * reads overall run data, populates DB and moves the report into data folder - inside project folder
     * @param projectId - Project ID. Retrieved from path
     * @param file      - File to process
     * @param model     - Returning JSP model. In our case we simply redirect to /{projectId} GET handler to re-load the page
     * @return
     * @throws IOException 
     */
    @PostMapping(value = "/{projectId}/projectRuns", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadResults(@PathVariable int projectId, @RequestParam("file") MultipartFile file, Model model, Authentication authentication) throws IOException {
        Project project = projectRepository.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Project with ID %d not found", projectId));
        }

        // First we save the file to temp location
        String rootTempFolderName = UUID.randomUUID().toString();
        Path tempFolder = FileFolderHandler.createTempFolder(rootTempFolderName);
        try {
            // Next, we save received file into that folder
            File tempFile = FileFolderHandler.createTempFileInFolder("report.zip", tempFolder);
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                outputStream.write(file.getBytes());
            } catch (IOException e) {
                log.error(e);

                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to create temp file for results upload");
            }

            // Then we unpack the .zip archive into this temp folder
            FileFolderHandler.unzipFileInto(tempFile, tempFolder);

            // Then we travel the contents of it and locate the folder, containing the report itself
            Path reportFolder = FileFolderHandler.findReportsFolderIn(tempFolder);

            // After that we analyze the results - extract the statistics we need to fill the ProjectRun record
            File resultsFile = reportFolder.resolve("results.js").toFile();
            String resultsFileContents = new String(Files.readAllBytes(resultsFile.toPath()));
            String testId = StringUtils.getRegexGroupValue(resultsFileContents, "const test_id = '(.*?)';", 1);
            String testDescription = StringUtils.getRegexGroupValue(resultsFileContents, "const test_description = '(.*?)';", 1);
            JSONArray timestamps = new JSONArray(StringUtils.getRegexGroupValue(resultsFileContents, "const timestamps = (.*?);", 1));
            long startTime = timestamps.getLong(0);
            long finishTime = timestamps.getLong(timestamps.length() - 1);

            ProjectRun projectRun = new ProjectRun(projectId, startTime, finishTime, testId, testDescription, RunStatus.FINISHED);
            projectRunRepository.save(projectRun);

            // Once all that done - we move the report file into the data folder and clear temp folders
            Path dataFolder = FileFolderHandler.createProjectRunFolder(project.getId(), projectRun.getId());
            try {
                FileUtils.copyDirectory(reportFolder.toFile(),
                                        dataFolder.toFile());
                FileUtils.copyFileToDirectory(tempFile, dataFolder.toFile());
            } catch (IOException e) {
                log.error(e);
                throw new RuntimeException(e);
            }

            model.addAttribute("projectName", project.getName());
            logActivity(authentication.getName(), UserActivities.UPLOAD_RESULTS, "Run ID: " + projectRun.getId());
        } finally {
            try {
                FileUtils.deleteDirectory(new File(tempFolder.toUri()));
            } catch (IOException e) {
                log.error(e);
                throw new RuntimeException(e);
            }
        }
        
        return "redirect:/project/{projectId}/projectRuns";
    }
    
    @PostMapping(value = "/{projectId}/deleteRuns", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String deleteRuns(@PathVariable int projectId, @RequestParam(value = "runId", required = false)Integer[] runIDs, Authentication authentication) {
        Project project = projectRepository.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Project with ID %d not found", projectId));
        }
        
        List<Integer> runs = Arrays.asList(runIDs);
        runs.forEach(run -> {
            File runDirectory = FileFolderHandler.getProjectRunFolder(project.getId(), run);
            try {
                FileUtils.deleteDirectory(runDirectory);
                projectRunRepository.deleteById((long)run);
            } catch (IOException e) {
                log.error(String.format("Failed to delete folder '%s'\nReason: %s", runDirectory, e));
            }
        });

        logActivity(authentication.getName(), UserActivities.DELETE_RESULTS, "Run IDs: " + String.join(", ", Arrays.toString(runIDs)));
        return "redirect:/project/{projectId}/projectRuns";
    }
}
