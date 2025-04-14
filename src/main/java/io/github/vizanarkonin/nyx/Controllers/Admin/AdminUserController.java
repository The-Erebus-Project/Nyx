package io.github.vizanarkonin.nyx.Controllers.Admin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.vizanarkonin.nyx.Controllers.ControllerBase;
import io.github.vizanarkonin.nyx.Models.Authority;
import io.github.vizanarkonin.nyx.Models.User;
import io.github.vizanarkonin.nyx.Repositories.AuthoritiesRepository;
import io.github.vizanarkonin.nyx.Utils.StringUtils;
import io.github.vizanarkonin.nyx.Utils.UserActivities;
import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController extends ControllerBase {

    @Autowired
    private AuthoritiesRepository authoritiesRepository;

    @GetMapping("")
    public String index(Model model) {
        List<User> usersList = StreamSupport.stream(userRepository.findAll().spliterator(), false).collect(Collectors.toList());
        model.addAttribute("usersList", usersList);

        return "admin/users";
    }

    @GetMapping("create")
    public String create(Model model) {
        List<User> usersList = StreamSupport.stream(userRepository.findAll().spliterator(), false).collect(Collectors.toList());
        model.addAttribute("usersList", usersList);

        return "admin/createUser";
    }

    @PostMapping("create")
    public String createUser(Model                                                          model,
                             RedirectAttributes                                             redirectAttrs,
                             Authentication                                                 authentication,
                             @RequestParam(value = "username",  required = true)  String    username,
                             @RequestParam(value = "email",     required = true)  String    email,
                             @RequestParam(value = "firstName", required = true)  String    firstName,
                             @RequestParam(value = "lastName",  required = true)  String    lastName,
                             @RequestParam(value = "role",      required = false) String[]  roles) {
        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("failureMessage", String.format("User with username '%s' already exists", username));

            return "admin/createUser";
        }

        User user = new User();
        String password = StringUtils.generateRandomStringWithNumbers(16);
        user.setUsername(username);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);

        for (String role : roles) {
            Authority authority = new Authority();
            authority.setAuthority(role);
            authority.setUsername(username);

            authoritiesRepository.save(authority);
        }
        
        userRepository.save(user);
        redirectAttrs.addFlashAttribute("successMessage", String.format("User '%s' (ID %d) was created successfully<br>Initial password is '%s'.<br>You can change it anytime in <a href=\"/userSettings\" class=\"alert-link\">user control panel</a>", username, user.getId(), password));
        String details = String.format("Initial values:<br>Username: %s<br> Email: %s<br>First Name: %s<br>Last Name: %s<br>Roles: %s",
                                        user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(), String.join(",", roles));
        logActivity(authentication.getName(), String.format(UserActivities.CREATE_USER, user.getUsername(), user.getId()), details);

        return "redirect:/admin/users";
    }

    @GetMapping("edit/{userId}")
    public String openUserPage(@PathVariable int userId, Model model) {
        User user = userRepository.findById(userId);
        authoritiesRepository.findByUsername(user.getUsername()).forEach(role -> model.addAttribute(role.getAuthority(), true));

        model.addAttribute("user", user);

        return "admin/editUser";
    }

    @PostMapping("edit/{userId}")
    @Transactional  // Method needs to be transactional in order for deleteAllByUsername to work
    public String editUser(Model                                                                    model,
                           @PathVariable                                            int             userId,
                                                                                    Authentication  authentication,
                           @RequestParam(value = "username",     required = true)   String          username,
                           @RequestParam(value = "email",        required = true)   String          email,
                           @RequestParam(value = "firstName",    required = true)   String          firstName,
                           @RequestParam(value = "lastName",     required = true)   String          lastName,
                           @RequestParam(value = "role",         required = false)  String[]        roles) {
        User user = userRepository.findById(userId);
        // We remove and rebuild the access roles in case we're changing the username
        authoritiesRepository.deleteAllByUsername(user.getUsername());

        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        
        userRepository.save(user);
        model.addAttribute("user", user);

        List<String> requestedRoles = Arrays.asList(roles);
        requestedRoles.forEach(role -> {
            Authority authority = new Authority();
            authority.setAuthority(role);
            authority.setUsername(username);

            authoritiesRepository.save(authority);
        });
        authoritiesRepository.findByUsername(user.getUsername()).forEach(role -> model.addAttribute(role.getAuthority(), true));
        model.addAttribute("successMessage", "User was updated successfully");
        String details = String.format("User changes:<br>Username: %s<br> Email: %s<br>First Name: %s<br>Last Name: %s<br>Roles: %s",
                                        user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(), String.join(",", roles));
        logActivity(authentication.getName(), String.format(UserActivities.EDIT_USER, user.getUsername(), user.getId()), details);

        return "admin/editUser";
    }

    @PostMapping("delete/{userId}")
    @Transactional  // Method needs to be transactional in order for deleteAllByUsername to work
    public String deleteUser(Model model, RedirectAttributes redirectAttrs, Authentication authentication, @PathVariable int userId) {
        User user = userRepository.findById(userId);
        String username = user.getUsername();

        authoritiesRepository.deleteAllByUsername(user.getUsername());
        userRepository.deleteById(userId);

        redirectAttrs.addFlashAttribute("successMessage", String.format("User '%s' (ID %d) was successfully deleted", username, userId));
        logActivity(authentication.getName(), String.format(UserActivities.DELETE_USER, user.getUsername(), user.getId()), "");

        return "redirect:/admin/users";
    }

    @PostMapping("resetPassword/{userId}")
    public String resetUserPassword(Model model, RedirectAttributes redirectAttrs, Authentication authentication, @PathVariable int userId) {
        User user = userRepository.findById(userId);

        String password = StringUtils.generateRandomStringWithNumbers(16);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        userRepository.save(user);

        redirectAttrs.addFlashAttribute("user", user);
        redirectAttrs.addFlashAttribute("successMessage", String.format("Password was successfully reset<br>New password is '%s'.<br>You can change it anytime in <a href=\"/userSettings\" class=\"alert-link\">user control panel</a>", password));
        logActivity(authentication.getName(), String.format(UserActivities.RESET_PASSWORD, user.getUsername(), user.getId()), "");

        return "redirect:/admin/users/edit/" + userId;
    }
}
