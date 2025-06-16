package com.can.travelplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TravelPlanRequest {
    @NotBlank(message = "目的地不能为空")
    private String destination;
    
    @NotNull(message = "旅行天数不能为空")
    private Integer duration;
    
    private String preferences;

    public TravelPlanRequest() {}

    public TravelPlanRequest(String destination, Integer duration, String preferences) {
        this.destination = destination;
        this.duration = duration;
        this.preferences = preferences;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }
} 