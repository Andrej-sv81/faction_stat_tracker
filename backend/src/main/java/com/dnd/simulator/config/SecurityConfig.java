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
    return http.csrf(c -> c.ignoringRequestMatchers("/api/**"))
        .cors(
            c ->
                c.configurationSource(
                    r -> {
                      var x = new CorsConfiguration();
                      x.setAllowedOrigins(List.of(origins.split(",")));
                      x.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                      x.setAllowedHeaders(List.of("*"));
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
