package com.dnd.simulator.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtFilter extends OncePerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
  private final JwtService jwt;

  public JwtFilter(JwtService jwt) {
    this.jwt = jwt;
  }

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    SecurityContextHolder.clearContext();
    String header = req.getHeader("Authorization");

    if (header != null && header.startsWith("Bearer ")) {
      try {
        String userId = jwt.userId(header.substring(7).trim());
        UUID.fromString(userId);
        
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
      } catch (JwtException | IllegalArgumentException exception) {
        logger.warn("Rejected invalid JWT for request {} {}", req.getMethod(), req.getRequestURI());
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"error\":\"Invalid or expired authentication token\"}");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
