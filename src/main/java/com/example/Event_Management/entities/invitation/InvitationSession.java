package com.example.Event_Management.entities.invitation;

import com.example.Event_Management.entities.event.Session;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invitations")
public class InvitationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invitation_form_id", nullable = false)
    private InvitationForm invitationForm;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "session_name", nullable = false)
    private String sessionName;

    @Column(name = "invitee", nullable = false)
    private String invitee;


    public InvitationSession(InvitationForm invitationForm, Session session, String eventName, String sessionName, String invitee) {
        this.invitationForm = invitationForm;
        this.session = session;
        this.eventName = eventName;
        this.sessionName = sessionName;
        this.invitee = invitee;
    }
}