package com.baber.bookingservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "t_appointment")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Appointment extends Base{

    private Long userId;
    private Long saloonId;
    private LocalDate date;
    private String status = "PENDING";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime time;
    private Long specialistId;

    @ElementCollection
    @CollectionTable(name = "appointment_saloon_service", joinColumns = @JoinColumn(name = "appointment_id"))
    @Column(name = "saloon_service_id")
    private List<Long> saloonServiceIds;

}
