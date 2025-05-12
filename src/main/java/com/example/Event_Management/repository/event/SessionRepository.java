package com.example.Event_Management.repository.event;

import com.example.Event_Management.entities.event.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByEventId(Long eventId);

    void deleteByEventId(Long id);
}