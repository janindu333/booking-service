package com.baber.bookingservice.controller;

import com.baber.bookingservice.dto.BaseResponse;
import com.baber.bookingservice.model.Appointment;
import com.baber.bookingservice.service.AppointmentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {
    private AppointmentService appointmentService;
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }
    @PostMapping("/create")
    public BaseResponse<String> createAppointment(@RequestBody Appointment appointment) {

        appointmentService.createAppointment(appointment);
        return new BaseResponse<>(true, "success", 0, "", null);
    }
    @GetMapping("/getAppointmentByUserId/{uId}")
    public BaseResponse<List<Appointment>> getAppointmentByUserId(@PathVariable Long uId) {
        return new BaseResponse<>(true, "success", 0, "",
                appointmentService.getAppointmentsByCustomerId(uId));
    }

    @PutMapping("/cancel/{id}")
    public BaseResponse<String> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return new BaseResponse<>(true, "success", 0, "", null);
    }

    @PutMapping("/update")
    public BaseResponse<String> updateAppointment(@RequestBody Appointment appointment) {
        appointmentService.updateAppointment(appointment);
        return new BaseResponse<>(true, "success", 0, "", null);
    }

    @GetMapping("/checkAvailability")
    public BaseResponse<Boolean> checkAvailability(@RequestParam Long saloonId,
                                                   @RequestParam Long specialistId,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                       LocalDate date,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
                                                       LocalTime time) {
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
