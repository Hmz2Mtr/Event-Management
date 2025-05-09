package com.example.Event_Management.entities.event;


import com.example.Event_Management.security.entities.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "SESSION_SPEAKERS")
public class SessionSpeakers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser speaker;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "session_name", nullable = false)
    private String sessionName;

    @Column(name = "speaker_name", nullable = false)
    private String speakerName;

    // Constructors
    public SessionSpeakers() {}

    public SessionSpeakers(Session session, AppUser speaker, String eventName, String sessionName, String speakerName) {
        this.session = session;
        this.speaker = speaker;
        this.eventName = eventName;
        this.sessionName = sessionName;
        this.speakerName = speakerName;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public AppUser getSpeaker() {
        return speaker;
    }

    public void setSpeaker(AppUser speaker) {
        this.speaker = speaker;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }
}