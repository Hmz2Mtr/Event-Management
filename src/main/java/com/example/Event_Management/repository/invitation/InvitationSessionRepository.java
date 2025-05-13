package com.example.Event_Management.repository.invitation;

import com.example.Event_Management.entities.invitation.InvitationForm;
import com.example.Event_Management.entities.invitation.InvitationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvitationSessionRepository extends JpaRepository<InvitationSession, Long> {
    List<InvitationSession> findByInvitationForm(InvitationForm form);
//    List<InvitationSession> findByEventId(Long eventId);
}
