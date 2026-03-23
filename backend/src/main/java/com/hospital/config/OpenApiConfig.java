package com.hospital.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediCare+ Hospital Management API")
                        .version("1.0.0")
                        .description("""
                            REST API for the Hospital Enquiry and Appointment Management System.
                            
                            ## Features
                            - Department & Doctor directory
                            - Patient registration
                            - Appointment booking with time slot management
                            - Enquiry submission and tracking
                            - Dashboard statistics
                            """)
                        .contact(new Contact()
                                .name("Hospital IT Team")
                                .email("it@hospital.com"))
                        .license(new License().name("Private")))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("Local Development"),
                        new Server().url("https://api.yourhospital.com").description("Production")
                ));
    }
}
