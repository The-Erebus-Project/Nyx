package io.github.vizanarkonin.nyx.Controllers.Admin;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.vizanarkonin.nyx.Controllers.ControllerBase;
import io.github.vizanarkonin.nyx.Models.UserActivity;
import io.github.vizanarkonin.nyx.Repositories.UserActivitiesRepository;

@Controller
@RequestMapping("/admin/userActivityLog")
public class AdminUserActivityLogController extends ControllerBase {
    @Autowired
    private UserActivitiesRepository userActivitiesRepository;

    @GetMapping("")
    public String index(Model model) {
        List<UserActivity> entries = userActivitiesRepository.findTop200ByOrderByIdDesc();
        model.addAttribute("logEntries", entries);

        return "admin/userActivityLog";
    }
}
