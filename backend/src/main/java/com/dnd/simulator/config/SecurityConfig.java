package com.dnd.simulator.config;

import com.dnd.simulator.security.JwtFilter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain filterChain(
      HttpSecurity http, JwtFilter jwt, @Value("${app.cors-origins}") String origins)
      throws Exception {
    return http.csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                        "/api/auth/register", "/api/auth/login", "/api/auth/csrf"))
        .cors(
            cors ->
                cors.configurationSource(
                    request -> {
                      var corsConfiguration = new CorsConfiguration();
                      corsConfiguration.setAllowedOrigins(
                          java.util.Arrays.stream(origins.split(","))
                              .map(String::trim)
                              .filter(origin -> !origin.isBlank())
                              .toList());
                      corsConfiguration.setAllowedMethods(
                          List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                      corsConfiguration.setAllowedHeaders(
                          List.of("Authorization", "Content-Type", "Accept", "X-XSRF-TOKEN"));
                      corsConfiguration.setExposedHeaders(List.of("Set-Cookie"));
                      corsConfiguration.setAllowCredentials(true);
                      return corsConfiguration;
                    }))
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorization ->
                authorization.requestMatchers("/api/auth/**", "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
