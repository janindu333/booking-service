package com.baber.bookingservice.controller;

import com.baber.bookingservice.client.SaloonResolverClient;
import com.baber.bookingservice.configuration.UserContext;
import com.baber.bookingservice.dto.BaseResponse;
import com.baber.bookingservice.model.Appointment;
import com.baber.bookingservice.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Frontend-compatible appointment list API ({@code /api/v1/appointments}).
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments (v1)", description = "Dashboard appointment list API")
public class AppointmentV1Controller {

    private final AppointmentService appointmentService;
    private final SaloonResolverClient saloonResolverClient;
    private final UserContext userContext;

    @Autowired
    public AppointmentV1Controller(AppointmentService appointmentService,
                                 SaloonResolverClient saloonResolverClient,
                                 UserContext userContext) {
        this.appointmentService = appointmentService;
        this.saloonResolverClient = saloonResolverClient;
        this.userContext = userContext;
    }

    @GetMapping
    @Operation(summary = "List appointments for a date", description = "Supports saloonId as numeric id or public UUID")
    public BaseResponse<List<Appointment>> listAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String saloonId,
            @RequestParam(defaultValue = "50") int limit) {
        if (!userContext.isAdmin()) {
            return new BaseResponse<>(false, "Access denied. Admin role required.", 403, "", null);
        }
        try {
            Long resolvedSaloonId = saloonId != null && !saloonId.isBlank()
                    ? saloonResolverClient.resolve(saloonId)
                    : null;
            List<Appointment> appointments = appointmentService.getAppointmentsForDate(resolvedSaloonId, date, limit);
            return new BaseResponse<>(true, "success", 0, "", appointments);
        } catch (IllegalArgumentException e) {
            return new BaseResponse<>(false, e.getMessage(), 400, "", null);
        } catch (Exception e) {
            return new BaseResponse<>(false, "Failed to list appointments", 500, "", null);
        }
    }
}
