package com.ecoroute.controller;

import com.ecoroute.dto.TenantRegistrationForm;
import com.ecoroute.model.Company;
import com.ecoroute.model.User;
import com.ecoroute.repository.CompanyRepository;
import com.ecoroute.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/register")
public class TenantRegistrationController {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.ecoroute.service.AuditLoggingService auditLoggingService;

    public TenantRegistrationController(CompanyRepository companyRepository,
                                        UserRepository userRepository,
                                        PasswordEncoder passwordEncoder,
                                        com.ecoroute.service.AuditLoggingService auditLoggingService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLoggingService = auditLoggingService;
    }

    @GetMapping("/tenant")
    public String showRegistrationForm(Model model) {
        model.addAttribute("form", new TenantRegistrationForm());
        return "register";
    }

    @PostMapping("/tenant")
    public String registerTenant(@Valid @ModelAttribute("form") TenantRegistrationForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        if (companyRepository.existsByName(form.getCompanyName())) {
            bindingResult.rejectValue("companyName", "error.companyName", "Company name already exists.");
        }

        if (userRepository.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue("username", "error.username", "Username is already taken.");
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            Company company = new Company();
            company.setName(form.getCompanyName());
            company.setIndustrySector(form.getIndustrySector());
            company.setOnboardingStatus(Company.OnboardingStatus.PENDING_APPROVAL);
            company = companyRepository.save(company);

            User user = new User();
            user.setUsername(form.getUsername());
            user.setEmail(form.getEmail());
            user.setPassword(passwordEncoder.encode(form.getPassword()));
            user.setRole(User.Role.LOGISTICS_MANAGER);
            user.setCompany(company);
            userRepository.save(user);

            auditLoggingService.log("TENANT_REGISTER", 
                "Tenant '" + form.getCompanyName() + "' (Sector: " + form.getIndustrySector() + ") registered initial manager '" + form.getUsername() + "'.", 
                "anonymous", form.getCompanyName());

            redirectAttributes.addFlashAttribute("success", 
                "Registration submitted successfully! Your account will be activated once approved by the administrator.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating tenant: " + e.getMessage());
            return "register";
        }
    }
}
