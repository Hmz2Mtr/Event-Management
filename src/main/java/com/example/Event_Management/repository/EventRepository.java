package com.example.Event_Management.repository;

import com.example.Event_Management.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}