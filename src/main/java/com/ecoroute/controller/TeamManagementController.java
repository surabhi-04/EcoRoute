package com.ecoroute.controller;

import com.ecoroute.dto.TeamMemberForm;
import com.ecoroute.model.Company;
import com.ecoroute.model.User;
import com.ecoroute.repository.UserRepository;
import com.ecoroute.security.EcoUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/dashboard/team")
public class TeamManagementController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.ecoroute.service.AuditLoggingService auditLoggingService;

    public TeamManagementController(UserRepository userRepository, 
                                    PasswordEncoder passwordEncoder,
                                    com.ecoroute.service.AuditLoggingService auditLoggingService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLoggingService = auditLoggingService;
    }

    @GetMapping
    public String showTeamManagement(@AuthenticationPrincipal EcoUserDetails principal, Model model) {
        Long companyId = principal.getCompanyId();
        List<User> teamMembers = userRepository.findByCompanyId(companyId);

        model.addAttribute("activeUser", principal.getUsername());
        model.addAttribute("activeCompany", principal.getCompanyName());
        model.addAttribute("role", principal.getRole());
        model.addAttribute("teamMembers", teamMembers);
        model.addAttribute("teamForm", new TeamMemberForm());

        return "team_management";
    }

    @PostMapping
    public String addTeamMember(@AuthenticationPrincipal EcoUserDetails principal,
                                @Valid @ModelAttribute("teamForm") TeamMemberForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        if (userRepository.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue("username", "error.username", "Username '" + form.getUsername() + "' is already taken.");
        }

        if (bindingResult.hasErrors()) {
            Long companyId = principal.getCompanyId();
            model.addAttribute("activeUser", principal.getUsername());
            model.addAttribute("activeCompany", principal.getCompanyName());
            model.addAttribute("role", principal.getRole());
            model.addAttribute("teamMembers", userRepository.findByCompanyId(companyId));
            return "team_management";
        }

        try {
            User.Role selectedRole = User.Role.valueOf(form.getRoleStr().toUpperCase());
            if (selectedRole == User.Role.SYSTEM_ADMIN) {
                bindingResult.rejectValue("roleStr", "error.roleStr", "Cannot provision a System Administrator.");
                Long companyId = principal.getCompanyId();
                model.addAttribute("activeUser", principal.getUsername());
                model.addAttribute("activeCompany", principal.getCompanyName());
                model.addAttribute("role", principal.getRole());
                model.addAttribute("teamMembers", userRepository.findByCompanyId(companyId));
                return "team_management";
            }

            Company company = new Company();
            company.setId(principal.getCompanyId());
            company.setName(principal.getCompanyName());

            User newUser = new User();
            newUser.setUsername(form.getUsername());
            newUser.setEmail(form.getEmail());
            newUser.setPassword(passwordEncoder.encode(form.getPassword()));
            newUser.setRole(selectedRole);
            newUser.setCompany(company);
            
            userRepository.save(newUser);

            auditLoggingService.log("USER_PROVISION", 
                "Provisioned new team member '" + form.getUsername() + "' (Role: " + form.getRoleStr() + ").", 
                principal.getUsername(), principal.getCompanyName());

            redirectAttributes.addFlashAttribute("success", "Team member '" + form.getUsername() + "' successfully added!");
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("roleStr", "error.roleStr", "Invalid role selected.");
            Long companyId = principal.getCompanyId();
            model.addAttribute("activeUser", principal.getUsername());
            model.addAttribute("activeCompany", principal.getCompanyName());
            model.addAttribute("role", principal.getRole());
            model.addAttribute("teamMembers", userRepository.findByCompanyId(companyId));
            return "team_management";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to add team member: " + e.getMessage());
            Long companyId = principal.getCompanyId();
            model.addAttribute("activeUser", principal.getUsername());
            model.addAttribute("activeCompany", principal.getCompanyName());
            model.addAttribute("role", principal.getRole());
            model.addAttribute("teamMembers", userRepository.findByCompanyId(companyId));
            return "team_management";
        }

        return "redirect:/dashboard?tab=team";
    }
}
