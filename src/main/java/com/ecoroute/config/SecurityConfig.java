package com.ecoroute.config;

import com.ecoroute.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login", "/admin/login?error=true").permitAll()
                .anyRequest().hasRole("SYSTEM_ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin/dashboard", true)
                .failureUrl("/admin/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/api/v1/**")
            );
        return http.build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/", "/login", "/register/**", "/css/**", "/js/**", "/images/**").permitAll()

                // Team Management
                .requestMatchers("/dashboard/team/**").hasRole("LOGISTICS_MANAGER")

                // ── Tenant-Isolated REST API v1 ───────────────────────────────
                // POST /api/v1/shipments → LOGISTICS_MANAGER only
                .requestMatchers(HttpMethod.POST, "/api/v1/shipments").hasRole("LOGISTICS_MANAGER")
                // GET /api/v1/** → both roles
                .requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()
                // All other /api/v1/** → authenticated
                .requestMatchers("/api/v1/**").authenticated()

                // ── Legacy REST (preserved for backwards compatibility) ────────
                .requestMatchers(HttpMethod.POST, "/api/shipments").hasRole("LOGISTICS_MANAGER")
                .requestMatchers(HttpMethod.GET,  "/api/**").authenticated()
                .requestMatchers("/api/**").authenticated()

                // ── Web UI ────────────────────────────────────────────────────
                // Certificate page → AUDITOR only
                .requestMatchers("/certificate/**").hasRole("AUDITOR")
                // Shipment add form → LOGISTICS_MANAGER only
                .requestMatchers("/shipments/add").hasRole("LOGISTICS_MANAGER")
                // All other authenticated pages
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // HTTP Basic Auth enabled for REST API clients (Postman, curl, etc.)
            .httpBasic(basic -> {})
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                // Disable CSRF for REST API to allow Postman/curl testing
                .ignoringRequestMatchers("/api/**", "/api/v1/**")
            );

        return http.build();
    }
}
