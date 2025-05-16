package com.example.Event_Management.entities.event;

import com.example.Event_Management.entities.invitation.InvitationSession;
import com.example.Event_Management.security.entities.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean facialDetection = false;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;


    private String eventName;
    private String creatorName;



    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionSpeakers> guestSpeakers = new ArrayList<>();


    // Helper method to add a speaker
    public void addGuestSpeaker(AppUser speaker, String eventName, String sessionName, String speakerName) {
        SessionSpeakers sessionSpeaker = new SessionSpeakers(this, speaker, eventName, sessionName, speakerName);
        guestSpeakers.add(sessionSpeaker);
    }

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<InvitationSession> invitationSessions = new ArrayList<>();




}