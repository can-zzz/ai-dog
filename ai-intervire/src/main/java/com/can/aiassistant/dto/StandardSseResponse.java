package com.can.aiassistant.dto;

import java.util.UUID;

/**
 * 标准SSE响应格式
 * 支持标准的 event: + data: 格式
 */
public class StandardSseResponse {
    
    public enum EventType {
        OPENED("opened"),
        ONLINE_SEARCH("onlineSearch"), 
        MESSAGE("message"),
        PING("ping"),
        FINISHED("finished");
        
        private final String value;
        
        EventType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    private EventType eventType;
    private Object data;
    
    public StandardSseResponse() {}
    
    public StandardSseResponse(EventType eventType, Object data) {
        this.eventType = eventType;
        this.data = data;
    }
    
    // 静态工厂方法
    public static StandardSseResponse opened(String sseId, String conversationId) {
        OpenedData openedData = new OpenedData();
        openedData.setSseId(sseId);
        openedData.setConversationId(conversationId);
        openedData.setTimestamp(String.valueOf(System.currentTimeMillis()));
        openedData.setTurnId(UUID.randomUUID().toString());
        openedData.setQuerySentenceId(UUID.randomUUID().toString());
        openedData.setAnswerSentenceId(UUID.randomUUID().toString());
        openedData.setEventIndex(0);
        openedData.setQueryTime(java.time.Instant.now().toString());
        
        return new StandardSseResponse(EventType.OPENED, openedData);
    }
    
    public static StandardSseResponse onlineSearch(int eventIndex, String stage, String title, String content) {
        OnlineSearchData searchData = new OnlineSearchData();
        searchData.setEventIndex(eventIndex);
        
        OnlineSearchData.Details details = new OnlineSearchData.Details();
        details.setStage(stage);
        details.setTitle(title);
        details.setContent(content);
        details.setIcon("");
        details.setUrl("");
        details.setToolName(null);
        
        OnlineSearchData.Content searchContent = new OnlineSearchData.Content();
        searchContent.setDetails(new OnlineSearchData.Details[]{details});
        searchData.setContent(searchContent);
        
        return new StandardSseResponse(EventType.ONLINE_SEARCH, searchData);
    }
    
    public static StandardSseResponse message(String sseId, int eventIndex, String content) {
        MessageData messageData = new MessageData();
        messageData.setSseId(sseId);
        messageData.setEventIndex(eventIndex);
        messageData.setContent(content);
        messageData.setTimestamp(String.valueOf(System.currentTimeMillis()));
        
        return new StandardSseResponse(EventType.MESSAGE, messageData);
    }
    
    public static StandardSseResponse ping() {
        return new StandardSseResponse(EventType.PING, null);
    }
    
    public static StandardSseResponse finished(String sseId, int eventIndex) {
        MessageData finishedData = new MessageData();
        finishedData.setSseId(sseId);
        finishedData.setEventIndex(eventIndex);
        finishedData.setContent("");
        finishedData.setTimestamp(String.valueOf(System.currentTimeMillis()));
        
        return new StandardSseResponse(EventType.FINISHED, finishedData);
    }
    
    // Getters and Setters
    public EventType getEventType() {
        return eventType;
    }
    
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    // 内部数据类
    public static class OpenedData {
        private String sseId;
        private String conversationId;
        private String answerSentenceId;
        private int eventIndex;
        private String turnId;
        private String timestamp;
        private String querySentenceId;
        private String queryTime;
        
        // Getters and Setters
        public String getSseId() { return sseId; }
        public void setSseId(String sseId) { this.sseId = sseId; }
        
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        
        public String getAnswerSentenceId() { return answerSentenceId; }
        public void setAnswerSentenceId(String answerSentenceId) { this.answerSentenceId = answerSentenceId; }
        
        public int getEventIndex() { return eventIndex; }
        public void setEventIndex(int eventIndex) { this.eventIndex = eventIndex; }
        
        public String getTurnId() { return turnId; }
        public void setTurnId(String turnId) { this.turnId = turnId; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getQuerySentenceId() { return querySentenceId; }
        public void setQuerySentenceId(String querySentenceId) { this.querySentenceId = querySentenceId; }
        
        public String getQueryTime() { return queryTime; }
        public void setQueryTime(String queryTime) { this.queryTime = queryTime; }
    }
    
    public static class OnlineSearchData {
        private int eventIndex;
        private Content content;
        
        public int getEventIndex() { return eventIndex; }
        public void setEventIndex(int eventIndex) { this.eventIndex = eventIndex; }
        
        public Content getContent() { return content; }
        public void setContent(Content content) { this.content = content; }
        
        public static class Content {
            private Details[] details;
            
            public Details[] getDetails() { return details; }
            public void setDetails(Details[] details) { this.details = details; }
        }
        
        public static class Details {
            private String stage;
            private String title;
            private String content;
            private String icon;
            private String url;
            private String toolName;
            
            public String getStage() { return stage; }
            public void setStage(String stage) { this.stage = stage; }
            
            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            
            public String getContent() { return content; }
            public void setContent(String content) { this.content = content; }
            
            public String getIcon() { return icon; }
            public void setIcon(String icon) { this.icon = icon; }
            
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            
            public String getToolName() { return toolName; }
            public void setToolName(String toolName) { this.toolName = toolName; }
        }
    }
    
    public static class MessageData {
        private String sseId;
        private int eventIndex;
        private String content;
        private String timestamp;
        
        public String getSseId() { return sseId; }
        public void setSseId(String sseId) { this.sseId = sseId; }
        
        public int getEventIndex() { return eventIndex; }
        public void setEventIndex(int eventIndex) { this.eventIndex = eventIndex; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
