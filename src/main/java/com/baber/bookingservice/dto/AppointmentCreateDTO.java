package com.baber.bookingservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Appointment creation request")
public class AppointmentCreateDTO {
    
    @Schema(description = "User ID who is booking the appointment", example = "1", required = true)
    private Long userId;
    
    @Schema(description = "Saloon ID where the appointment is booked", example = "1", required = true)
    private Long saloonId;
    
    @Schema(description = "Appointment date (YYYY-MM-DD)", example = "2024-01-15", required = true)
    private LocalDate date;
    
    @Schema(description = "Appointment time (HH:mm)", example = "14:30", required = true)
    private LocalTime time;
    
    @Schema(description = "Specialist ID who will perform the service", example = "1", required = true)
    private Long specialistId;
    
    @Schema(description = "List of saloon service IDs to be performed", example = "[1, 2, 3]", required = true)
    private List<Long> saloonServiceIds;
    
    @Schema(description = "Additional notes for the appointment", example = "Please arrive 10 minutes early")
    private String notes;
    
    @Schema(description = "Preferred contact method", example = "PHONE", allowableValues = {"PHONE", "EMAIL", "SMS"})
    private String contactMethod;
    
    @Schema(description = "Special requests or requirements", example = "Wheelchair accessible entrance required")
    private String specialRequests;
} 