package com.ems.academic.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // parents: only their own portal and the notices
                        .requestMatchers(HttpMethod.GET, "/api/v1/parent/**").hasRole("PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/notices").hasAnyRole("TENANT_ADMIN", "TEACHER", "PARENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/notices").hasRole("TENANT_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/notices/*").hasRole("TENANT_ADMIN")
                        // school administration
                        .requestMatchers(HttpMethod.PUT, "/api/v1/school/profile", "/api/v1/academic-years/*/current",
                                "/api/v1/students/*", "/api/v1/students/*/guardians").hasRole("TENANT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/academic-years", "/api/v1/classes",
                                "/api/v1/classes/*/sections", "/api/v1/students", "/api/v1/students/import")
                        .hasRole("TENANT_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/attendance/sections/*").hasAnyRole("TENANT_ADMIN", "TEACHER")
                        // everything else that is readable belongs to school staff only
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("TENANT_ADMIN", "TEACHER")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));

        return http.build();
    }
}