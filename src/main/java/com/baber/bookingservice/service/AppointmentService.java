package com.baber.bookingservice.service;

import com.baber.bookingservice.model.Appointment;
import com.baber.bookingservice.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final KafkaProducerService kafkaProducerService;

    public AppointmentService(AppointmentRepository appointmentRepository, KafkaProducerService kafkaProducerService) {
        this.appointmentRepository = appointmentRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public void createAppointment(Appointment appointment) {
        appointmentRepository.save(appointment);

        // Send message to Kafka after successful creation
        String message = "Appointment created with ID: " + appointment.getId();
        System.out.println("Appointment created with ID: " );
        kafkaProducerService.sendMessage("salon.booking", message);
    }
    public List<Appointment> getAppointmentsByCustomerId(Long cusId) {
        return appointmentRepository.getAppointmentsByUserId(cusId);
    }
    public void updateAppointment(Appointment appointment) {
        Appointment existingAppointment = appointmentRepository.findById(appointment.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        existingAppointment.setDate(appointment.getDate());
        existingAppointment.setTime(appointment.getTime());
        existingAppointment.setSaloonId(appointment.getSaloonId());
        existingAppointment.setSpecialistId(appointment.getSpecialistId());
        existingAppointment.setSaloonServiceIds(appointment.getSaloonServiceIds());
        existingAppointment.setStatus(appointment.getStatus());
        appointmentRepository.save(existingAppointment);
    }
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        appointment.setStatus("CANCELLED");
        appointmentRepository.save(appointment);
    }
    public boolean checkAvailability(Long saloonId, Long specialistId, LocalDate date, LocalTime time) {
        return !appointmentRepository.existsBySaloonIdAndSpecialistIdAndDateAndTime(saloonId, specialistId, date, time);
    }

}

