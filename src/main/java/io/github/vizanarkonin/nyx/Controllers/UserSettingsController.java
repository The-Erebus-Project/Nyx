package io.github.vizanarkonin.nyx.Controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.vizanarkonin.nyx.Models.User;
import io.github.vizanarkonin.nyx.Utils.UserActivities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/userSettings")
public class UserSettingsController extends ControllerBase {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @GetMapping("")
    public String index(Model model) {      
        return "userSettings";
    }
    
    @PostMapping("")
    public String changePassword(                                                                   Model           model,
                                                                                                    Authentication  authentication,
                                 @RequestParam(value = "newPassword", required = true)              String          newPassword, 
                                 @RequestParam(value = "newPasswordConfirmation", required = true)  String          newPasswordConfirmation) {
        if (!newPassword.equals(newPasswordConfirmation)) {
            log.error("Provided passwords didn't match. Aborting");
            model.addAttribute("failureMessage", "Provided passwords didn't match. Please, try again.");
            return "userSettings";
        }

        // TODO: There is a pretty neat password check on the front-end side, which is not implemented here. Do it.
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        userRepository.save(user);

        model.addAttribute("successMessage", "Password was changed successfully");
        logActivity(authentication.getName(), String.format(UserActivities.PASSWORD_CHANGE, user.getUsername(), user.getId()), "");
        return "userSettings";
    }
}
