package com.can.happydog.dto;

/**
 * 流式响应DTO
 */
public class StreamResponse {
    
    private String content;
    private boolean done;
    private String error;
    private ThinkingStep currentStep;

    public StreamResponse() {}

    public StreamResponse(String content, boolean done) {
        this.content = content;
        this.done = done;
    }

    public static StreamResponse chunk(String content) {
        return new StreamResponse(content, false);
    }

    public static StreamResponse done() {
        return new StreamResponse(null, true);
    }

    public static StreamResponse error(String error) {
        StreamResponse response = new StreamResponse();
        response.error = error;
        response.done = true;
        return response;
    }

    public static StreamResponse thinking(ThinkingStep step) {
        StreamResponse response = new StreamResponse();
        response.currentStep = step;
        response.done = false;
        return response;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public ThinkingStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(ThinkingStep currentStep) {
        this.currentStep = currentStep;
    }
}
