package com.baber.bookingservice.repository;

import com.baber.bookingservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a WHERE a.userId = :userId")
    List<Appointment> getAppointmentsByUserId(@Param("userId") Long userId);

}
