package com.ecoroute.controller;

import com.ecoroute.config.SecurityConfig;
import com.ecoroute.model.Company;
import com.ecoroute.model.Shipment;
import com.ecoroute.model.User;
import com.ecoroute.security.EcoUserDetails;
import com.ecoroute.service.CustomUserDetailsService;
import com.ecoroute.service.DistanceService;
import com.ecoroute.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShipmentRestController.class)
@Import(SecurityConfig.class)
class ShipmentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @MockBean
    private DistanceService distanceService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CustomUserDetailsService customUserDetailsService() {
            return new CustomUserDetailsService(null) {
                @Override
                public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) {
                    Company company = new Company();
                    company.setId(1L);
                    company.setName("EcoCorp");

                    User user = new User();
                    user.setId(username.equals("manager") ? 10L : 20L);
                    user.setUsername(username);
                    user.setPassword("password");
                    user.setRole(username.equals("manager") ? User.Role.LOGISTICS_MANAGER : User.Role.AUDITOR);
                    user.setCompany(company);

                    return new EcoUserDetails(user);
                }
            };
        }
    }

    @Test
    @WithMockUser(username = "manager", roles = "LOGISTICS_MANAGER")
    void testGetDistance_Success() throws Exception {
        when(distanceService.getDistanceBetween("Chennai", "Mysuru")).thenReturn(495.2);

        mockMvc.perform(get("/api/routes/distance")
                        .param("origin", "Chennai")
                        .param("destination", "Mysuru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm").value(495.2));
    }

    @Test
    @WithUserDetails("manager")
    void testCreateShipment_Success() throws Exception {
        Shipment mockShipment = new Shipment();
        mockShipment.setId(100L);
        mockShipment.setCo2Emissions(15.0);
        when(shipmentService.createShipment(eq("TRK-12345"), eq(5.0), eq("ROAD"), eq("Bengaluru"), eq("Mysuru"), eq(1L)))
                .thenReturn(mockShipment);

        mockMvc.perform(post("/api/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\":\"TRK-12345\",\"weightTons\":5.0,\"transportMode\":\"ROAD\",\"originCity\":\"Bengaluru\",\"destinationCity\":\"Mysuru\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("created"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.co2EmissionsKg").value(15.0));

        verify(shipmentService).createShipment("TRK-12345", 5.0, "ROAD", "Bengaluru", "Mysuru", 1L);
    }

    @Test
    @WithUserDetails("auditor")
    void testCreateShipment_AccessDeniedForAuditor() throws Exception {
        // Auditors do not have permission to write/POST shipments
        mockMvc.perform(post("/api/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\":\"TRK-12345\",\"weightTons\":5.0,\"transportMode\":\"ROAD\",\"originCity\":\"Bengaluru\",\"destinationCity\":\"Mysuru\"}"))
                .andExpect(status().isForbidden());
    }
}
