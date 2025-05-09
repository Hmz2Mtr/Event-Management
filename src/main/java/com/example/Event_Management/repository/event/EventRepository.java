package com.example.Event_Management.repository.event;

import com.example.Event_Management.entities.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}