package com.baber.bookingservice.repository;

import com.baber.bookingservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a WHERE a.userId = :userId")
    List<Appointment> getAppointmentsByUserId(@Param("userId") Long userId);

    boolean existsBySaloonIdAndSpecialistIdAndDateAndTime(Long saloonId, Long specialistId, LocalDate date,
                                                          LocalTime time);

    // Get appointments by saloon
    @Query("SELECT a FROM Appointment a WHERE a.saloonId = :saloonId")
    List<Appointment> getAppointmentsBySaloonId(@Param("saloonId") Long saloonId);

    // Get appointments by specialist
    @Query("SELECT a FROM Appointment a WHERE a.specialistId = :specialistId")
    List<Appointment> getAppointmentsBySpecialistId(@Param("specialistId") Long specialistId);

    // Get appointments by date range
    @Query("SELECT a FROM Appointment a WHERE a.date BETWEEN :startDate AND :endDate")
    List<Appointment> getAppointmentsByDateRange(@Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate);

    // Get appointments by status
    @Query("SELECT a FROM Appointment a WHERE a.status = :status")
    List<Appointment> getAppointmentsByStatus(@Param("status") String status);

    // Get appointments by saloon and date range
    @Query("SELECT a FROM Appointment a WHERE a.saloonId = :saloonId AND a.date BETWEEN :startDate AND :endDate")
    List<Appointment> getAppointmentsBySaloonIdAndDateRange(@Param("saloonId") Long saloonId,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    // Get appointments by specialist and date range
    @Query("SELECT a FROM Appointment a WHERE a.specialistId = :specialistId AND a.date BETWEEN :startDate AND :endDate")
    List<Appointment> getAppointmentsBySpecialistIdAndDateRange(@Param("specialistId") Long specialistId,
                                                                @Param("startDate") LocalDate startDate,
                                                                @Param("endDate") LocalDate endDate);

    // Get appointments by saloon and date
    @Query("SELECT a FROM Appointment a WHERE a.saloonId = :saloonId AND a.date = :date")
    List<Appointment> getAppointmentsBySaloonIdAndDate(@Param("saloonId") Long saloonId, @Param("date") LocalDate date);

    // Get appointments by specialist and date
    @Query("SELECT a FROM Appointment a WHERE a.specialistId = :specialistId AND a.date = :date")
    List<Appointment> getAppointmentsBySpecialistIdAndDate(@Param("specialistId") Long specialistId, @Param("date") LocalDate date);

    // Get appointments by saloon, specialist and date
    @Query("SELECT a FROM Appointment a WHERE a.saloonId = :saloonId AND a.specialistId = :specialistId AND a.date = :date")
    List<Appointment> getAppointmentsBySaloonIdAndSpecialistIdAndDate(@Param("saloonId") Long saloonId, 
                                                                      @Param("specialistId") Long specialistId, 
                                                                      @Param("date") LocalDate date);

    // Count appointments by status
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status")
    long countByStatus(@Param("status") String status);

    // Count appointments by date range
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.date BETWEEN :startDate AND :endDate")
    long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Count appointments by saloon and date range
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.saloonId = :saloonId AND a.date BETWEEN :startDate AND :endDate")
    long countBySaloonIdAndDateRange(@Param("saloonId") Long saloonId, 
                                     @Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);

    // Get popular time slots by saloon
    @Query("SELECT a.time, COUNT(a) as count FROM Appointment a WHERE a.saloonId = :saloonId GROUP BY a.time ORDER BY count DESC")
    List<Object[]> getPopularTimeSlotsBySaloonId(@Param("saloonId") Long saloonId);

    // Get appointments by status and date range
    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.date BETWEEN :startDate AND :endDate")
    List<Appointment> getAppointmentsByStatusAndDateRange(@Param("status") String status,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);
}

