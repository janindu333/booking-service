package com.baber.bookingservice.service;

import com.baber.bookingservice.model.Appointment;
import com.baber.bookingservice.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {

    private AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public void createAppointment(Appointment appointment) {

        appointmentRepository.save(appointment);
    }

    public List<Appointment> getAppointmentsByCustomerId(Long cusId){
return appointmentRepository.getAppointmentsByUserId(cusId);
    }
}
