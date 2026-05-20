package com.baber.bookingservice.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Accepts legacy HS256 tokens (identity-issued) and Keycloak RS256 access tokens.
 * Keycloak tokens are parsed as payload after the gateway has validated the signature;
 * local userId is resolved via identity-service when absent from claims.
 */
@Component
@Order(1)
public class UserContextFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(UserContextFilter.class);

    @Value("${jwt.secret:U2dWa1lwMzczNjc5NzkyRjQyRjQ1Mjg0ODJCNGRiNjI1MTY1NTQ2ODU3NmQ1YTcxNDc0Nw==}")
    private String jwtSecret;

    @Value("${identity.service.url:http://identity-service:8082}")
    private String identityServiceUrl;

    @Autowired
    private UserContext userContext;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Key signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    private Map<String, Object> parseTokenClaims(String token) throws JwtException {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtException("Malformed JWT");
        }
        String alg;
        try {
            byte[] headerBytes = Decoders.BASE64URL.decode(parts[0]);
            JsonNode header = objectMapper.readTree(headerBytes);
            JsonNode algNode = header.get("alg");
            alg = algNode != null && algNode.isTextual() ? algNode.asText() : "";
        } catch (IOException e) {
            throw new JwtException("Invalid JWT header", e);
        }

        if ("HS256".equals(alg)) {
            Claims body = Jwts.parserBuilder()
                    .setSigningKey(signingKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return body;
        }

        if ("RS256".equals(alg) || "RS384".equals(alg) || "RS512".equals(alg)
                || "ES256".equals(alg) || "ES384".equals(alg) || "ES512".equals(alg)) {
            try {
                byte[] payloadBytes = Decoders.BASE64URL.decode(parts[1]);
                return objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                throw new JwtException("Invalid JWT payload", e);
            }
        }

        throw new JwtException("Unsupported JWT algorithm: " + alg);
    }

    @Nullable
    private static String stringClaim(Map<String, ?> claims, String name) {
        Object v = claims.get(name);
        return v instanceof String s && !s.isBlank() ? s : null;
    }

    private static void addRolesFromAccess(Map<?, ?> accessMap, List<String> out) {
        if (accessMap == null) {
            return;
        }
        Object rolesObj = accessMap.get("roles");
        if (!(rolesObj instanceof Collection<?> roles)) {
            return;
        }
        for (Object roleObj : roles) {
            if (roleObj == null) {
                continue;
            }
            String r = roleObj.toString().trim();
            if (!r.isEmpty()) {
                out.add(r);
            }
        }
    }

    private static void collectKeycloakRoles(Map<String, ?> claims, List<String> out) {
        Object realmAccessObj = claims.get("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            addRolesFromAccess(realmAccess, out);
        }
        Object resourceAccessObj = claims.get("resource_access");
        if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
            for (Object clientObj : resourceAccess.values()) {
                if (clientObj instanceof Map<?, ?> clientAccess) {
                    addRolesFromAccess(clientAccess, out);
                }
            }
        }
    }

    /** Custom / legacy tokens: top-level {@code roles} or Spring-style {@code authorities}. */
    private static void collectTopLevelRoleArrays(Map<String, ?> claims, List<String> out) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof Collection<?> roles) {
            for (Object roleObj : roles) {
                if (roleObj == null) {
                    continue;
                }
                String r = roleObj.toString().trim();
                if (!r.isEmpty()) {
                    out.add(r);
                }
            }
        }
        Object authObj = claims.get("authorities");
        if (authObj instanceof Collection<?> auths) {
            for (Object a : auths) {
                if (a == null) {
                    continue;
                }
                String r = a.toString().trim();
                if (!r.isEmpty()) {
                    out.add(r);
                }
            }
        }
    }

    /** Strip {@code ROLE_} / {@code role_} prefixes for matching (e.g. {@code role_admin} → admin). */
    private static String stripRolePrefix(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        while (t.length() >= 5) {
            if (t.regionMatches(true, 0, "ROLE_", 0, 5)) {
                t = t.substring(5).trim();
            } else if (t.regionMatches(true, 0, "role_", 0, 5)) {
                t = t.substring(5).trim();
            } else {
                break;
            }
        }
        return t;
    }

    private static boolean roleMatchesPreferred(String raw, String preferred) {
        if (raw == null || preferred == null) {
            return false;
        }
        if (preferred.equalsIgnoreCase(raw)) {
            return true;
        }
        String stripped = stripRolePrefix(raw);
        return preferred.equalsIgnoreCase(stripped);
    }

    @Nullable
    private String canonicalizeChosenRole(String raw) {
        if (raw == null) {
            return null;
        }
        String stripped = stripRolePrefix(raw);
        if ("admin".equalsIgnoreCase(stripped) || "administrator".equalsIgnoreCase(stripped)) {
            return "admin";
        }
        if ("super_admin".equalsIgnoreCase(stripped)) {
            return "super_admin";
        }
        if ("owner".equalsIgnoreCase(stripped)) {
            return "owner";
        }
        return raw.trim();
    }

    @Nullable
    private String extractRole(Map<String, ?> claims) {
        String direct = stringClaim(claims, "role");
        if (direct != null) {
            return canonicalizeChosenRole(direct);
        }
        List<String> roles = new ArrayList<>();
        collectKeycloakRoles(claims, roles);
        collectTopLevelRoleArrays(claims, roles);
        for (String preferred : List.of(
                "super_admin", "admin", "owner", "Administrator", "Manager", "Customer", "Staff", "Scheduler")) {
            for (String r : roles) {
                if (roleMatchesPreferred(r, preferred)) {
                    return canonicalizeChosenRole(r);
                }
            }
        }
        return roles.isEmpty() ? null : canonicalizeChosenRole(roles.get(0));
    }

    @Nullable
    private Long resolveUserIdFromIdentity(String authHeader) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    identityServiceUrl + "/auth/internal/me-id",
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.warn("identity /auth/internal/me-id non-2xx or empty body: {}", response.getStatusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode successNode = root.get("success");
            if (successNode == null || !successNode.asBoolean(false)) {
                JsonNode msg = root.get("message");
                logger.warn("identity /auth/internal/me-id success=false: {}", msg != null ? msg.asText() : response.getBody());
                return null;
            }
            JsonNode data = root.get("data");
            if (data != null && data.isNumber()) {
                return data.longValue();
            }
            if (data != null && data.isTextual()) {
                return Long.parseLong(data.asText());
            }
            return null;
        } catch (RestClientResponseException e) {
            logger.warn("identity /auth/internal/me-id HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.warn("Could not resolve user id from identity-service: {}", e.getMessage());
            return null;
        }
    }

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.contains("/v3/api-docs")
                || requestURI.contains("/swagger-ui")
                || requestURI.contains("/swagger-ui.html")
                || requestURI.contains("/actuator/health")
                || requestURI.contains("/actuator/info");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String userAgent = request.getHeader("X-User-Details");
            UserContext.setUserDetailsJson(userAgent);

            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (token.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid authorization header");
                    return;
                }
                try {
                    Map<String, Object> claims = parseTokenClaims(token);

                    String username = stringClaim(claims, "preferred_username");
                    if (username == null) {
                        username = stringClaim(claims, "email");
                    }
                    if (username == null) {
                        username = stringClaim(claims, "username");
                    }
                    if (username == null) {
                        username = claims.get("sub") instanceof String s && !s.isBlank() ? s : null;
                    }
                    if (username == null) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Invalid token: missing subject/username");
                        return;
                    }

                    String role = extractRole(claims);
                    if (role == null) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Invalid token: missing required role claims");
                        return;
                    }

                    Long userId = null;
                    Object userIdObj = claims.get("userId");
                    if (userIdObj instanceof Number) {
                        userId = ((Number) userIdObj).longValue();
                    } else if (userIdObj instanceof String userIdStr && !userIdStr.isBlank()) {
                        userId = Long.parseLong(userIdStr);
                    }
                    if (userId == null) {
                        userId = resolveUserIdFromIdentity(authHeader);
                    }

                    Long roleId = null;
                    Object roleIdObj = claims.get("roleId");
                    if (roleIdObj instanceof Number) {
                        roleId = ((Number) roleIdObj).longValue();
                    } else if (roleIdObj instanceof String roleIdStr && !roleIdStr.isBlank()) {
                        roleId = Long.parseLong(roleIdStr);
                    }

                    userContext.setUsername(username);
                    userContext.setRole(role);
                    userContext.setUserId(userId);
                    userContext.setRoleId(roleId);

                    request.setAttribute("username", username);
                    request.setAttribute("role", role);
                    request.setAttribute("userId", userId);
                    request.setAttribute("roleId", roleId);
                } catch (JwtException e) {
                    logger.warn("JWT rejected: {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token");
                    return;
                }
            } else {
                String requestURI = request.getRequestURI();
                if (!isPublicEndpoint(requestURI)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Authorization header required");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
