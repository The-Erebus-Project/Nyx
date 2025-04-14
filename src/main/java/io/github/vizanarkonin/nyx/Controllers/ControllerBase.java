package io.github.vizanarkonin.nyx.Controllers;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.vizanarkonin.nyx.Models.UserActivity;
import io.github.vizanarkonin.nyx.Repositories.UserActivitiesRepository;
import io.github.vizanarkonin.nyx.Repositories.UserRepository;

/**
 * Base type for all Controller classes - provides common methods, beans and handlers
 */
public abstract class ControllerBase {
    @Autowired
    private SessionRegistry sessionRegistry;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected UserActivitiesRepository userActivitiesRepository;

    @ExceptionHandler (Exception.class)
    public String exceptionHandler(final Exception e, Model model) {
        model.addAttribute(
            "exception", 
            ExceptionUtils.getStackTrace(e)
                .trim()                 // Remove leading/trailing whitespace
                .replace("\t", "    ")  // Convert tabs to 4 spaces
                .replaceAll("^\n+", "") // Remove leading newlines);
            ); 

        return "errors/error-500";
    }

    public void logActivity(String username, String action, String details) {
        io.github.vizanarkonin.nyx.Models.User user = userRepository.findByUsername(username);
        UserActivity activity = new UserActivity(user, action, details);

        userActivitiesRepository.save(activity);
    }

    public String getSessionId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        if (principal == null) {
            return "";
        }

        List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
        if (sessions.isEmpty()) {
            return "";
        }

        return sessions.get(0).getSessionId();
    }
}
