package com.ecoroute.controller;

import com.ecoroute.config.SecurityConfig;
import com.ecoroute.model.Company;
import com.ecoroute.model.User;
import com.ecoroute.repository.CompanyRepository;
import com.ecoroute.repository.UserRepository;
import com.ecoroute.security.EcoUserDetails;
import com.ecoroute.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({TenantRegistrationController.class, AdminController.class, TeamManagementController.class})
@Import(SecurityConfig.class)
class TenantOnboardingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyRepository companyRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private com.ecoroute.repository.AuditLogRepository auditLogRepository;

    @MockBean
    private com.ecoroute.service.AuditLoggingService auditLoggingService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CustomUserDetailsService customUserDetailsService() {
            CustomUserDetailsService mock = Mockito.mock(CustomUserDetailsService.class);

            Company adminCompany = new Company();
            adminCompany.setId(100L);
            adminCompany.setName("EcoRoute System");

            User adminUser = new User();
            adminUser.setId(1L);
            adminUser.setUsername("admin");
            adminUser.setPassword("password");
            adminUser.setRole(User.Role.SYSTEM_ADMIN);
            adminUser.setCompany(adminCompany);

            Company managerCompany = new Company();
            managerCompany.setId(200L);
            managerCompany.setName("Acme Corp");

            User managerUser = new User();
            managerUser.setId(2L);
            managerUser.setUsername("manager");
            managerUser.setPassword("password");
            managerUser.setRole(User.Role.LOGISTICS_MANAGER);
            managerUser.setCompany(managerCompany);

            Mockito.when(mock.loadUserByUsername("admin")).thenReturn(new EcoUserDetails(adminUser));
            Mockito.when(mock.loadUserByUsername("manager")).thenReturn(new EcoUserDetails(managerUser));

            return mock;
        }
    }

    // ── Tenant Registration Flow Tests ────────────────────────────────────────

    @Test
    void testShowRegistrationForm_Success() throws Exception {
        mockMvc.perform(get("/register/tenant"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void testRegisterTenant_Success() throws Exception {
        when(companyRepository.existsByName("New Company")).thenReturn(false);
        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(passwordEncoder.encode("P@ssword123")).thenReturn("hashed_password");

        Company savedCompany = new Company();
        savedCompany.setId(10L);
        savedCompany.setName("New Company");
        savedCompany.setOnboardingStatus(Company.OnboardingStatus.PENDING_APPROVAL);

        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

        mockMvc.perform(post("/register/tenant")
                        .with(csrf())
                        .param("companyName", "New Company")
                        .param("industrySector", "Technology")
                        .param("username", "new_user")
                        .param("email", "new_user@company.com")
                        .param("password", "P@ssword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        verify(companyRepository).save(any(Company.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterTenant_ValidationFailure_InvalidEmail() throws Exception {
        mockMvc.perform(post("/register/tenant")
                        .with(csrf())
                        .param("companyName", "New Company")
                        .param("industrySector", "Technology")
                        .param("username", "new_user")
                        .param("email", "new_user@invalid")
                        .param("password", "P@ssword123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testRegisterTenant_ValidationFailure_WeakPassword() throws Exception {
        mockMvc.perform(post("/register/tenant")
                        .with(csrf())
                        .param("companyName", "New Company")
                        .param("industrySector", "Technology")
                        .param("username", "new_user")
                        .param("email", "new_user@company.com")
                        .param("password", "weakpass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors());
    }

    // ── Admin Dashboard Tests ──────────────────────────────────────────────────

    @Test
    @WithUserDetails("admin")
    void testAdminDashboard_Success() throws Exception {
        when(companyRepository.findByOnboardingStatus(Company.OnboardingStatus.PENDING_APPROVAL))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"))
                .andExpect(model().attributeExists("pendingCompanies"));
    }

    @Test
    @WithUserDetails("manager")
    void testAdminDashboard_AccessDeniedForManager() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin")
    void testApproveCompany_Success() throws Exception {
        Company company = new Company();
        company.setId(5L);
        company.setName("Acme");
        company.setOnboardingStatus(Company.OnboardingStatus.PENDING_APPROVAL);

        when(companyRepository.findById(5L)).thenReturn(Optional.of(company));

        mockMvc.perform(post("/admin/companies/5/approve").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("success", "Company Acme approved successfully!"));

        verify(companyRepository).save(any(Company.class));
    }

    // ── Team Provisioning Tests ────────────────────────────────────────────────

    @Test
    @WithUserDetails("manager")
    void testShowTeamManagement_Success() throws Exception {
        when(userRepository.findByCompanyId(200L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/dashboard/team"))
                .andExpect(status().isOk())
                .andExpect(view().name("team_management"))
                .andExpect(model().attributeExists("teamMembers"));
    }

    @Test
    @WithUserDetails("manager")
    void testAddTeamMember_Success() throws Exception {
        when(userRepository.existsByUsername("new_staff")).thenReturn(false);
        when(passwordEncoder.encode("P@ssword123")).thenReturn("hashed_password");

        mockMvc.perform(post("/dashboard/team")
                        .with(csrf())
                        .param("username", "new_staff")
                        .param("email", "new_staff@acme.com")
                        .param("password", "P@ssword123")
                        .param("roleStr", "AUDITOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?tab=team"))
                .andExpect(flash().attribute("success", "Team member 'new_staff' successfully added!"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @WithUserDetails("manager")
    void testAddTeamMember_ValidationFailure_InvalidEmail() throws Exception {
        mockMvc.perform(post("/dashboard/team")
                        .with(csrf())
                        .param("username", "new_staff")
                        .param("email", "new_staff@invalid")
                        .param("password", "P@ssword123")
                        .param("roleStr", "AUDITOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("team_management"))
                .andExpect(model().hasErrors());
    }

    @Test
    @WithUserDetails("manager")
    void testAddTeamMember_ValidationFailure_WeakPassword() throws Exception {
        mockMvc.perform(post("/dashboard/team")
                        .with(csrf())
                        .param("username", "new_staff")
                        .param("email", "new_staff@acme.com")
                        .param("password", "weakpass")
                        .param("roleStr", "AUDITOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("team_management"))
                .andExpect(model().hasErrors());
    }
}
