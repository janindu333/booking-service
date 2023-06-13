package com.baber.bookingservice.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "t_sallonservice")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentServiceId {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long appointmentId;
    private Long saloonServiceId;

}
