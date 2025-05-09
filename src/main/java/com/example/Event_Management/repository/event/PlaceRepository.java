package com.example.Event_Management.repository.event;

import com.example.Event_Management.entities.event.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
}
