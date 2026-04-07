package com.baber.bookingservice.client;

import com.baber.bookingservice.dto.BaseResponse;
import com.baber.bookingservice.dto.PaymentCreateDTO;
import com.baber.bookingservice.dto.PaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "payment-service", url = "${payment.service.url:http://payment-service:8086}")
public interface PaymentClient {
    
    @PostMapping("/api/payment/create")
    BaseResponse<PaymentResponseDTO> createPayment(@RequestBody PaymentCreateDTO dto);
    
    @GetMapping("/api/payment/{id}")
    BaseResponse<PaymentResponseDTO> getPaymentById(@PathVariable Long id);
    
    @PostMapping("/api/payment/{id}/success")
    BaseResponse<PaymentResponseDTO> markPaymentSuccess(@PathVariable Long id);
    
    @PostMapping("/api/payment/{id}/fail")
    BaseResponse<PaymentResponseDTO> markPaymentFailed(@PathVariable Long id, @RequestParam String reason);
    
    @PostMapping("/api/payment/{id}/refund")
    BaseResponse<PaymentResponseDTO> refundPayment(@PathVariable Long id, @RequestParam String providerRefundId);
}
