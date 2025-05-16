package com.example.Event_Management.Controllers;

import com.example.Event_Management.DTO.PasswordForm;
import com.example.Event_Management.DTO.UserForm;
import com.example.Event_Management.entities.invitation.InvitationForm;
import com.example.Event_Management.entities.invitation.InvitationSession;
import com.example.Event_Management.security.TokenDecoder.JwtTokenDecoder;
import com.example.Event_Management.security.entities.AppUser;
import com.example.Event_Management.security.services.AccountService;
import com.example.Event_Management.security.services.invitations.InvitationFormService;
import com.example.Event_Management.security.services.invitations.InvitationSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.Event_Management.security.Controller.AccountRestController.addAuthButtonAttributes;

@Controller(value = "customProfileController") // Explicitly name the bean to avoid conflict
public class ProfileController {
    private AccountService accountService;
    private InvitationFormService invitationFormService;
    private JwtTokenDecoder jwtTokenDecoder;
    private PasswordEncoder passwordEncoder;
    private InvitationSessionService invitationSessionService;

    public ProfileController(AccountService accountService, InvitationFormService invitationFormService, JwtTokenDecoder jwtTokenDecoder, PasswordEncoder passwordEncoder, InvitationSessionService invitationSessionService) {
        this.accountService = accountService;
        this.invitationFormService = invitationFormService;
        this.jwtTokenDecoder = jwtTokenDecoder;
        this.passwordEncoder = passwordEncoder;
        this.invitationSessionService = invitationSessionService;
    }

    @GetMapping("/registrations")
    public String showRegistrations(Model model, HttpServletRequest request) {
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

//        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = accountService.loadUserByUsername((String) userInfo.get("username"));
        List<InvitationForm> forms = invitationFormService.findByEmail(user.getEmail());
        List<InvitationSession> sessions = forms.stream()
                .flatMap(form -> form.getInvitationSessions().stream())
                .collect(Collectors.toList());
        model.addAttribute("user", user);
        model.addAttribute("sessions", sessions);
        return "profile/myRegistrations";
    }

    @GetMapping("/infos")
    public String showInfos(Model model,HttpServletRequest request) {
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        AppUser user = accountService.loadUserByUsername((String) userInfo.get("username"));
        model.addAttribute("user", user);
        model.addAttribute("userForm", new UserForm(user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getNumberPhone()));
        return "profile/myInfos";
    }

    @PostMapping("/infos")
    public String updateInfos(@ModelAttribute("userForm") @Valid UserForm userForm, BindingResult result, Model model,HttpServletRequest request) {
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        AppUser user = accountService.loadUserByUsername((String) userInfo.get("username"));
        if (result.hasErrors()) {
            model.addAttribute("error", "Please correct the errors in the form.");
            model.addAttribute("user", user);
            return "profile/myInfos";
        }
        if (!user.getUsername().equals(userForm.getUsername()) && accountService.loadUserByUsername(userForm.getUsername()) != null) {
            result.rejectValue("username", "error.username", "Username already exists.");
            model.addAttribute("error", "Username already exists.");
            model.addAttribute("user", user);
            return "profile/myInfos";
        }
        if (!user.getEmail().equals(userForm.getEmail()) && accountService.loadUserByUsername(userForm.getEmail()) != null) {
            result.rejectValue("email", "error.email", "Email already exists.");
            model.addAttribute("error", "Email already exists.");
            model.addAttribute("user", user);
            return "profile/myInfos";
        }
        //first and last name
        if (!user.getFirstName().equals(userForm.getFirstName()) && accountService.loadUserByUsername(userForm.getFirstName()) != null) {
            result.rejectValue("firstName", "error.firstName", "First name already exists.");
            model.addAttribute("error", "First name already exists.");
            model.addAttribute("user", user);
            return "profile/myInfos";
        }
        if (!user.getLastName().equals(userForm.getLastName()) && accountService.loadUserByUsername(userForm.getLastName()) != null) {
            result.rejectValue("lastName", "error.lastName", "Last name already exists.");
            model.addAttribute("error", "Last name already exists.");
            model.addAttribute("user", user);
            return "profile/myInfos";
        }
        if (!user.getNumberPhone().equals(userForm.getNumberPhone()) && accountService.loadUserByUsername(userForm.getNumberPhone()) != null) {
            result.rejectValue("numberPhone", "error.numberPhone", "Phone number already exists.");
            model.addAttribute("error", "Phone number already exists.");
            model.addAttribute("user", user);
            return "profile/myInfos";
        }

        AppUser newUser= accountService.loadUserByUsername(userForm.getUsername());
        user.setUsername(userForm.getUsername());
        user.setEmail(userForm.getEmail());
        user.setFirstName(userForm.getFirstName());
        user.setLastName(userForm.getLastName());
        user.setNumberPhone(userForm.getNumberPhone());
        user.setProfilePicture(user.getProfilePicture());
        user.setPassword(user.getPassword());

        if(newUser != null && !newUser.getUsername().equals(user.getUsername())) {
            model.addAttribute("user", user);
            model.addAttribute("userForm", new UserForm(user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getNumberPhone()));
            model.addAttribute("error", "UserName already exist.");
            return "profile/myInfos";
        }
        accountService.updateUser(user);
        model.addAttribute("success", "Profile updated successfully.");
        model.addAttribute("user", user);
        model.addAttribute("userForm", userForm);
        return "profile/myInfos";
    }

    @GetMapping("/password")
    public String showChangePassword(Model model,HttpServletRequest request) {
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        AppUser user = accountService.loadUserByUsername((String) userInfo.get("username"));
        model.addAttribute("user", user);
        model.addAttribute("passwordForm", new PasswordForm());
        return "profile/changePassword";
    }

    @PostMapping("/password")
    public String changePassword(@ModelAttribute("passwordForm") @Valid PasswordForm passwordForm, BindingResult result, Model model,HttpServletRequest request) {
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        AppUser user = accountService.loadUserByUsername((String) userInfo.get("username"));
        if (result.hasErrors() || !passwordForm.getNewPassword().equals(passwordForm.getConfirmPassword())) {
            model.addAttribute("error", "Please correct the errors or ensure passwords match.");
            model.addAttribute("user", user);
            return "profile/changePassword";
        }
//        if (passwordForm.getNewPassword().length() < 8) {
//            model.addAttribute("error", "Password must be at least 8 characters long.");
//            model.addAttribute("user", user);
//            return "profile/changePassword";
//        }



        if (passwordEncoder.matches(passwordForm.getCurrentPassword(), user.getPassword())) {
            user.setPassword(passwordEncoder.encode(passwordForm.getNewPassword()));
            // Update the password in the database
            accountService.updateUsersPassword(user);
            // Optionally, you can also re-add the user to ensure the password is updated
            //accountService.addNewUser(user);
            model.addAttribute("success", "Password changed successfully.");
        } else {
            model.addAttribute("error", "Current password is incorrect.");
        }
        model.addAttribute("user", user);
        model.addAttribute("passwordForm", new PasswordForm());
        return "profile/changePassword";
    }

    @PostMapping("/deleteRegistration")
    public String deleteRegistration(@RequestParam Long registrationId,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        AppUser user = accountService.loadUserByUsername((String) userInfo.get("username"));
        
        try {
            InvitationSession registration = invitationSessionService.findById(registrationId);
            
            if (registration == null) {
                redirectAttributes.addFlashAttribute("error", "Registration not found.");
                return "redirect:/registrations";
            }
            
            // Check if the registration belongs to the current user
            if (!registration.getInvitationForm().getEmail().equals(user.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to cancel this registration.");
                return "redirect:/registrations";
            }
            
            // Delete the registration
            invitationSessionService.deleteById(registrationId);
            redirectAttributes.addFlashAttribute("success", "Registration cancelled successfully.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while cancelling the registration.");
        }
        
        return "redirect:/registrations";
    }
}