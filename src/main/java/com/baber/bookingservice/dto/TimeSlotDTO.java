package com.baber.bookingservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Time slot information")
public class TimeSlotDTO {
    
    @Schema(description = "Time slot", example = "09:00")
    private LocalTime time;
    
    @Schema(description = "Whether the time slot is available", example = "true")
    private boolean available;
    
    @Schema(description = "Reason if not available", example = "Already booked")
    private String reason;
} 