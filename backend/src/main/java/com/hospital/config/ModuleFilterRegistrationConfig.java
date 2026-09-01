package com.hospital.config;

import com.hospital.security.ModuleAccessFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModuleFilterRegistrationConfig {
    @Bean
    public FilterRegistrationBean<ModuleAccessFilter> moduleAccessFilterRegistration(ModuleAccessFilter filter) {
        FilterRegistrationBean<ModuleAccessFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
