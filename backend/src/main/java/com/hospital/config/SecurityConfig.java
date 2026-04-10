package com.hospital.config;

import com.hospital.security.JwtAuthFilter;
import com.hospital.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService; // ← use impl, not UserRepo
    private final JwtAuthFilter jwtAuthFilter;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/auth/**",
        "/hospitals/public",
        "/departments",
        "/departments/**",
        "/doctors",
        "/doctors/**",
        "/appointments",
        "/appointments/**",
        "/enquiries",
        "/patients",
        "/actuator/health",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
    };

    private static final String[] HOSPITAL_ADMIN_ENDPOINTS = {
        "/doctors/**",
        "/departments/**",
        "/dashboard/**",
        "/common-medicines/**",
        "/common-tests/**",
        "/prescriptions/**",
    };

    private static final String[] STAFF_ENDPOINTS = {
        "/patients/**",
        "/appointments/**",
        "/enquiries/**",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HOSPITAL_ADMIN_ENDPOINTS).hasAnyRole("HOSPITAL_ADMIN", "SUPER_ADMIN")
                .requestMatchers(STAFF_ENDPOINTS).hasAnyRole("STAFF", "RECEPTIONIST", "HOSPITAL_ADMIN", "SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // ← injected directly
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}