package io.github.vizanarkonin.nyx.Controllers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.vizanarkonin.nyx.Models.Project;
import io.github.vizanarkonin.nyx.Repositories.ProjectRepository;

@Controller
@RequestMapping("/")
public class IndexController extends ControllerBase {

    @Autowired
    private ProjectRepository projectRepository;

    @GetMapping("/")
    public String index(Model model) {
        List<Project> projectsList = StreamSupport.stream(projectRepository.findAll().spliterator(), false).collect(Collectors.toList());

        model.addAttribute("projectsList", projectsList);
        
        return "index";
    }
}
