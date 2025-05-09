package com.example.Event_Management.repository.invitation;

import com.example.Event_Management.entities.invitation.InvitationForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvitationFormRepository extends JpaRepository<InvitationForm, Long> {
}
