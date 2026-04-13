package com.baber.bookingservice.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1) // Specify the order of the filter in the filter chain
public class UserContextFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(UserContextFilter.class);
    
    @Value("${jwt.secret:U2dWa1lwMzczNjc5NzkyRjQyRjQ1Mjg0ODJCNGRiNjI1MTY1NTQ2ODU3NmQ1YTcxNDc0Nw==}")
    private String jwtSecret;

    @Autowired
    private UserContext userContext;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            System.out.println("Booking Service: UserContextFilter called for URI = " + request.getRequestURI());
            
            // Legacy user details handling
            String userAgent = request.getHeader("X-User-Details");
            UserContext.setUserDetailsJson(userAgent);
            
            // JWT-based authentication
            String authHeader = request.getHeader("Authorization");
            System.out.println("Booking Service: Received Authorization header = " + (authHeader != null ? authHeader.substring(0, Math.min(50, authHeader.length())) + "..." : "null"));
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                System.out.println("Booking Service: Processing JWT token");
                
                // Validate JWT token
                Claims claims = extractClaims(token);
                if (claims != null) {
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);
                    Long userId = extractUserId(claims);
                    Long roleId = extractRoleId(claims);
                    
                    System.out.println("Booking Service: Extracted claims - username=" + username + ", role=" + role + ", userId=" + userId + ", roleId=" + roleId);
                    
                    // Validate required claims
                    if (username == null || role == null) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Invalid token: missing required claims");
                        return;
                    }
                    
                    // Populate UserContext
                    userContext.setUsername(username);
                    userContext.setRole(role);
                    userContext.setUserId(userId);
                    userContext.setRoleId(roleId);
                    
                    System.out.println("Booking Service: UserContext populated - username=" + userContext.getUsername() + ", role=" + userContext.getRole() + ", userId=" + userContext.getUserId() + ", roleId=" + userContext.getRoleId());
                    
                    // Store user info in request attributes for use in controllers
                    request.setAttribute("username", username);
                    request.setAttribute("role", role);
                    request.setAttribute("userId", userId);
                    request.setAttribute("roleId", roleId);
                } else {
                    // Invalid token
                    System.out.println("Booking Service: Invalid token - claims extraction failed");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token");
                    return;
                }
            } else {
                // No Authorization header - check if this is a public endpoint
                String requestURI = request.getRequestURI();
                System.out.println("Booking Service: No Authorization header for URI = " + requestURI);
                if (!isPublicEndpoint(requestURI)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Authorization header required");
                    return;
                }
            }
            
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear(); // Make sure to clear the user details after the request is processed
        }
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(java.util.Base64.getDecoder().decode(jwtSecret))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error extracting claims from token", e);
            return null;
        }
    }

    private Long extractUserId(Claims claims) {
        try {
            // Try to extract userId from claims, fallback to username if not available
            Object userIdObj = claims.get("userId");
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    return ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    return Long.parseLong((String) userIdObj);
                }
            }
            // If no userId in claims, return null
            return null;
        } catch (Exception e) {
            logger.error("Error extracting userId from claims", e);
            return null;
        }
    }

    private Long extractRoleId(Claims claims) {
        try {
            // Try to extract roleId from claims, fallback to role if not available
            Object roleIdObj = claims.get("roleId");
            if (roleIdObj != null) {
                if (roleIdObj instanceof Number) {
                    return ((Number) roleIdObj).longValue();
                } else if (roleIdObj instanceof String) {
                    return Long.parseLong((String) roleIdObj);
                }
            }
            // If no roleId in claims, return null
            return null;
        } catch (Exception e) {
            logger.error("Error extracting roleId from claims", e);
            return null;
        }
    }
    
    private boolean isPublicEndpoint(String requestURI) {
        // Define public endpoints that don't require authentication
        return requestURI.contains("/v3/api-docs") ||
               requestURI.contains("/swagger-ui") ||
               requestURI.contains("/swagger-ui.html") ||
               requestURI.contains("/actuator/health") ||
               requestURI.contains("/actuator/info");
    }
}
