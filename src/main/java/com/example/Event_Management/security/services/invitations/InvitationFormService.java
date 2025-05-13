package com.example.Event_Management.security.services.invitations;

import com.example.Event_Management.entities.invitation.InvitationForm;
import com.example.Event_Management.repository.invitation.InvitationFormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InvitationFormService {
    @Autowired
    private InvitationFormRepository invitationFormRepository;

    public List<InvitationForm> findByEmail(String email) {
        return invitationFormRepository.findByEmail(email);
    }
}