package com.baber.bookingservice.controller;

import com.baber.bookingservice.dto.BaseResponse;
import com.baber.bookingservice.model.Appointment;
import com.baber.bookingservice.service.AppointmentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    private AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public BaseResponse<String> createAppointment(@RequestBody Appointment appointment) {
        appointmentService.createAppointment(appointment);
        return new BaseResponse<>(true, "success", 0, "", null);
    }

    @GetMapping("/getAppointmentByUserId")
    public BaseResponse<List<Appointment>> getAppointmentsByCustomerId(@RequestParam Long customerId){

        return new BaseResponse<>(true, "success", 0, "", appointmentService.getAppointmentsByCustomerId(customerId));
    }

    @GetMapping("/test")
    @CircuitBreaker(name="test",fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "saloon")
    @Retry(name = "saloon")
    public CompletableFuture<String> test(){

        return CompletableFuture.supplyAsync(()-> "test complete 1");
    }

    public CompletableFuture<String> fallbackMethod(RuntimeException runtimeException){

       return CompletableFuture.supplyAsync(()-> "Oops something went wrong");
    }

}
