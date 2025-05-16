package com.example.Event_Management.DTO.event;

import com.example.Event_Management.entities.event.SessionSpeakers;
import lombok.Data;

import java.util.List;

@Data
public class SessionDTO {
    private String name;
    private String date;
    private String startTime;
    private String endTime;
    private PlaceDTO place;
    private String guestSpeakers;
    private boolean facialDetection;
}