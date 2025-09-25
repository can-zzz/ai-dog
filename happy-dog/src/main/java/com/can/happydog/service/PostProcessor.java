package com.can.happydog.service;

import com.can.happydog.dto.StreamResponse;
import com.can.happydog.dto.ThinkingStep;
import com.can.happydog.service.RequestPreprocessor.ProcessedRequest;
import com.can.happydog.service.AgentExecutor.ExecutionContext;
import com.can.happydog.config.PromptTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 后处理器
 * 负责追问生成、响应质量评估、日志记录和性能统计
 */
@Component
public class PostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(PostProcessor.class);
    
    // 性能统计缓存
    private final Map<String, PerformanceStats> performanceStats = new ConcurrentHashMap<>();
    
    // 质量评估缓存
    private final Map<String, QualityAssessment> qualityCache = new ConcurrentHashMap<>();
    
    // 提示词模板管理
    private final PromptTemplates promptTemplates;
    
    @Autowired
    public PostProcessor(PromptTemplates promptTemplates) {
        this.promptTemplates = promptTemplates;
    }
    
    /**
     * 执行后处理
     */
    public void postProcess(ProcessedRequest processedRequest, StreamResponseCallback callback, ExecutionContext context) {
        long postProcessStartTime = System.currentTimeMillis();
        String sessionId = processedRequest.getSessionId();
        
        try {
            log.info("🔄 后处理开始 - 会话: {}", sessionId);
            
            // 1. 响应质量评估
            QualityAssessment quality = assessResponseQuality(processedRequest, context);
            
            // 2. 生成追问建议
            List<String> followUpQuestions = generateFollowUpQuestions(processedRequest, context, quality);
            
            // 3. 记录性能统计
            PerformanceStats stats = recordPerformanceStats(processedRequest, context, postProcessStartTime);
            
            // 4. 发送追问建议
            sendFollowUpQuestions(followUpQuestions, callback);
            
            // 5. 发送完成信号
            callback.onResponse(StreamResponse.done());
            
            // 6. 记录详细日志
            logDetailedAnalysis(processedRequest, context, quality, stats);
            
            long postProcessDuration = System.currentTimeMillis() - postProcessStartTime;
            log.info("✅ 后处理完成 - 会话: {}, 质量评分: {:.2f}, 耗时: {}ms", 
                sessionId, quality.getOverallScore(), postProcessDuration);
            
        } catch (Exception e) {
            long postProcessDuration = System.currentTimeMillis() - postProcessStartTime;
            log.error("❌ 后处理失败 - 会话: {}, 错误: {}, 耗时: {}ms", 
                sessionId, e.getMessage(), postProcessDuration);
        }
    }
    
    /**
     * 评估响应质量
     */
    private QualityAssessment assessResponseQuality(ProcessedRequest processedRequest, ExecutionContext context) {
        String sessionId = processedRequest.getSessionId();
        log.debug("🔍 评估响应质量 - 会话: {}", sessionId);
        
        QualityAssessment assessment = new QualityAssessment();
        
        // 1. 响应完整性评估
        double completeness = assessCompleteness(processedRequest, context);
        assessment.setCompleteness(completeness);
        
        // 2. 响应相关性评估
        double relevance = assessRelevance(processedRequest, context);
        assessment.setRelevance(relevance);
        
        // 3. 响应清晰度评估
        double clarity = assessClarity(context);
        assessment.setClarity(clarity);
        
        // 4. 处理效率评估
        double efficiency = assessEfficiency(context);
        assessment.setEfficiency(efficiency);
        
        // 5. 计算总体评分
        double overallScore = (completeness * 0.3 + relevance * 0.3 + clarity * 0.2 + efficiency * 0.2);
        assessment.setOverallScore(overallScore);
        
        // 6. 缓存评估结果
        qualityCache.put(sessionId, assessment);
        
        log.debug("📊 质量评估完成 - 会话: {}, 总分: {:.2f}", sessionId, overallScore);
        return assessment;
    }
    
    /**
     * 评估响应完整性
     */
    private double assessCompleteness(ProcessedRequest processedRequest, ExecutionContext context) {
        if (context.getGeneratedResponse() == null) {
            return 0.0;
        }
        
        String response = context.getGeneratedResponse().toString();
        String question = processedRequest.getRequest().getMessage();
        
        // 简单的完整性评估逻辑
        double score = 0.5; // 基础分
        
        // 响应长度合理性
        if (response.length() > 50) score += 0.2;
        if (response.length() > 200) score += 0.1;
        
        // 是否包含关键信息
        if (containsKeywords(response, question)) score += 0.2;
        
        return Math.min(1.0, score);
    }
    
    /**
     * 评估响应相关性
     */
    private double assessRelevance(ProcessedRequest processedRequest, ExecutionContext context) {
        if (context.getGeneratedResponse() == null) {
            return 0.0;
        }
        
        String response = context.getGeneratedResponse().toString();
        String question = processedRequest.getRequest().getMessage();
        
        // 简单的相关性评估
        double score = 0.3; // 基础分
        
        // 关键词匹配
        if (containsKeywords(response, question)) score += 0.4;
        
        // 话题连贯性
        if (isTopicCoherent(response, question)) score += 0.3;
        
        return Math.min(1.0, score);
    }
    
    /**
     * 评估响应清晰度
     */
    private double assessClarity(ExecutionContext context) {
        if (context.getGeneratedResponse() == null) {
            return 0.0;
        }
        
        String response = context.getGeneratedResponse().toString();
        
        // 简单的清晰度评估
        double score = 0.4; // 基础分
        
        // 结构化程度
        if (isWellStructured(response)) score += 0.3;
        
        // 语言表达
        if (isClearExpression(response)) score += 0.3;
        
        return Math.min(1.0, score);
    }
    
    /**
     * 评估处理效率
     */
    private double assessEfficiency(ExecutionContext context) {
        if (context.getProcessedRequest() == null) {
            return 0.5;
        }
        
        long preprocessingTime = context.getProcessedRequest().getPreprocessingTime();
        
        // 基于处理时间评估效率
        if (preprocessingTime < 100) return 1.0;
        if (preprocessingTime < 500) return 0.8;
        if (preprocessingTime < 1000) return 0.6;
        if (preprocessingTime < 2000) return 0.4;
        return 0.2;
    }
    
    /**
     * 生成追问建议
     */
    private List<String> generateFollowUpQuestions(ProcessedRequest processedRequest, ExecutionContext context, QualityAssessment quality) {
        String sessionId = processedRequest.getSessionId();
        log.debug("🤔 生成追问建议 - 会话: {}", sessionId);
        
        List<String> followUps = new ArrayList<>();
        String userMessage = processedRequest.getRequest().getMessage();
        
        // 根据用户问题类型生成追问
        if (isHowToQuestion(userMessage)) {
            followUps.add("您想了解更多实施细节吗？");
            followUps.add("有什么具体的困难需要解决吗？");
        } else if (isWhatQuestion(userMessage)) {
            followUps.add("您想深入了解相关的应用场景吗？");
            followUps.add("还有其他相关概念需要解释吗？");
        } else if (isWhyQuestion(userMessage)) {
            followUps.add("您想了解更多背景信息吗？");
            followUps.add("有什么具体的例子可以帮助理解吗？");
        } else {
            followUps.add("还有什么相关问题吗？");
            followUps.add("需要我详细解释某个方面吗？");
        }
        
        // 根据质量评估调整追问
        if (quality.getOverallScore() < 0.7) {
            followUps.add("我的回答是否完全解决了您的问题？");
        }
        
        // 基于处理路由添加特定追问
        switch (processedRequest.getRoute()) {
            case DEEP_THINKING_SIMPLE:
            case DEEP_THINKING_WITH_TOOLS:
                followUps.add("您希望我从其他角度分析这个问题吗？");
                break;
            case SIMPLE_CHAT:
                followUps.add("还有什么可以帮助您的吗？");
                break;
        }
        
        log.debug("💡 生成了 {} 个追问建议 - 会话: {}", followUps.size(), sessionId);
        return followUps;
    }
    
    /**
     * 记录性能统计
     */
    private PerformanceStats recordPerformanceStats(ProcessedRequest processedRequest, ExecutionContext context, long postProcessStartTime) {
        String sessionId = processedRequest.getSessionId();
        
        PerformanceStats stats = new PerformanceStats();
        stats.setSessionId(sessionId);
        stats.setProcessingRoute(processedRequest.getRoute().toString());
        stats.setPreprocessingTime(processedRequest.getPreprocessingTime());
        stats.setTotalTokens(context.getTotalTokens());
        stats.setTotalCost(context.getTotalCost());
        stats.setTimestamp(System.currentTimeMillis());
        
        // 计算各阶段耗时
        long totalTime = System.currentTimeMillis() - postProcessStartTime;
        stats.setTotalProcessingTime(totalTime);
        
        // 缓存统计信息
        performanceStats.put(sessionId, stats);
        
        log.debug("📈 性能统计记录完成 - 会话: {}", sessionId);
        return stats;
    }
    
    /**
     * 发送追问建议
     */
    private void sendFollowUpQuestions(List<String> followUpQuestions, StreamResponseCallback callback) {
        if (followUpQuestions.isEmpty()) {
            return;
        }
        
        // 发送追问建议
        callback.onResponse(StreamResponse.thinking(
            ThinkingStep.analyze("追问建议", 
                "以下是一些您可能感兴趣的追问：\n" + String.join("\n", followUpQuestions))
        ));
    }
    
    /**
     * 记录详细分析日志
     */
    private void logDetailedAnalysis(ProcessedRequest processedRequest, ExecutionContext context, 
                                   QualityAssessment quality, PerformanceStats stats) {
        String sessionId = processedRequest.getSessionId();
        
        log.info("📋 === 详细分析报告 - 会话: {} ===", sessionId);
        log.info("🎯 处理路由: {}", processedRequest.getRoute());
        log.info("📊 质量评估: 总分={:.2f}, 完整性={:.2f}, 相关性={:.2f}, 清晰度={:.2f}, 效率={:.2f}", 
            quality.getOverallScore(), quality.getCompleteness(), quality.getRelevance(), 
            quality.getClarity(), quality.getEfficiency());
        log.info("⏱️ 性能统计: 预处理={}ms, 总处理={}ms", 
            stats.getPreprocessingTime(), stats.getTotalProcessingTime());
        log.info("💰 资源消耗: Tokens={}, 成本={}", stats.getTotalTokens(), stats.getTotalCost());
        log.info("📋 === 分析报告结束 ===");
    }
    
    // 辅助方法
    private boolean containsKeywords(String response, String question) {
        String[] questionWords = question.toLowerCase().split("\\s+");
        String responseLower = response.toLowerCase();
        
        int matchCount = 0;
        for (String word : questionWords) {
            if (responseLower.contains(word)) {
                matchCount++;
            }
        }
        
        return matchCount >= Math.min(questionWords.length / 2, 3);
    }
    
    private boolean isTopicCoherent(String response, String question) {
        // 简单的话题连贯性检查
        return response.length() > 50 && containsKeywords(response, question);
    }
    
    private boolean isWellStructured(String response) {
        return response.contains("。") || response.contains("\n") || response.contains("：");
    }
    
    private boolean isClearExpression(String response) {
        return !response.contains("...") && response.length() > 20;
    }
    
    private boolean isHowToQuestion(String message) {
        return message.contains("如何") || message.contains("怎么") || message.contains("怎样");
    }
    
    private boolean isWhatQuestion(String message) {
        return message.contains("什么是") || message.contains("什么") || message.contains("是什么");
    }
    
    private boolean isWhyQuestion(String message) {
        return message.contains("为什么") || message.contains("为何") || message.contains("原因");
    }
    
    /**
     * 获取会话统计信息
     */
    public PerformanceStats getSessionStats(String sessionId) {
        return performanceStats.get(sessionId);
    }
    
    /**
     * 获取质量评估结果
     */
    public QualityAssessment getQualityAssessment(String sessionId) {
        return qualityCache.get(sessionId);
    }
    
    /**
     * 清理过期数据
     */
    public void cleanupExpiredData() {
        long currentTime = System.currentTimeMillis();
        long ttl = 24 * 60 * 60 * 1000; // 24小时
        
        performanceStats.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getTimestamp() > ttl);
            
        qualityCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getTimestamp() > ttl);
            
        log.debug("🧹 清理过期数据完成");
    }
    
    /**
     * 质量评估类
     */
    public static class QualityAssessment {
        private double completeness;
        private double relevance;
        private double clarity;
        private double efficiency;
        private double overallScore;
        private long timestamp = System.currentTimeMillis();
        
        // Getters and Setters
        public double getCompleteness() { return completeness; }
        public void setCompleteness(double completeness) { this.completeness = completeness; }
        
        public double getRelevance() { return relevance; }
        public void setRelevance(double relevance) { this.relevance = relevance; }
        
        public double getClarity() { return clarity; }
        public void setClarity(double clarity) { this.clarity = clarity; }
        
        public double getEfficiency() { return efficiency; }
        public void setEfficiency(double efficiency) { this.efficiency = efficiency; }
        
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
        
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 性能统计类
     */
    public static class PerformanceStats {
        private String sessionId;
        private String processingRoute;
        private long preprocessingTime;
        private long totalProcessingTime;
        private long totalTokens;
        private long totalCost;
        private long timestamp;
        
        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getProcessingRoute() { return processingRoute; }
        public void setProcessingRoute(String processingRoute) { this.processingRoute = processingRoute; }
        
        public long getPreprocessingTime() { return preprocessingTime; }
        public void setPreprocessingTime(long preprocessingTime) { this.preprocessingTime = preprocessingTime; }
        
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public void setTotalProcessingTime(long totalProcessingTime) { this.totalProcessingTime = totalProcessingTime; }
        
        public long getTotalTokens() { return totalTokens; }
        public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }
        
        public long getTotalCost() { return totalCost; }
        public void setTotalCost(long totalCost) { this.totalCost = totalCost; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
