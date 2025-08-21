package com.baber.bookingservice.service;

import com.baber.bookingservice.dto.AppointmentCreateDTO;
import com.baber.bookingservice.dto.AppointmentStatsDTO;
import com.baber.bookingservice.dto.PopularTimeSlotDTO;
import com.baber.bookingservice.dto.RevenueReportDTO;
import com.baber.bookingservice.dto.TimeSlotDTO;
import com.baber.bookingservice.dto.AppointmentGetAllDTO;
import com.baber.bookingservice.model.Appointment;
import com.baber.bookingservice.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public void createAppointment(AppointmentCreateDTO appointmentCreateDTO) {
        Appointment appointment = convertToAppointment(appointmentCreateDTO);
        appointmentRepository.save(appointment);

        // Send message to Kafka after successful creation
        String message = "Appointment created with ID: " + appointment.getId();
        System.out.println("Appointment created with ID: " + appointment.getId());
        kafkaProducerService.sendMessage("salon.booking", message);
    }

    private Appointment convertToAppointment(AppointmentCreateDTO dto) {
        Appointment appointment = new Appointment();
        appointment.setUserId(dto.getUserId());
        appointment.setSaloonId(dto.getSaloonId());
        appointment.setDate(dto.getDate());
        appointment.setTime(dto.getTime());
        appointment.setSpecialistId(dto.getSpecialistId());
        appointment.setSaloonServiceIds(dto.getSaloonServiceIds());
        appointment.setStatus("PENDING"); // Default status
        return appointment;
    }

    public List<Appointment> getAppointmentsByCustomerId(Long cusId) {
        return appointmentRepository.getAppointmentsByUserId(cusId);
    }

    // Get all appointments (Admin)
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Get all appointments as DTOs (Admin) - Clean response without audit fields
    public List<AppointmentGetAllDTO> getAllAppointmentsAsDTOs() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return appointments.stream()
                .map(this::convertToGetAllDTO)
                .collect(Collectors.toList());
    }

    // Helper method to convert Appointment to AppointmentGetAllDTO
    private AppointmentGetAllDTO convertToGetAllDTO(Appointment appointment) {
        return new AppointmentGetAllDTO(
            appointment.getUserId(),
            appointment.getSaloonId(),
            appointment.getDate(),
            appointment.getStatus(),
            appointment.getTime(),
            appointment.getSpecialistId(),
            appointment.getSaloonServiceIds()
        );
    }

    // Get appointment by ID
    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    // Get appointments by saloon
    public List<Appointment> getAppointmentsBySaloonId(Long saloonId) {
        return appointmentRepository.getAppointmentsBySaloonId(saloonId);
    }

    // Get appointments by specialist
    public List<Appointment> getAppointmentsBySpecialistId(Long specialistId) {
        return appointmentRepository.getAppointmentsBySpecialistId(specialistId);
    }

    // Get appointments by date range
    public List<Appointment> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.getAppointmentsByDateRange(startDate, endDate);
    }

    // Get appointments by status
    public List<Appointment> getAppointmentsByStatus(String status) {
        return appointmentRepository.getAppointmentsByStatus(status);
    }

    // Get appointments by saloon and date range
    public List<Appointment> getAppointmentsBySaloonIdAndDateRange(Long saloonId, LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.getAppointmentsBySaloonIdAndDateRange(saloonId, startDate, endDate);
    }

    // Get appointments by specialist and date range
    public List<Appointment> getAppointmentsBySpecialistIdAndDateRange(Long specialistId, LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.getAppointmentsBySpecialistIdAndDateRange(specialistId, startDate, endDate);
    }

    // Get available time slots for a date
    public List<TimeSlotDTO> getAvailableTimeSlots(Long saloonId, Long specialistId, LocalDate date) {
        List<TimeSlotDTO> timeSlots = new ArrayList<>();
        
        // Generate time slots from 9 AM to 6 PM (30-minute intervals)
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        
        for (LocalTime currentTime = startTime; !currentTime.isAfter(endTime); currentTime = currentTime.plusMinutes(30)) {
            boolean isAvailable = !appointmentRepository.existsBySaloonIdAndSpecialistIdAndDateAndTime(
                saloonId, specialistId, date, currentTime);
            
            TimeSlotDTO timeSlot = new TimeSlotDTO();
            timeSlot.setTime(currentTime);
            timeSlot.setAvailable(isAvailable);
            timeSlot.setReason(isAvailable ? null : "Already booked");
            
            timeSlots.add(timeSlot);
        }
        
        return timeSlots;
    }

    // Get specialist availability for a date
    public List<TimeSlotDTO> getSpecialistAvailability(Long specialistId, LocalDate date) {
        List<Appointment> appointments = appointmentRepository.getAppointmentsBySpecialistIdAndDate(specialistId, date);
        List<TimeSlotDTO> timeSlots = new ArrayList<>();
        
        // Generate time slots from 9 AM to 6 PM (30-minute intervals)
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        
        for (LocalTime currentTime = startTime; !currentTime.isAfter(endTime); currentTime = currentTime.plusMinutes(30)) {
            final LocalTime timeSlot = currentTime;
            boolean isAvailable = appointments.stream()
                .noneMatch(appointment -> appointment.getTime().equals(timeSlot));
            
            TimeSlotDTO timeSlotDTO = new TimeSlotDTO();
            timeSlotDTO.setTime(timeSlot);
            timeSlotDTO.setAvailable(isAvailable);
            timeSlotDTO.setReason(isAvailable ? null : "Already booked");
            
            timeSlots.add(timeSlotDTO);
        }
        
        return timeSlots;
    }

    // Get saloon availability for a date
    public List<TimeSlotDTO> getSaloonAvailability(Long saloonId, LocalDate date) {
        List<Appointment> appointments = appointmentRepository.getAppointmentsBySaloonIdAndDate(saloonId, date);
        List<TimeSlotDTO> timeSlots = new ArrayList<>();
        
        // Generate time slots from 9 AM to 6 PM (30-minute intervals)
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        
        for (LocalTime currentTime = startTime; !currentTime.isAfter(endTime); currentTime = currentTime.plusMinutes(30)) {
            final LocalTime timeSlot = currentTime;
            long bookedCount = appointments.stream()
                .filter(appointment -> appointment.getTime().equals(timeSlot))
                .count();
            
            boolean isAvailable = bookedCount < 3; // Assuming max 3 appointments per time slot
            
            TimeSlotDTO timeSlotDTO = new TimeSlotDTO();
            timeSlotDTO.setTime(timeSlot);
            timeSlotDTO.setAvailable(isAvailable);
            timeSlotDTO.setReason(isAvailable ? null : "Maximum capacity reached");
            
            timeSlots.add(timeSlotDTO);
        }
        
        return timeSlots;
    }

    // Confirm appointment
    public void confirmAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        appointment.setStatus("CONFIRMED");
        appointmentRepository.save(appointment);
    }

    // Complete appointment
    public void completeAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        appointment.setStatus("COMPLETED");
        appointmentRepository.save(appointment);
    }

    // Reschedule appointment
    public void rescheduleAppointment(Long appointmentId, LocalDate newDate, LocalTime newTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        
        // Check if new time slot is available
        if (appointmentRepository.existsBySaloonIdAndSpecialistIdAndDateAndTime(
                appointment.getSaloonId(), appointment.getSpecialistId(), newDate, newTime)) {
            throw new IllegalArgumentException("New time slot is not available");
        }
        
        appointment.setDate(newDate);
        appointment.setTime(newTime);
        appointmentRepository.save(appointment);
    }

    // Get appointment statistics
    public AppointmentStatsDTO getAppointmentStats() {
        long totalAppointments = appointmentRepository.count();
        long pendingAppointments = appointmentRepository.countByStatus("PENDING");
        long confirmedAppointments = appointmentRepository.countByStatus("CONFIRMED");
        long completedAppointments = appointmentRepository.countByStatus("COMPLETED");
        long cancelledAppointments = appointmentRepository.countByStatus("CANCELLED");
        
        // Calculate average appointments per day (last 30 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        long appointmentsInLast30Days = appointmentRepository.countByDateRange(startDate, endDate);
        double averageAppointmentsPerDay = appointmentsInLast30Days / 30.0;
        
        // Mock revenue calculation (in real app, this would come from service pricing)
        double totalRevenue = completedAppointments * 50.0; // Assuming $50 per appointment
        
        return new AppointmentStatsDTO(
            totalAppointments,
            pendingAppointments,
            confirmedAppointments,
            completedAppointments,
            cancelledAppointments,
            totalRevenue,
            averageAppointmentsPerDay
        );
    }

    // Get revenue report
    public List<RevenueReportDTO> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        List<Appointment> appointments = appointmentRepository.getAppointmentsByStatusAndDateRange("COMPLETED", startDate, endDate);
        
        return appointments.stream()
            .collect(Collectors.groupingBy(Appointment::getDate))
            .entrySet().stream()
            .map(entry -> {
                LocalDate date = entry.getKey();
                List<Appointment> dailyAppointments = entry.getValue();
                long appointmentCount = dailyAppointments.size();
                double dailyRevenue = appointmentCount * 50.0; // Assuming $50 per appointment
                double averageRevenuePerAppointment = appointmentCount > 0 ? dailyRevenue / appointmentCount : 0.0;
                
                return new RevenueReportDTO(date, dailyRevenue, appointmentCount, averageRevenuePerAppointment);
            })
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .collect(Collectors.toList());
    }

    // Get popular time slots
    public List<PopularTimeSlotDTO> getPopularTimeSlots(Long saloonId) {
        List<Object[]> popularSlots = appointmentRepository.getPopularTimeSlotsBySaloonId(saloonId);
        long totalBookings = popularSlots.stream()
            .mapToLong(slot -> (Long) slot[1])
            .sum();
        
        return popularSlots.stream()
            .map(slot -> {
                LocalTime time = (LocalTime) slot[0];
                long bookingCount = (Long) slot[1];
                double percentage = totalBookings > 0 ? (bookingCount * 100.0) / totalBookings : 0.0;
                
                return new PopularTimeSlotDTO(time, bookingCount, percentage);
            })
            .collect(Collectors.toList());
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

