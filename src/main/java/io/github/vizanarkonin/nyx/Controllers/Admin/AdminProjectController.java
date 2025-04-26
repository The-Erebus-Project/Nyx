package io.github.vizanarkonin.nyx.Controllers.Admin;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.vizanarkonin.nyx.Controllers.ControllerBase;
import io.github.vizanarkonin.nyx.Handlers.FileFolderHandler;
import io.github.vizanarkonin.nyx.Models.Project;
import io.github.vizanarkonin.nyx.Repositories.ProjectRepository;
import io.github.vizanarkonin.nyx.Repositories.ProjectRunRepository;
import io.github.vizanarkonin.nyx.Utils.UserActivities;
import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/admin/projects")
public class AdminProjectController extends ControllerBase {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectRunRepository projectRunRepository;

    @GetMapping("")
    public String index(Model model) {
        List<Project> projectsList = StreamSupport.stream(projectRepository.findAll().spliterator(), false).collect(Collectors.toList());
        model.addAttribute("projectsList", projectsList);

        return "admin/projects";
    }

    @GetMapping("edit/{projectId}")
    public String openProjectPage(@PathVariable int projectId, Model model) {
        Project project = projectRepository.findById(projectId);
        model.addAttribute("project", project);

        return "admin/editProject";
    }

    @GetMapping("create")
    public String createIndex(Model model) {
        return "admin/createProject";
    }

    @PostMapping(value="create", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String createProject(
                                RedirectAttributes redirectAttrs, 
                                Model model, 
                                Authentication authentication,
                                @RequestParam(value = "projectName",            required = true)    String  projectName,
                                @RequestParam(value = "projectDescription",     required = false)   String  projectDescription,
                                @RequestParam(value = "isStrict",               required = false)   boolean isStrict
                            ){
        Project project = projectRepository.findByName(projectName);
        if (project != null) {
            model.addAttribute("failureMessage", String.format("Project with name '%s' already exists", project.getName()));
            return "admin/createProject";
        }

        project = new Project();
        project.setName(projectName);
        project.setDescription(projectDescription);
        project.setStrict(isStrict);
        projectRepository.save(project);

        FileFolderHandler.createProjectFolder(project.getId());

        redirectAttrs.addFlashAttribute("successMessage", String.format("Project '%s' (ID %d) was successfully created", project.getName(), project.getId()));
        String details = String.format("Initial values:<br>Name: %s<br>Description:%s", project.getName(), project.getDescription());
        logActivity(authentication.getName(), String.format(UserActivities.CREATE_PROJECT, project.getName(), project.getId()), details);

        return "redirect:/admin/projects";
    }

    @PostMapping("edit/{projectId}")
    public String editProject(Model          model,
                              Authentication authentication,
                              @PathVariable                                                     int         projectId,
                              @RequestParam(value = "projectName",          required = true)    String      projectName,
                              @RequestParam(value = "projectDescription",   required = false)   String      projectDescription,
                              @RequestParam(value = "isStrict",             required = false)   boolean     isStrict
                            ){
        Project project = projectRepository.findById(projectId);

        if (!projectName.equals(project.getName()))
            project.setName(projectName);
        if (!projectDescription.equals(project.getDescription()))
            project.setDescription(projectDescription);
        if (project.isStrict() != isStrict)
            project.setStrict(isStrict);
        
        projectRepository.save(project);
        model.addAttribute("project", project);
        model.addAttribute("successMessage", "Project was updated successfully");

        String details = String.format("Project changes:<br>Name: %s<br>Description:%s<br>Is strict: %b", project.getName(), project.getDescription(), isStrict);
        logActivity(authentication.getName(), String.format(UserActivities.EDIT_PROJECT, project.getName(), project.getId()), details);

        return "admin/editProject";
    }

    @PostMapping("delete/{projectId}")
    @Transactional  // Method needs to be transactional in order for deleteAllByProjectId to work
    public String deleteProject(RedirectAttributes redirectAttrs, Authentication authentication, @PathVariable int projectId) {
        Project project = projectRepository.findById(projectId);
        String projectName = project.getName();

        FileFolderHandler.deleteProjectFolder(projectId);
        projectRunRepository.deleteAllByProjectId(projectId);
        projectRepository.delete(project);

        redirectAttrs.addFlashAttribute("successMessage", String.format("Project '%s' (ID %d) was successfully deleted", projectName, projectId));
        logActivity(authentication.getName(), String.format(UserActivities.DELETE_PROJECT, project.getName(), project.getId()), "");

        return "redirect:/admin/projects";
    }
}
