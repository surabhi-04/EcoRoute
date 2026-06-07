package com.ecoroute.controller;

import com.ecoroute.model.Company;
import com.ecoroute.model.User;
import com.ecoroute.repository.CompanyRepository;
import com.ecoroute.security.EcoUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CompanyRepository companyRepository;
    private final com.ecoroute.repository.UserRepository userRepository;
    private final com.ecoroute.repository.AuditLogRepository auditLogRepository;
    private final com.ecoroute.service.AuditLoggingService auditLoggingService;

    public AdminController(CompanyRepository companyRepository,
                           com.ecoroute.repository.UserRepository userRepository,
                           com.ecoroute.repository.AuditLogRepository auditLogRepository,
                           com.ecoroute.service.AuditLoggingService auditLoggingService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLoggingService = auditLoggingService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(@AuthenticationPrincipal EcoUserDetails principal, Model model) {
        List<Company> pendingCompanies = companyRepository.findByOnboardingStatus(Company.OnboardingStatus.PENDING_APPROVAL);
        
        List<java.util.Map<String, Object>> companiesWithManagers = pendingCompanies.stream().map(company -> {
            List<User> users = userRepository.findByCompanyId(company.getId());
            String managerUsername = users.isEmpty() ? "N/A" : users.get(0).getUsername();
            String managerEmail = (users.isEmpty() || users.get(0).getEmail() == null) ? "N/A" : users.get(0).getEmail();
            
            // java.util.Map.of throws NPE if values are null, so we use a HashMap or ensure non-null
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", company.getId());
            map.put("name", company.getName());
            map.put("industrySector", company.getIndustrySector() != null ? company.getIndustrySector() : "N/A");
            map.put("createdAt", company.getCreatedAt() != null ? company.getCreatedAt().toString() : "N/A");
            map.put("managerUsername", managerUsername);
            map.put("managerEmail", managerEmail);
            return map;
        }).toList();

        long pendingCount = companyRepository.findByOnboardingStatus(Company.OnboardingStatus.PENDING_APPROVAL).size();
        long activeCount = companyRepository.findByOnboardingStatus(Company.OnboardingStatus.ACTIVE).size();
        long rejectedCount = companyRepository.findByOnboardingStatus(Company.OnboardingStatus.REJECTED).size();

        model.addAttribute("activeUser", principal.getUsername());
        model.addAttribute("activeCompany", principal.getCompanyName());
        model.addAttribute("role", principal.getRole());
        model.addAttribute("pendingCompanies", companiesWithManagers);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("auditLogs", auditLogRepository.findFirst50ByOrderByTimestampDesc());
        
        return "admin-dashboard";
    }

    @PostMapping("/companies/{id}/approve")
    public String approveCompany(@PathVariable Long id,
                                 @AuthenticationPrincipal EcoUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        Optional<Company> opt = companyRepository.findById(id);
        if (opt.isPresent()) {
            Company company = opt.get();
            company.setOnboardingStatus(Company.OnboardingStatus.ACTIVE);
            companyRepository.save(company);

            auditLoggingService.log("TENANT_APPROVE", 
                "Approved onboarding for company '" + company.getName() + "'.", 
                principal.getUsername(), "EcoRoute System");

            redirectAttributes.addFlashAttribute("success", "Company " + company.getName() + " approved successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Company not found.");
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/companies/{id}/reject")
    public String rejectCompany(@PathVariable Long id,
                                @AuthenticationPrincipal EcoUserDetails principal,
                                RedirectAttributes redirectAttributes) {
        Optional<Company> opt = companyRepository.findById(id);
        if (opt.isPresent()) {
            Company company = opt.get();
            company.setOnboardingStatus(Company.OnboardingStatus.REJECTED);
            companyRepository.save(company);

            auditLoggingService.log("TENANT_REJECT", 
                "Rejected onboarding for company '" + company.getName() + "'.", 
                principal.getUsername(), "EcoRoute System");

            redirectAttributes.addFlashAttribute("success", "Company " + company.getName() + " rejected.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Company not found.");
        }
        return "redirect:/admin/dashboard";
    }
}
