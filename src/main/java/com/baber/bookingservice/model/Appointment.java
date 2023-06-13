package com.baber.bookingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "t_appointment")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long saloonId;
    private String saloonName;
    private String address;
    private String date;
    private String time;;
    private Long specialistId;
    private String specialistName;

    @OneToMany(cascade = CascadeType.ALL)
    private List<AppointmentServiceId> saloonServiceIds ;


}
