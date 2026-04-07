package com.baber.bookingservice.dto;

import com.baber.bookingservice.model.Appointment;

public class AppointmentWithPaymentDTO {
    private Appointment appointment;
    private Long paymentId;
    private String paymentStatus;
    private String message;

    public AppointmentWithPaymentDTO(Appointment appointment, Long paymentId, String paymentStatus, String message) {
        this.appointment = appointment;
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
        this.message = message;
    }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
