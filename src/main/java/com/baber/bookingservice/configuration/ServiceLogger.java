package com.baber.bookingservice.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ServiceLogger {

    private Logger logger;

    public ServiceLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
        // Set service name in MDC for each instance
        MDC.put("serviceName", "booking-service");
    }

    public void info(String message) {
        logger.info("[{}] {}", MDC.get("serviceName"), message);
    }

    public void debug(String message) {
        logger.debug("[{}] {}", MDC.get("serviceName"), message);
    }

    public void error(String message) {
        logger.error("[{}] {}", MDC.get("serviceName"), message);
    }

    public void warn(String message) {
        logger.warn("[{}] {}", MDC.get("serviceName"), message);
    }

    public void trace(String message) {
        logger.trace("[{}] {}", MDC.get("serviceName"), message);
    }
}
