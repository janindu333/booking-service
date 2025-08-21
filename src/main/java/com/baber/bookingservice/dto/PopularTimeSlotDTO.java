package com.baber.bookingservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Popular time slot information")
public class PopularTimeSlotDTO {
    
    @Schema(description = "Time slot", example = "14:30")
    private LocalTime time;
    
    @Schema(description = "Number of bookings at this time", example = "25")
    private long bookingCount;
    
    @Schema(description = "Percentage of total bookings", example = "15.5")
    private double percentage;
} 