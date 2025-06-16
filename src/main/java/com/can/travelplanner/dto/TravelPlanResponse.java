package com.can.travelplanner.dto;

import java.util.Map;
import lombok.Data;

@Data
public class TravelPlanResponse {
    private String destination;
    private Integer duration;
    private String plan;
    private LocationInfo locationInfo;
    private Map<String, Object> weatherInfo;

    public TravelPlanResponse() {}

    public TravelPlanResponse(String travelPlan, Map<String, Object> weatherInfo, LocationInfo locationInfo) {
        this.plan = travelPlan;
        this.weatherInfo = weatherInfo;
        this.locationInfo = locationInfo;
    }

    public String getTravelPlan() {
        return plan;
    }

    public void setTravelPlan(String travelPlan) {
        this.plan = travelPlan;
    }

    public Map<String, Object> getWeatherInfo() {
        return weatherInfo;
    }

    public void setWeatherInfo(Map<String, Object> weatherInfo) {
        this.weatherInfo = weatherInfo;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    public static class LocationInfo {
        private Double latitude;
        private Double longitude;

        public LocationInfo() {}

        public LocationInfo(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }
    }
} 