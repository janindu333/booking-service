package com.baber.bookingservice;

import com.baber.bookingservice.configuration.ServiceLogger;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableScheduling
public class BookingServiceApplication {

    // Create an instance of ServiceLogger
    private static final ServiceLogger logger = new ServiceLogger(BookingServiceApplication.class);

    @Value("${spring.zipkin.base-url}")
    private String zipkinBaseUrl;

    @PostConstruct
    public void logZipkinUrl() {
        System.out.println("Resolved Zipkin Base URL: " + zipkinBaseUrl);
    }
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);

        // Log a message when the application starts
        logger.info("application started successfully.");
    }

}
