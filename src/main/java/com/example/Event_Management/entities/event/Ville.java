package com.example.Event_Management.entities.event;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Ville {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
