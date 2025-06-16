package com.can.travelplanner.exception;

public class TravelPlannerException extends RuntimeException {
    public TravelPlannerException(String message) {
        super(message);
    }

    public TravelPlannerException(String message, Throwable cause) {
        super(message, cause);
    }
} 