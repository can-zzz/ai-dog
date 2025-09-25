package com.can.happydog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求DTO
 */
public class ChatRequest {
    
    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000字符")
    private String message;
    
    private String sessionId;
    private String modelType;
    private Boolean saveHistory = true;
    private Boolean enableDeepThinking = false;

    public ChatRequest() {}

    public ChatRequest(String message, String sessionId, String modelType, Boolean saveHistory) {
        this.message = message;
        this.sessionId = sessionId;
        this.modelType = modelType;
        this.saveHistory = saveHistory;
    }

    public ChatRequest(String message, String sessionId, String modelType, Boolean saveHistory, Boolean enableDeepThinking) {
        this.message = message;
        this.sessionId = sessionId;
        this.modelType = modelType;
        this.saveHistory = saveHistory;
        this.enableDeepThinking = enableDeepThinking;
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

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public Boolean getSaveHistory() {
        return saveHistory;
    }

    public void setSaveHistory(Boolean saveHistory) {
        this.saveHistory = saveHistory;
    }

    public Boolean getEnableDeepThinking() {
        return enableDeepThinking;
    }

    public void setEnableDeepThinking(Boolean enableDeepThinking) {
        this.enableDeepThinking = enableDeepThinking;
    }
}