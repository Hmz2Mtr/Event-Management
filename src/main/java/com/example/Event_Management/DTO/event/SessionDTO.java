package com.example.Event_Management.DTO.event;

public class SessionDTO {
    private String name;
    private String date;
    private String startTime;
    private String endTime;
    private PlaceDTO place;
    private String guestSpeakers;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public PlaceDTO getPlace() {
        return place;
    }

    public void setPlace(PlaceDTO place) {
        this.place = place;
    }

    public String getGuestSpeakers() {
        return guestSpeakers;
    }

    public void setGuestSpeakers(String guestSpeakers) {
        this.guestSpeakers = guestSpeakers;
    }
}