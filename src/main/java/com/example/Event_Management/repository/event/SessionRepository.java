package com.example.Event_Management.repository.event;

import com.example.Event_Management.entities.event.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
}