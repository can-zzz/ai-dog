package com.can.travelplanner.dto;

import lombok.Data;

@Data
public class LocationInfo {
    private Double latitude;
    private Double longitude;
    private String displayName;
    private String type;
} 