package com.baber.bookingservice.service;

import com.baber.bookingservice.client.PaymentClient;
import com.baber.bookingservice.dto.AppointmentCreateDTO;
import com.baber.bookingservice.dto.AppointmentStatsDTO;
import com.baber.bookingservice.dto.BaseResponse;
import com.baber.bookingservice.dto.PopularTimeSlotDTO;
import com.baber.bookingservice.dto.RevenueReportDTO;
import com.baber.bookingservice.dto.TimeSlotDTO;
import com.baber.bookingservice.dto.AppointmentGetAllDTO;
import com.baber.bookingservice.dto.PaymentCreateDTO;
import com.baber.bookingservice.dto.PaymentResponseDTO;
import com.baber.bookingservice.dto.AppointmentWithPaymentDTO;
import com.baber.bookingservice.dto.ValidationResult;
import com.baber.bookingservice.model.Appointment;
import com.baber.bookingservice.repository.AppointmentRepository;
import com.baber.bookingservice.configuration.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final KafkaProducerService kafkaProducerService;
    
    @Autowired
    private PaymentClient paymentClient;

    private final UserContext userContext;

    public AppointmentService(AppointmentRepository appointmentRepository, KafkaProducerService kafkaProducerService, UserContext userContext) {
        this.appointmentRepository = appointmentRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.userContext = userContext;
    }

    public void createAppointment(Appointment appointment) {
        appointmentRepository.save(appointment);

        // Send message to Kafka after successful creation
        String message = "Appointment created with ID: " + appointment.getId();
        System.out.println("Appointment created with ID: " + appointment.getId());
        kafkaProducerService.sendMessage("salon.booking", message);
        
        // Create payment intent for the appointment
        createPaymentIntent(appointment.getId(), calculateDefaultAmount());
    }

    // Enhanced Payment-First Approach
    public AppointmentWithPaymentDTO createAppointment(AppointmentCreateDTO appointmentCreateDTO) {
        try {
            // 1. Pre-validation
            ValidationResult validation = validateAppointment(appointmentCreateDTO);
            if (!validation.isValid()) {
                throw new IllegalArgumentException(validation.getMessage());
            }
            
            // 2. Check availability
            if (!checkAvailability(appointmentCreateDTO)) {
                throw new IllegalArgumentException("Selected slot is not available");
            }
            
            // 3. Create temporary booking
            Appointment appointment = createTemporaryBooking(appointmentCreateDTO);
            
            // 4. Create payment intent
            PaymentResponseDTO paymentResponse = createPaymentIntent(appointment.getId(), calculateAmountFromServices(appointmentCreateDTO.getSaloonServiceIds()));
            
            if (paymentResponse == null) {
                // If payment intent fails, cancel the temporary booking
                appointment.setStatus("CANCELLED");
                appointmentRepository.save(appointment);
                throw new RuntimeException("Failed to create payment intent");
            }
            
            // 5. Return with clear next steps
            return new AppointmentWithPaymentDTO(
                appointment,
                paymentResponse.getId(),
                "PENDING_PAYMENT",
                "Appointment reserved! Complete payment within 15 minutes to confirm."
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create appointment: " + e.getMessage());
        }
    }
    
    private PaymentResponseDTO createPaymentIntent(Long appointmentId, BigDecimal amount) {
        try {
            PaymentCreateDTO paymentDto = new PaymentCreateDTO();
            paymentDto.setAppointmentId(appointmentId);
            paymentDto.setAmount(amount);
            paymentDto.setMethod("STRIPE");
            paymentDto.setCurrency("USD");
            
            BaseResponse<PaymentResponseDTO> paymentResponse = paymentClient
            .createPayment(paymentDto);
            
            if (paymentResponse.isSuccess()) {
                System.out.println("Payment intent created successfully: " + paymentResponse.getData().getId());
                return paymentResponse.getData();
            } else {
                System.out.println("Failed to create payment intent: " + paymentResponse.getMessage());
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error creating payment intent: " + e.getMessage());
            // Don't fail the appointment creation if payment fails
            return null;
        }
    }
    
    private BigDecimal calculateDefaultAmount() {
        // Default amount for basic appointment
        return BigDecimal.valueOf(50.0);
    }
    
    private BigDecimal calculateAmountFromServices(List<Long> serviceIds) {
        // TODO: Implement actual service pricing logic
        // For now, return default amount
        return BigDecimal.valueOf(50.0);
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

    // Get client count by saloon
    public long getClientCountBySaloonId(Long saloonId) {
        return appointmentRepository.countDistinctClientsBySaloonId(saloonId);
    }

    // Get total treatments (completed appointments) by saloon
    public long getTreatmentCountBySaloonId(Long saloonId) {
        return appointmentRepository.countBySaloonIdAndStatus(saloonId, "COMPLETED");
    }

    // Get appointments by specialist and date range
    public List<Appointment> getAppointmentsBySpecialistIdAndDateRange(Long specialistId, LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.getAppointmentsBySpecialistIdAndDateRange(specialistId, startDate, endDate);
    }

    // Get available time slots for a date (legacy method - keeping for backward compatibility)
    public List<TimeSlotDTO> getAvailableTimeSlots(Long saloonId, Long specialistId, LocalDate date) {
        List<Appointment> appointments = appointmentRepository.findBySpecialistIdAndDate(specialistId, date);
        List<TimeSlotDTO> timeSlots = new ArrayList<>();
        
        // Generate time slots from 9 AM to 8 PM (1-hour intervals)
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(20, 0);
        
        LocalTime currentTime = startTime;
        while (!currentTime.isAfter(endTime.minusHours(1))) { // Ensure 1-hour slots fit
            final LocalTime timeSlot = currentTime;
            boolean isAvailable = appointments.stream()
                .noneMatch(appointment -> appointment.getTime().equals(timeSlot));
            
            String timeStr = timeSlot.format(DateTimeFormatter.ofPattern("HH:mm"));
            String status = isAvailable ? "AVAILABLE" : "BOOKED";
            String notes = isAvailable ? "" : "Already booked";
            
            // Apply pricing logic
            double price = 50.0;
            if (timeSlot.isBefore(LocalTime.of(11, 0))) {
                price = 45.0; // Early bird discount
                notes = isAvailable ? "Early morning discount" : "Already booked";
            } else if (timeSlot.isAfter(LocalTime.of(17, 0))) {
                price = 55.0; // Evening premium
                notes = isAvailable ? "Evening premium" : "Already booked";
            }
            
            TimeSlotDTO timeSlotDTO = new TimeSlotDTO(timeStr, isAvailable, status, 60, notes, price);
            timeSlots.add(timeSlotDTO);
            
            currentTime = currentTime.plusHours(1);
        }
        
        return timeSlots;
    }

    // Get available time slots for a week
    public Map<String, List<TimeSlotDTO>> getAvailableTimeSlotsForWeek(Long saloonId, Long specialistId, String startDate) {
        try {
            LocalDate weekStart = LocalDate.parse(startDate);
            Map<String, List<TimeSlotDTO>> weeklySlots = new HashMap<>();
            
            // Generate slots for 7 days
            for (int i = 0; i < 7; i++) {
                LocalDate currentDate = weekStart.plusDays(i);
                String dateStr = currentDate.toString();
                
                List<TimeSlotDTO> daySlots = getAvailableTimeSlots(saloonId, specialistId, currentDate);
                weeklySlots.put(dateStr, daySlots);
            }
            
            return weeklySlots;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get weekly available time slots: " + e.getMessage());
        }
    }

    // Generate all possible time slots for business hours
    private List<TimeSlotDTO> generateTimeSlots() {
        List<TimeSlotDTO> slots = new ArrayList<>();
        
        // Business hours: 9 AM to 8 PM (9:00 to 20:00)
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(20, 0);
        
        LocalTime currentTime = startTime;
        while (!currentTime.isAfter(endTime.minusHours(1))) { // Ensure 1-hour slots fit
            String timeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            
            // Add special pricing for early morning slots (9 AM - 11 AM)
            double price = 50.0;
            String notes = "";
            
            if (currentTime.isBefore(LocalTime.of(11, 0))) {
                price = 45.0; // Early bird discount
                notes = "Early morning discount";
            } else if (currentTime.isAfter(LocalTime.of(17, 0))) {
                price = 55.0; // Evening premium
                notes = "Evening premium";
            }
            
            TimeSlotDTO slot = new TimeSlotDTO(timeStr, true, "AVAILABLE", 60, notes, price);
            slots.add(slot);
            
            currentTime = currentTime.plusHours(1);
        }
        
        return slots;
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
            
            String timeStr = timeSlot.format(DateTimeFormatter.ofPattern("HH:mm"));
            String status = isAvailable ? "AVAILABLE" : "BOOKED";
            String notes = isAvailable ? "" : "Already booked";
            
            TimeSlotDTO timeSlotDTO = new TimeSlotDTO(timeStr, isAvailable, status, 30, notes, 50.0);
            
            timeSlots.add(timeSlotDTO);
        }
        
        return timeSlots;
    }

    // Get saloon availability for a date
    public List<TimeSlotDTO> getSaloonAvailability(Long saloonId, LocalDate date) {
        List<Appointment> appointments = appointmentRepository.getAppointmentsBySaloonIdAndDate(saloonId, date);
        List<TimeSlotDTO> timeSlots = new ArrayList<>();
        
        // Generate time slots from 9 AM to 8 PM (1-hour intervals)
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(20, 0);
        
        LocalTime currentTime = startTime;
        while (!currentTime.isAfter(endTime.minusHours(1))) { // Ensure 1-hour slots fit
            final LocalTime timeSlot = currentTime;
            long bookedCount = appointments.stream()
                .filter(appointment -> appointment.getTime().equals(timeSlot))
                .count();
            
            boolean isAvailable = bookedCount < 3; // Assuming max 3 appointments per time slot
            
            String timeStr = timeSlot.format(DateTimeFormatter.ofPattern("HH:mm"));
            String status = isAvailable ? "AVAILABLE" : "UNAVAILABLE";
            String notes = isAvailable ? "" : "Maximum capacity reached";
            
            // Apply pricing logic
            double price = 50.0;
            if (timeSlot.isBefore(LocalTime.of(11, 0))) {
                price = 45.0; // Early bird discount
                notes = isAvailable ? "Early morning discount" : "Maximum capacity reached";
            } else if (timeSlot.isAfter(LocalTime.of(17, 0))) {
                price = 55.0; // Evening premium
                notes = isAvailable ? "Evening premium" : "Maximum capacity reached";
            }
            
            TimeSlotDTO timeSlotDTO = new TimeSlotDTO(timeStr, isAvailable, status, 60, notes, price);
            timeSlots.add(timeSlotDTO);
            
            currentTime = currentTime.plusHours(1);
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

    // Validation method
    private ValidationResult validateAppointment(AppointmentCreateDTO dto) {
        ValidationResult result = new ValidationResult();
        
        // Check required fields
        if (dto.getSaloonId() == null || dto.getDate() == null || dto.getTime() == null) {
            result.setValid(false);
            result.setMessage("Missing required fields: saloonId, date, and time are required");
            return result;
        }
        
        // Check if date is in future
        if (dto.getDate().isBefore(LocalDate.now())) {
            result.setValid(false);
            result.setMessage("Appointment date must be in the future");
            return result;
        }
        
        // Check if time is within business hours (9 AM to 8 PM)
        LocalTime businessStart = LocalTime.of(9, 0);
        LocalTime businessEnd = LocalTime.of(20, 0);
        
        if (dto.getTime().isBefore(businessStart) || dto.getTime().isAfter(businessEnd)) {
            result.setValid(false);
            result.setMessage("Appointment time must be between 9:00 AM and 8:00 PM");
            return result;
        }
        
        result.setValid(true);
        return result;
    }

    // Availability checking
    private boolean checkAvailability(AppointmentCreateDTO dto) {
        // Check if specialist is available at that time using existing method
        return appointmentRepository.existsBySaloonIdAndSpecialistIdAndDateAndTime(
            dto.getSaloonId(), 
            dto.getSpecialistId(), 
            dto.getDate(), 
            dto.getTime()
        );
    }

    // Create temporary booking
    private Appointment createTemporaryBooking(AppointmentCreateDTO dto) {
        Appointment appointment = new Appointment();
        appointment.setUserId(dto.getUserId() != null ? dto.getUserId() : userContext.getUserId());
        appointment.setSaloonId(dto.getSaloonId());
        appointment.setSpecialistId(dto.getSpecialistId());
        appointment.setDate(dto.getDate());
        appointment.setTime(dto.getTime());
        appointment.setSaloonServiceIds(dto.getSaloonServiceIds());
        appointment.setStatus("PENDING_PAYMENT"); // Key status for payment-first approach
        appointment.setCreatedOn(new Date());
        appointment.setCreatedBy(String.valueOf(userContext.getUserId()));
        
        Appointment saved = appointmentRepository.save(appointment);
        
        // Send Kafka message for temporary booking
        String message = "Temporary appointment created with ID: " + saved.getId() + " - Pending payment";
        kafkaProducerService.sendMessage("salon.booking", message);
        
        return saved;
    }

    // Scheduled cleanup for unpaid bookings (runs every 5 minutes)
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cancelUnpaidBookings() {
        try {
            // Find appointments that are pending payment for more than 15 minutes
            Date cutoffTime = new Date(System.currentTimeMillis() - (15 * 60 * 1000)); // 15 minutes ago
            
            List<Appointment> unpaidBookings = findByStatusAndCreatedOnBefore("PENDING_PAYMENT", cutoffTime);
            
            if (!unpaidBookings.isEmpty()) {
                System.out.println("Found " + unpaidBookings.size() + " unpaid bookings to cancel");
                
                unpaidBookings.forEach(appointment -> {
                    appointment.setStatus("CANCELLED");
                    appointmentRepository.save(appointment);
                    
                    // Send cancellation notification via Kafka
                    String message = "Appointment cancelled due to non-payment: " + appointment.getId();
                    kafkaProducerService.sendMessage("salon.booking", message);
                    
                    System.out.println("Cancelled unpaid appointment: " + appointment.getId());
                });
            }
        } catch (Exception e) {
            System.err.println("Error in scheduled cleanup: " + e.getMessage());
        }
    }

    // Helper method to find appointments by status and creation time
    private List<Appointment> findByStatusAndCreatedOnBefore(String status, Date cutoffTime) {
        // This is a simplified implementation - you might want to add this to the repository
        List<Appointment> allPending = appointmentRepository.findByStatus(status);
        return allPending.stream()
            .filter(appointment -> appointment.getCreatedOn().before(cutoffTime))
            .collect(Collectors.toList());
    }

    // Confirm payment and update appointment status
    public AppointmentWithPaymentDTO confirmPayment(Long appointmentId) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        
        // Check if appointment is pending payment
        if (!"PENDING_PAYMENT".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("Appointment is not pending payment. Current status: " + appointment.getStatus());
        }
        
        // Check if payment exists and is successful
        try {
            // Call payment service to check payment status
            BaseResponse<PaymentResponseDTO> paymentResponse = paymentClient.getPaymentById(appointmentId);
            
            if (paymentResponse.isSuccess() && paymentResponse.getData() != null) {
                PaymentResponseDTO payment = paymentResponse.getData();
                
                if ("SUCCESS".equals(payment.getStatus())) {
                    // Update appointment status to confirmed
                    appointment.setStatus("CONFIRMED");
                    appointmentRepository.save(appointment);
                    
                    // Send confirmation message via Kafka
                    String message = "Appointment confirmed with ID: " + appointment.getId() + " - Payment successful";
                    kafkaProducerService.sendMessage("salon.booking", message);
                    
                    return new AppointmentWithPaymentDTO(
                        appointment,
                        payment.getId(),
                        "CONFIRMED",
                        "Appointment confirmed! Payment successful."
                    );
                } else {
                    throw new RuntimeException("Payment is not successful. Current status: " + payment.getStatus());
                }
            } else {
                throw new RuntimeException("Payment not found or failed to retrieve");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to confirm payment: " + e.getMessage());
        }
    }

    // Check specific slot availability
    public TimeSlotDTO checkSpecificSlotAvailability(Long saloonId, Long specialistId, String date, String time) {
        try {
            LocalDate appointmentDate = LocalDate.parse(date);
            LocalTime appointmentTime = LocalTime.parse(time);
            
            // Validate date is in future
            if (appointmentDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Cannot check availability for past dates");
            }
            
            // Validate time is within business hours
            LocalTime businessStart = LocalTime.of(9, 0);
            LocalTime businessEnd = LocalTime.of(20, 0);
            
            if (appointmentTime.isBefore(businessStart) || appointmentTime.isAfter(businessEnd)) {
                return new TimeSlotDTO(time, false, "UNAVAILABLE", 60, "Outside business hours", 0.0);
            }
            
            // Check if slot conflicts with existing appointments
            boolean isAvailable = !appointmentRepository.existsBySaloonIdAndSpecialistIdAndDateAndTime(
                saloonId, specialistId, appointmentDate, appointmentTime);
            
            if (isAvailable) {
                // Calculate price for this time slot
                double price = 50.0;
                String notes = "";
                
                if (appointmentTime.isBefore(LocalTime.of(11, 0))) {
                    price = 45.0; // Early bird discount
                    notes = "Early morning discount";
                } else if (appointmentTime.isAfter(LocalTime.of(17, 0))) {
                    price = 55.0; // Evening premium
                    notes = "Evening premium";
                }
                
                return new TimeSlotDTO(time, true, "AVAILABLE", 60, notes, price);
            } else {
                return new TimeSlotDTO(time, false, "BOOKED", 60, "Slot already booked", 0.0);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to check slot availability: " + e.getMessage());
        }
    }

    // Get available time slots for a specific date (new method for String dates)
    public List<TimeSlotDTO> getAvailableTimeSlots(Long saloonId, Long specialistId, String date) {
        try {
            LocalDate appointmentDate = LocalDate.parse(date);
            
            // Validate date is in future
            if (appointmentDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Cannot check availability for past dates");
            }
            
            // Generate all possible time slots for business hours (9 AM to 8 PM)
            List<TimeSlotDTO> allSlots = generateTimeSlots();
            
            // Get existing appointments for this date and specialist
            List<Appointment> existingAppointments = appointmentRepository
                .findBySpecialistIdAndDate(specialistId, appointmentDate);
            
            // Mark booked slots as unavailable
            for (TimeSlotDTO slot : allSlots) {
                LocalTime slotTime = LocalTime.parse(slot.getTime());
                
                // Check if this slot conflicts with any existing appointment
                boolean isBooked = existingAppointments.stream()
                    .anyMatch(appointment -> {
                        LocalTime appointmentTime = appointment.getTime();
                        // Check if slots overlap (assuming 1-hour appointments)
                        return !slotTime.isBefore(appointmentTime.plusHours(1)) && 
                               !slotTime.plusHours(1).isAfter(appointmentTime);
                    });
                
                if (isBooked) {
                    slot.setAvailable(false);
                    slot.setStatus("BOOKED");
                }
            }
            
            return allSlots;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get available time slots: " + e.getMessage());
        }
    }
}

