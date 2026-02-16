package com.fathallah.jobapplicationtracker.security.config;

import com.fathallah.jobapplicationtracker.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) {
        try {
            return cfg.getAuthenticationManager();
        }catch(Exception e){
            throw new IllegalStateException("Failed to get AuthenticationManager", e);
        }
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) {
        try {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(cors -> {})
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((req, res, e) ->
                                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                            .accessDeniedHandler((req, res, e) ->
                                    res.sendError(HttpServletResponse.SC_FORBIDDEN))
                    )
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    "/api/auth/**",
                                    "/swagger-ui.html", "/swagger-ui/**",
                                    "/v3/api-docs/**"
                            )
                            .permitAll()
                            .requestMatchers("/api/applications","/api/applications/**").hasRole("USER")
                            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .build();
        } catch (Exception e){
            throw new IllegalStateException("Failed to build SecurityFilterChain", e);
        }
    }
}
