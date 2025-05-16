package com.example.Event_Management.security.services.invitations;

import com.example.Event_Management.entities.invitation.InvitationForm;
import com.example.Event_Management.entities.invitation.InvitationSession;
import com.example.Event_Management.repository.invitation.InvitationSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InvitationSessionService {
    @Autowired
    private InvitationSessionRepository invitationSessionRepository;

    public List<InvitationSession> findByInvitationForm(InvitationForm form) {
        return invitationSessionRepository.findByInvitationForm(form);
    }

    public InvitationSession findById(Long id) {
        Optional<InvitationSession> session = invitationSessionRepository.findById(id);
        return session.orElse(null);
    }

    public void deleteById(Long id) {
        invitationSessionRepository.deleteById(id);
    }
}
