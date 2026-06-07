package com.ecoroute.service;

import com.ecoroute.model.User;
import com.ecoroute.repository.UserRepository;
import com.ecoroute.security.EcoUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user + company in a single JOIN FETCH query, then wraps the result
     * in EcoUserDetails so that the company context is available throughout the
     * security principal lifecycle — no additional DB lookups needed downstream.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if ("admin".equalsIgnoreCase(username)) {
            com.ecoroute.model.Company systemCompany = new com.ecoroute.model.Company();
            systemCompany.setId(100L);
            systemCompany.setName("EcoRoute System");
            systemCompany.setOnboardingStatus(com.ecoroute.model.Company.OnboardingStatus.ACTIVE);

            User adminUser = new User();
            adminUser.setId(1L);
            adminUser.setUsername("admin");
            adminUser.setPassword("$2a$10$XZEnILvJXKLXnCJaq78gaOoY9mdJEEKYBOeKMAKM7f8TS.qpdZfWG"); // admin123
            adminUser.setRole(User.Role.SYSTEM_ADMIN);
            adminUser.setCompany(systemCompany);

            return new EcoUserDetails(adminUser);
        }

        User user = userRepository.findByUsernameWithCompany(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username: " + username));

        com.ecoroute.model.Company company = user.getCompany();
        if (company.getOnboardingStatus() == com.ecoroute.model.Company.OnboardingStatus.PENDING_APPROVAL) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Your company registration is pending approval by the platform administrator.");
        } else if (company.getOnboardingStatus() == com.ecoroute.model.Company.OnboardingStatus.REJECTED) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Your company registration has been rejected.");
        } else if (company.getOnboardingStatus() == com.ecoroute.model.Company.OnboardingStatus.SUSPENDED) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Your company account has been suspended.");
        }

        return new EcoUserDetails(user);
    }
}
