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
            c ->
                c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                        "/api/auth/register", "/api/auth/login", "/api/auth/csrf"))
        .cors(
            c ->
                c.configurationSource(
                    r -> {
                      var x = new CorsConfiguration();
                      x.setAllowedOrigins(
                          java.util.Arrays.stream(origins.split(","))
                              .map(String::trim)
                              .filter(origin -> !origin.isBlank())
                              .toList());
                      x.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                      x.setAllowedHeaders(
                          List.of("Authorization", "Content-Type", "Accept", "X-XSRF-TOKEN"));
                      x.setExposedHeaders(List.of("Set-Cookie"));
                      x.setAllowCredentials(true);
                      return x;
                    }))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/auth/**", "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
