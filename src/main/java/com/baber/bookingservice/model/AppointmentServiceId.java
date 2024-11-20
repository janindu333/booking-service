package com.baber.bookingservice.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
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
public class AppointmentServiceId extends Base{
    private Long saloonServiceId;

    @ManyToOne
    @JoinColumn(name = "appointment_id") // Specifies the foreign key column name
    @JsonBackReference
    private Appointment appointment;

}
