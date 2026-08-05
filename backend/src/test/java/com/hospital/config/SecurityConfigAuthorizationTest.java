package com.hospital.config;

import com.hospital.security.JwtAuthFilter;
import com.hospital.security.SecurityAuditFilter;
import com.hospital.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(controllers = SecurityConfigAuthorizationTest.RouteProbeController.class)
@ContextConfiguration(classes = {
        SecurityConfig.class,
        CorsConfig.class,
        SecurityConfigAuthorizationTest.RouteProbeController.class
})
@TestPropertySource(properties = "app.cors.allowed-origins=https://allowed.example")
class SecurityConfigAuthorizationTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private SecurityAuditFilter securityAuditFilter;

    @org.junit.jupiter.api.BeforeEach
    void passRequestsThroughCustomFilters() throws Exception {
        doAnswer(invocation -> {
            var request = invocation.getArgument(0, jakarta.servlet.ServletRequest.class);
            var response = invocation.getArgument(1, jakarta.servlet.ServletResponse.class);
            var chain = invocation.getArgument(2, jakarta.servlet.FilterChain.class);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            var request = invocation.getArgument(0, jakarta.servlet.ServletRequest.class);
            var response = invocation.getArgument(1, jakarta.servlet.ServletResponse.class);
            var chain = invocation.getArgument(2, jakarta.servlet.FilterChain.class);
            chain.doFilter(request, response);
            return null;
        }).when(securityAuditFilter).doFilter(any(), any(), any());
    }

    @Test
    void publicBookingAndPatientRegistrationRemainAvailable() throws Exception {
        mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousClinicalReadsAndMutationsAreRejected() throws Exception {
        mockMvc.perform(get("/appointments")).andExpect(status().isForbidden());
        mockMvc.perform(get("/patients")).andExpect(status().isForbidden());
        mockMvc.perform(patch("/appointments/00000000-0000-0000-0000-000000000001/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reports/patients")).andExpect(status().isForbidden());
    }

    @Test
    void publicDoctorAndDepartmentBrowsingRemainAvailable() throws Exception {
        mockMvc.perform(get("/doctors")).andExpect(status().isOk());
        mockMvc.perform(get("/departments")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCanReadHospitalAppointments() throws Exception {
        mockMvc.perform(get("/appointments/hospital")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotAccessAdministratorPrescriptionRoutes() throws Exception {
        mockMvc.perform(get("/prescriptions/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsAllowsOnlyConfiguredBrowserOrigin() throws Exception {
        mockMvc.perform(options("/appointments")
                        .header("Origin", "https://allowed.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://allowed.example"));

        mockMvc.perform(options("/appointments")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class RouteProbeController {
        @PostMapping({"/appointments", "/patients"})
        void createPublicResource() {}

        @GetMapping({"/appointments", "/patients", "/reports/patients", "/doctors", "/departments",
                "/appointments/hospital", "/prescriptions/{id}"})
        void readResource() {}

        @PatchMapping("/appointments/{id}/status")
        void updateAppointment() {}
    }
}
