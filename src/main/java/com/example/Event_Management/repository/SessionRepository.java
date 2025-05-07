package com.example.Event_Management.repository;

import com.example.Event_Management.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
}