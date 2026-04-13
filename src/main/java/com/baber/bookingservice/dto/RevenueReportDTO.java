package com.baber.bookingservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Revenue report information")
public class RevenueReportDTO {
    
    @Schema(description = "Date", example = "2024-01-15")
    private LocalDate date;
    
    @Schema(description = "Daily revenue", example = "500.00")
    private double dailyRevenue;
    
    @Schema(description = "Number of appointments", example = "10")
    private long appointmentCount;
    
    @Schema(description = "Average revenue per appointment", example = "50.00")
    private double averageRevenuePerAppointment;
} 