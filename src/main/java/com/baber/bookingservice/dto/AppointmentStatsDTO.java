package com.baber.bookingservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Appointment statistics")
public class AppointmentStatsDTO {
    
    @Schema(description = "Total appointments", example = "150")
    private long totalAppointments;
    
    @Schema(description = "Pending appointments", example = "25")
    private long pendingAppointments;
    
    @Schema(description = "Confirmed appointments", example = "100")
    private long confirmedAppointments;
    
    @Schema(description = "Completed appointments", example = "20")
    private long completedAppointments;
    
    @Schema(description = "Cancelled appointments", example = "5")
    private long cancelledAppointments;
    
    @Schema(description = "Total revenue", example = "5000.00")
    private double totalRevenue;
    
    @Schema(description = "Average appointments per day", example = "5.2")
    private double averageAppointmentsPerDay;
} 