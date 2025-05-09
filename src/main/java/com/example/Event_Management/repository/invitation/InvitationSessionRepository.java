package com.example.Event_Management.repository.invitation;

import com.example.Event_Management.entities.invitation.InvitationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvitationSessionRepository extends JpaRepository<InvitationSession, Long> {
}
