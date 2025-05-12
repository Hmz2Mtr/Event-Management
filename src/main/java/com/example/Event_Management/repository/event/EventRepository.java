package com.example.Event_Management.repository.event;

import com.example.Event_Management.entities.event.Event;
import com.example.Event_Management.security.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCreatedBy(AppUser createdBy);
}