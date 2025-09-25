package com.can.happydog.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天响应DTO
 */
public class ChatResponse {
    
    private String message;
    private String sessionId;
    private LocalDateTime timestamp;
    private String model;
    private Long processingTime;
    private Boolean success;
    private String error;
    private List<ThinkingStep> thinkingSteps;

    public ChatResponse() {}

    public ChatResponse(String message, String sessionId, String model, Long processingTime, Boolean success) {
        this.message = message;
        this.sessionId = sessionId;
        this.model = model;
        this.processingTime = processingTime;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public static ChatResponse success(String message, String sessionId, String model, long processingTime) {
        return new ChatResponse(message, sessionId, model, processingTime, true);
    }
    
    public static ChatResponse successWithThinking(String message, String sessionId, String model, long processingTime, List<ThinkingStep> thinkingSteps) {
        ChatResponse response = new ChatResponse(message, sessionId, model, processingTime, true);
        response.thinkingSteps = thinkingSteps;
        return response;
    }
    
    public static ChatResponse error(String error, String sessionId) {
        ChatResponse response = new ChatResponse();
        response.error = error;
        response.sessionId = sessionId;
        response.timestamp = LocalDateTime.now();
        response.success = false;
        return response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<ThinkingStep> getThinkingSteps() {
        return thinkingSteps;
    }

    public void setThinkingSteps(List<ThinkingStep> thinkingSteps) {
        this.thinkingSteps = thinkingSteps;
    }
}