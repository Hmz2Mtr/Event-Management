package com.example.Event_Management.entities.invitation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invitation_forms")
public class InvitationForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String email;

    @OneToMany(mappedBy = "invitationForm", cascade = CascadeType.ALL)
    private List<InvitationSession> invitationSessions = new ArrayList<>();

    public InvitationForm(String firstName, String lastName, String phoneNumber, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    // Add method to append InvitationSession to the list
    public void addInvitationSession(InvitationSession invitationSession) {
        this.invitationSessions.add(invitationSession);
    }
}