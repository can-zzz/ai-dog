package com.can.happydog.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 聊天消息DTO
 */
public class ChatMessage {
    
    public enum MessageType {
        USER, ASSISTANT, SYSTEM
    }
    
    private String content;
    private MessageType type;
    private String sender;
    private String sessionId;
    private LocalDateTime timestamp;
    private String messageId;

    public ChatMessage() {}

    public ChatMessage(String content, MessageType type, String sender, String sessionId) {
        this.content = content;
        this.type = type;
        this.sender = sender;
        this.sessionId = sessionId;
        this.timestamp = LocalDateTime.now();
        this.messageId = UUID.randomUUID().toString();
    }

    public static ChatMessage userMessage(String content, String sessionId, String sender) {
        return new ChatMessage(content, MessageType.USER, sender, sessionId);
    }
    
    public static ChatMessage assistantMessage(String content, String sessionId) {
        return new ChatMessage(content, MessageType.ASSISTANT, "快乐小狗", sessionId);
    }
    
    public static ChatMessage systemMessage(String content, String sessionId) {
        return new ChatMessage(content, MessageType.SYSTEM, "系统", sessionId);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
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

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}