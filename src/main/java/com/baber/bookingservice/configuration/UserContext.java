package com.baber.bookingservice.configuration;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class UserContext {
    private static final ThreadLocal<String> userDetailsJsonHolder = new ThreadLocal<>();
    
    // JWT-based user information
    private String username;
    private String role;
    private Long userId;
    private Long roleId; // Added roleId field

    public UserContext() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public boolean isAdmin() {
        if (role == null || role.isBlank()) {
            return false;
        }
        String r = role.trim();
        return "Administrator".equalsIgnoreCase(r)
                || "Manager".equalsIgnoreCase(r)
                || "admin".equalsIgnoreCase(r)
                || "super_admin".equalsIgnoreCase(r)
                || "owner".equalsIgnoreCase(r);
    }

    public boolean isStaff() {
        return "Staff".equals(role) || "Scheduler".equals(role);
    }

    public boolean isCustomer() {
        return "Customer".equals(role);
    }

    // Legacy method for backward compatibility
    public static void setUserDetailsJson(String userDetailsJson) {
        userDetailsJsonHolder.set(userDetailsJson);
    }

    public static String getUserDetailsJson() {
        return userDetailsJsonHolder.get();
    }

    public static void clear() {
        userDetailsJsonHolder.remove();
    }
    
    // JWT-based user information methods
    public boolean isUser() {
        return "Customer".equals(role);
    }
    
    public boolean isAuthenticated() {
        return username != null && !username.isEmpty();
    }
}

