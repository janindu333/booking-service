package com.baber.bookingservice.controller;

import com.baber.bookingservice.configuration.UserContext;
import com.baber.bookingservice.dto.AppointmentCreateDTO;
import com.baber.bookingservice.dto.AppointmentStatsDTO;
import com.baber.bookingservice.dto.BaseResponse;
import com.baber.bookingservice.dto.PopularTimeSlotDTO;
import com.baber.bookingservice.dto.RevenueReportDTO;
import com.baber.bookingservice.dto.TimeSlotDTO;
import com.baber.bookingservice.dto.AppointmentGetAllDTO;
import com.baber.bookingservice.model.Appointment;
import com.baber.bookingservice.service.AppointmentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/appointment")
@Tag(name = "Appointment Management", description = "APIs for managing appointments")
public class AppointmentController {
    private AppointmentService appointmentService;
    
    @Autowired
    private UserContext userContext;
    
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }
    
    @PostMapping("/create")
    @Operation(summary = "Create a new appointment", description = "Creates a new appointment and sends notification via Kafka")
    public BaseResponse<String> createAppointment(@RequestBody AppointmentCreateDTO appointmentCreateDTO) {
        // Set the userId from the authenticated user if not provided in DTO
        if (appointmentCreateDTO.getUserId() == null && userContext.getUserId() != null) {
            appointmentCreateDTO.setUserId(userContext.getUserId());
        }
        
        appointmentService.createAppointment(appointmentCreateDTO);
        return new BaseResponse<>(true, "success", 0, "", null);
    }
    
    @GetMapping("/getAppointmentByUserId/{uId}")
    @Operation(summary = "Get appointments by user ID", description = "Retrieves all appointments for a specific user")
    public BaseResponse<List<Appointment>> getAppointmentByUserId(
            @Parameter(description = "User ID", example = "1") @PathVariable Long uId) {
        // Users can only access their own appointments, admins can access any
        if (!userContext.isAdmin() && !uId.equals(userContext.getUserId())) {
            return new BaseResponse<>(false, "Access denied", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAppointmentsByCustomerId(uId));
    }

    // Get all appointments (Admin only)
    @GetMapping("/getAll")
    @Operation(summary = "Get all appointments", description = "Retrieves all appointments (Admin only)")
    public BaseResponse<List<AppointmentGetAllDTO>> getAllAppointments() {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAllAppointmentsAsDTOs());
    }

    // Get appointment by ID
    @GetMapping("/getById/{id}")
    @Operation(summary = "Get appointment by ID", description = "Retrieves a specific appointment by its ID")
    public BaseResponse<Appointment> getAppointmentById(
            @Parameter(description = "Appointment ID", example = "1") @PathVariable Long id) {
        return appointmentService.getAppointmentById(id)
                .map(appointment -> {
                    // Users can only access their own appointments, admins can access any
                    if (!userContext.isAdmin() && !appointment.getUserId().equals(userContext.getUserId())) {
                        return new BaseResponse<Appointment>(false, "Access denied", 403, "", null);
                    }
                    return new BaseResponse<Appointment>(true, "success", 0, "", appointment);
                })
                .orElse(new BaseResponse<Appointment>(false, "Appointment not found", 1, "", null));
    }

    // Get appointments by saloon (Admin only)
    @GetMapping("/getBySaloon/{saloonId}")
    @Operation(summary = "Get appointments by saloon", description = "Retrieves all appointments for a specific saloon")
    public BaseResponse<List<Appointment>> getAppointmentsBySaloon(
            @Parameter(description = "Saloon ID", example = "1") @PathVariable Long saloonId) {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAppointmentsBySaloonId(saloonId));
    }

    // Get appointments by specialist (Admin only)
    @GetMapping("/getBySpecialist/{specialistId}")
    @Operation(summary = "Get appointments by specialist", description = "Retrieves all appointments for a specific specialist")
    public BaseResponse<List<Appointment>> getAppointmentsBySpecialist(
            @Parameter(description = "Specialist ID", example = "1") @PathVariable Long specialistId) {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAppointmentsBySpecialistId(specialistId));
    }

    // Get appointments by date range (Admin only)
    @GetMapping("/getByDateRange")
    @Operation(summary = "Get appointments by date range", description = "Retrieves appointments within a specified date range")
    public BaseResponse<List<Appointment>> getAppointmentsByDateRange(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAppointmentsByDateRange(startDate, endDate));
    }

    // Get appointments by status (Admin only)
    @GetMapping("/getByStatus/{status}")
    @Operation(summary = "Get appointments by status", description = "Retrieves appointments with a specific status")
    public BaseResponse<List<Appointment>> getAppointmentsByStatus(
            @Parameter(description = "Appointment status", example = "PENDING") @PathVariable String status) {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAppointmentsByStatus(status));
    }

    // Get appointments by saloon and date range (Admin only)
    @GetMapping("/getBySaloonAndDateRange")
    @Operation(summary = "Get appointments by saloon and date range", description = "Retrieves appointments for a specific saloon within a date range")
    public BaseResponse<List<Appointment>> getAppointmentsBySaloonAndDateRange(
            @Parameter(description = "Saloon ID", example = "1") @RequestParam Long saloonId,
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAppointmentsBySaloonIdAndDateRange(saloonId, startDate, endDate));
    }

    // Get appointments by specialist and date range (Admin only)
    @GetMapping("/getBySpecialistAndDateRange")
    @Operation(summary = "Get appointments by specialist and date range", description = "Retrieves appointments for a specific specialist within a date range")
    public BaseResponse<List<Appointment>> getAppointmentsBySpecialistAndDateRange(
            @Parameter(description = "Specialist ID", example = "1") @RequestParam Long specialistId,
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAppointmentsBySpecialistIdAndDateRange(specialistId, startDate, endDate));
    }

    // Get available time slots for a date
    @GetMapping("/getAvailableSlots")
    @Operation(summary = "Get available time slots", description = "Retrieves available time slots for a specific saloon, specialist, and date")
    public BaseResponse<List<TimeSlotDTO>> getAvailableTimeSlots(
            @Parameter(description = "Saloon ID", example = "1") @RequestParam Long saloonId,
            @Parameter(description = "Specialist ID", example = "2") @RequestParam Long specialistId,
            @Parameter(description = "Date (YYYY-MM-DD)", example = "2024-01-15") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAvailableTimeSlots(saloonId, specialistId, date));
    }

    // Get specialist availability for a date
    @GetMapping("/getSpecialistAvailability")
    @Operation(summary = "Get specialist availability", description = "Retrieves specialist availability for a specific date")
    public BaseResponse<List<TimeSlotDTO>> getSpecialistAvailability(
            @Parameter(description = "Specialist ID", example = "1") @RequestParam Long specialistId,
            @Parameter(description = "Date (YYYY-MM-DD)", example = "2024-01-15") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getSpecialistAvailability(specialistId, date));
    }

    // Get saloon availability for a date
    @GetMapping("/getSaloonAvailability")
    @Operation(summary = "Get saloon availability", description = "Retrieves saloon availability for a specific date")
    public BaseResponse<List<TimeSlotDTO>> getSaloonAvailability(
            @Parameter(description = "Saloon ID", example = "1") @RequestParam Long saloonId,
            @Parameter(description = "Date (YYYY-MM-DD)", example = "2024-01-15") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getSaloonAvailability(saloonId, date));
    }

    // Confirm appointment
    @PutMapping("/confirm/{id}")
    @Operation(summary = "Confirm appointment", description = "Confirms an appointment by setting its status to CONFIRMED")
    public BaseResponse<String> confirmAppointment(
            @Parameter(description = "Appointment ID", example = "1") @PathVariable Long id) {
        // Check if user can confirm this appointment
        return appointmentService.getAppointmentById(id)
                .map(appointment -> {
                    if (!userContext.isAdmin() && !appointment.getUserId().equals(userContext.getUserId())) {
                        return new BaseResponse<String>(false, "Access denied", 403, "", null);
                    }
                    appointmentService.confirmAppointment(id);
                    return new BaseResponse<String>(true, "success", 0, "", null);
                })
                .orElse(new BaseResponse<String>(false, "Appointment not found", 1, "", null));
    }

    // Complete appointment
    @PutMapping("/complete/{id}")
    @Operation(summary = "Complete appointment", description = "Completes an appointment by setting its status to COMPLETED")
    public BaseResponse<String> completeAppointment(
            @Parameter(description = "Appointment ID", example = "1") @PathVariable Long id) {
        // Check if user can complete this appointment
        return appointmentService.getAppointmentById(id)
                .map(appointment -> {
                    if (!userContext.isAdmin() && !appointment.getUserId().equals(userContext.getUserId())) {
                        return new BaseResponse<String>(false, "Access denied", 403, "", null);
                    }
                    appointmentService.completeAppointment(id);
                    return new BaseResponse<String>(true, "success", 0, "", null);
                })
                .orElse(new BaseResponse<String>(false, "Appointment not found", 1, "", null));
    }

    // Reschedule appointment
    @PutMapping("/reschedule/{id}")
    @Operation(summary = "Reschedule appointment", description = "Reschedules an appointment to a new date and time")
    public BaseResponse<String> rescheduleAppointment(
            @Parameter(description = "Appointment ID", example = "1") @PathVariable Long id,
            @Parameter(description = "New date (YYYY-MM-DD)", example = "2024-01-16") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @Parameter(description = "New time (HH:mm)", example = "15:30") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newTime) {
        // Check if user can reschedule this appointment
        return appointmentService.getAppointmentById(id)
                .map(appointment -> {
                    if (!userContext.isAdmin() && !appointment.getUserId().equals(userContext.getUserId())) {
                        return new BaseResponse<String>(false, "Access denied", 403, "", null);
                    }
                    try {
                        appointmentService.rescheduleAppointment(id, newDate, newTime);
                        return new BaseResponse<String>(true, "success", 0, "", null);
                    } catch (IllegalArgumentException e) {
                        return new BaseResponse<String>(false, e.getMessage(), 1, "", null);
                    }
                })
                .orElse(new BaseResponse<String>(false, "Appointment not found", 1, "", null));
    }

    // Get appointment statistics (Admin only)
    @GetMapping("/stats")
    @Operation(summary = "Get appointment statistics", description = "Retrieves appointment statistics and analytics")
    public BaseResponse<AppointmentStatsDTO> getAppointmentStats(HttpServletRequest request) {
        System.out.println("Booking Service: getAppointmentStats called");
        System.out.println("Booking Service: userContext = " + userContext);
        System.out.println("Booking Service: userContext.getClass() = " + userContext.getClass());
        System.out.println("Booking Service: username = " + userContext.getUsername());
        System.out.println("Booking Service: role = " + userContext.getRole());
        System.out.println("Booking Service: userId = " + userContext.getUserId());
        
        // Debug: Print all headers
        System.out.println("Booking Service: All headers received:");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            System.out.println("  " + headerName + " = " + headerValue);
        }
        
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAppointmentStats());
    }

    // Get revenue report (Admin only)
    @GetMapping("/revenue")
    @Operation(summary = "Get revenue report", description = "Retrieves revenue report for a date range")
    public BaseResponse<List<RevenueReportDTO>> getRevenueReport(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getRevenueReport(startDate, endDate));
    }

    // Get popular time slots (Admin only)
    @GetMapping("/popularSlots")
    @Operation(summary = "Get popular time slots", description = "Retrieves popular time slots for a saloon")
    public BaseResponse<List<PopularTimeSlotDTO>> getPopularTimeSlots(
            @Parameter(description = "Saloon ID", example = "1") @RequestParam Long saloonId) {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getPopularTimeSlots(saloonId));
    }

    @PutMapping("/cancel/{id}")
    @Operation(summary = "Cancel appointment", description = "Cancels an appointment by setting its status to CANCELLED")
    public BaseResponse<String> cancelAppointment(
            @Parameter(description = "Appointment ID", example = "1") @PathVariable Long id) {
        // Check if user can cancel this appointment
        return appointmentService.getAppointmentById(id)
                .map(appointment -> {
                    if (!userContext.isAdmin() && !appointment.getUserId().equals(userContext.getUserId())) {
                        return new BaseResponse<String>(false, "Access denied", 403, "", null);
                    }
                    appointmentService.cancelAppointment(id);
                    return new BaseResponse<String>(true, "success", 0, "", null);
                })
                .orElse(new BaseResponse<String>(false, "Appointment not found", 1, "", null));
    }

    @PutMapping("/update")
    @Operation(summary = "Update appointment", description = "Updates an existing appointment")
    public BaseResponse<String> updateAppointment(@RequestBody Appointment appointment) {
        // Check if user can update this appointment
        return appointmentService.getAppointmentById(appointment.getId())
                .map(existingAppointment -> {
                    if (!userContext.isAdmin() && !existingAppointment.getUserId().equals(userContext.getUserId())) {
                        return new BaseResponse<String>(false, "Access denied", 403, "", null);
                    }
                    appointmentService.updateAppointment(appointment);
                    return new BaseResponse<String>(true, "success", 0, "", null);
                })
                .orElse(new BaseResponse<String>(false, "Appointment not found", 1, "", null));
    }

    @GetMapping("/checkAvailability")
    @Operation(summary = "Check appointment availability", description = "Checks if a time slot is available for booking")
    public BaseResponse<Boolean> checkAvailability(
            @Parameter(description = "Saloon ID", example = "1") @RequestParam Long saloonId,
            @Parameter(description = "Specialist ID", example = "1") @RequestParam Long specialistId,
            @Parameter(description = "Date (YYYY-MM-DD)", example = "2024-01-15") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Time (HH:mm)", example = "14:30") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        boolean isAvailable = appointmentService.checkAvailability(saloonId, specialistId, date, time);
        return new BaseResponse<>(true, "success", 0, "", isAvailable);
    }

//    @GetMapping("/test")
//    @CircuitBreaker(name="test",fallbackMethod = "fallbackMethod")
//    @TimeLimiter(name = "saloon")
//    @Retry(name = "saloon")
//    public CompletableFuture<String> test(){
//
//        return CompletableFuture.supplyAsync(()-> "test complete 1");
//    }
//
//    public CompletableFuture<String> fallbackMethod(RuntimeException runtimeException){
//
//       return CompletableFuture.supplyAsync(()-> "Oops something went wrong");
//    }

}
