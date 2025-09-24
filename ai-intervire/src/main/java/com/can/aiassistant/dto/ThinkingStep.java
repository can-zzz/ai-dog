package com.can.aiassistant.dto;

import java.time.LocalDateTime;

/**
 * 思考步骤DTO
 */
public class ThinkingStep {
    
    public enum StepType {
        ANALYZE,        // 分析问题
        RESEARCH,       // 搜索相关信息
        REASON,         // 推理思考
        SYNTHESIZE,     // 综合整理
        VALIDATE        // 验证答案
    }
    
    private StepType type;
    private String title;
    private String content;
    private LocalDateTime timestamp;
    private Long duration; // 耗时（毫秒）

    public ThinkingStep() {}

    public ThinkingStep(StepType type, String title, String content) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public static ThinkingStep analyze(String title, String content) {
        return new ThinkingStep(StepType.ANALYZE, title, content);
    }

    public static ThinkingStep research(String title, String content) {
        return new ThinkingStep(StepType.RESEARCH, title, content);
    }

    public static ThinkingStep reason(String title, String content) {
        return new ThinkingStep(StepType.REASON, title, content);
    }

    public static ThinkingStep synthesize(String title, String content) {
        return new ThinkingStep(StepType.SYNTHESIZE, title, content);
    }

    public static ThinkingStep validate(String title, String content) {
        return new ThinkingStep(StepType.VALIDATE, title, content);
    }

    public StepType getType() {
        return type;
    }

    public void setType(StepType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
