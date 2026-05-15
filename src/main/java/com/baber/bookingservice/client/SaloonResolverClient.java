package com.baber.bookingservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Resolves saloon public UUID or numeric id to internal DB id via saloon-service.
 */
@Component
public class SaloonResolverClient {

    private final RestTemplate restTemplate;

    @Value("${saloon.service.url:http://saloon-service:8083}")
    private String saloonServiceUrl;

    public SaloonResolverClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Long resolve(String saloonIdOrPublicId) {
        if (saloonIdOrPublicId == null || saloonIdOrPublicId.isBlank()) {
            throw new IllegalArgumentException("saloonId is required");
        }
        String url = saloonServiceUrl + "/api/saloon/resolve/{id}";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class, saloonIdOrPublicId.trim());
            if (response == null) {
                throw new IllegalArgumentException("Saloon not found: " + saloonIdOrPublicId);
            }
            Object success = response.get("success");
            if (!(success instanceof Boolean b) || !b) {
                Object msg = response.get("errorMessage");
                throw new IllegalArgumentException(msg != null ? msg.toString() : "Saloon not found: " + saloonIdOrPublicId);
            }
            Object data = response.get("data");
            if (data instanceof Number n) {
                return n.longValue();
            }
            if (data instanceof String s && !s.isBlank()) {
                return Long.parseLong(s.trim());
            }
            throw new IllegalArgumentException("Saloon not found: " + saloonIdOrPublicId);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new IllegalArgumentException("Invalid saloon id: " + saloonIdOrPublicId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve saloon id: " + e.getMessage(), e);
        }
    }
}
