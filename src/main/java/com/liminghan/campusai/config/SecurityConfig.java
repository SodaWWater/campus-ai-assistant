package com.liminghan.campusai.config;

import com.liminghan.campusai.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequestIdFilter requestIdFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RequestIdFilter requestIdFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.requestIdFilter = requestIdFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/", "/index.html", "/static/**", "/assets/**", "/favicon.ico").permitAll()
                // Admin
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                // Teacher
                .requestMatchers("/api/teacher/**").hasAnyAuthority("TEACHER", "ADMIN")
                // Student
                .requestMatchers("/api/student/**").hasAnyAuthority("STUDENT", "ADMIN")
                // Academic: teachers and admins only
                .requestMatchers("/api/academic/**").hasAnyAuthority("TEACHER", "ADMIN")
                // System: admin only
                .requestMatchers("/api/system/**").hasAuthority("ADMIN")
                // Actuator (beyond health): admin only
                .requestMatchers("/actuator/**").hasAuthority("ADMIN")
                // KB read: all authenticated (visibility enforced in service)
                .requestMatchers(HttpMethod.GET, "/api/kb/**").authenticated()
                // KB mutation: teachers and admins
                .requestMatchers(HttpMethod.POST, "/api/kb/**").hasAnyAuthority("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/kb/**").hasAnyAuthority("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/kb/**").hasAnyAuthority("TEACHER", "ADMIN")
                // Document operations: teachers and admins
                .requestMatchers(HttpMethod.POST, "/api/document/**").hasAnyAuthority("TEACHER", "ADMIN")
                // Chat and conversations: all authenticated
                .requestMatchers("/api/chat/**").authenticated()
                .requestMatchers("/api/conversations/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(requestIdFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
