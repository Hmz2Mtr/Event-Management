package com.example.Event_Management.repository.event;

import com.example.Event_Management.entities.event.Ville;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VilleRepository extends JpaRepository<Ville, Long> {
    Optional<Ville> findByName(String name);
}