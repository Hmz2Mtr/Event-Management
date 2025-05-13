package com.example.Event_Management.repository.invitation;

import com.example.Event_Management.entities.invitation.InvitationForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvitationFormRepository extends JpaRepository<InvitationForm, Long> {
    List<InvitationForm> findByEmail(String email);
}
