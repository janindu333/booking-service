package com.baber.bookingservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Time slot information")
public class TimeSlotDTO {
    
    @Schema(description = "Time slot (HH:mm)", example = "14:30")
    private String time;
    
    @Schema(description = "Whether the slot is available", example = "true")
    private boolean available;
    
    @Schema(description = "Slot status", example = "AVAILABLE", allowableValues = {"AVAILABLE", "BOOKED", "UNAVAILABLE"})
    private String status;
    
    @Schema(description = "Duration in minutes", example = "60")
    private Integer durationMinutes;
    
    @Schema(description = "Special notes for this slot", example = "Early morning discount")
    private String notes;
    
    @Schema(description = "Price for this time slot", example = "75.00")
    private Double price;
    
    // Constructor for simple time slot
    public TimeSlotDTO(String time, boolean available) {
        this.time = time;
        this.available = available;
        this.status = available ? "AVAILABLE" : "BOOKED";
        this.durationMinutes = 60; // Default 1 hour
        this.notes = "";
        this.price = 50.0; // Default price
    }
    
    // Constructor for detailed time slot
    public TimeSlotDTO(String time, boolean available, String status, Integer durationMinutes, String notes, Double price) {
        this.time = time;
        this.available = available;
        this.status = status;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
        this.price = price;
    }
} 