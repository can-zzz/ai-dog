package com.can.aiassistant.service;

import com.can.aiassistant.dto.ChatMessage;
import com.can.aiassistant.service.AgentExecutor.ExecutionContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存管理器
 * 负责智能的对话历史管理、上下文压缩、相关性评分和自动清理
 */
@Component
public class MemoryManager {
    
    private static final Logger log = LoggerFactory.getLogger(MemoryManager.class);
    
    // 对话历史存储
    private final Map<String, List<ChatMessage>> chatHistory = new ConcurrentHashMap<>();
    
    // 上下文相关性缓存
    private final Map<String, ContextRelevance> contextCache = new ConcurrentHashMap<>();
    
    // 配置参数
    private static final int MAX_HISTORY_SIZE = 50; // 最大历史记录数
    private static final int CONTEXT_WINDOW_SIZE = 10; // 上下文窗口大小
    private static final double RELEVANCE_THRESHOLD = 0.3; // 相关性阈值
    private static final long CONTEXT_TTL = 3600000; // 上下文缓存TTL (1小时)
    
    /**
     * 加载上下文
     */
    public void loadContext(String sessionId, ExecutionContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("🧠 加载上下文 - 会话: {}", sessionId);
            
            // 1. 获取对话历史
            List<ChatMessage> history = getSessionHistory(sessionId);
            
            // 2. 计算上下文相关性
            ContextRelevance relevance = calculateContextRelevance(sessionId, history, context);
            
            // 3. 压缩上下文
            List<ChatMessage> compressedContext = compressContext(history, relevance);
            
            // 4. 构建内存上下文对象
            MemoryContext memoryContext = new MemoryContext(
                sessionId,
                history,
                compressedContext,
                relevance,
                System.currentTimeMillis()
            );
            
            context.setMemoryContext(memoryContext);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 上下文加载完成 - 会话: {}, 历史记录: {}, 压缩后: {}, 耗时: {}ms", 
                sessionId, history.size(), compressedContext.size(), duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 上下文加载失败 - 会话: {}, 错误: {}, 耗时: {}ms", 
                sessionId, e.getMessage(), duration);
            
            // 创建空的内存上下文
            context.setMemoryContext(new MemoryContext(sessionId, new ArrayList<>(), 
                new ArrayList<>(), new ContextRelevance(), System.currentTimeMillis()));
        }
    }
    
    /**
     * 保存上下文
     */
    public void saveContext(String sessionId, ExecutionContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("💾 保存上下文 - 会话: {}", sessionId);
            
            MemoryContext memoryContext = (MemoryContext) context.getMemoryContext();
            if (memoryContext == null) {
                log.warn("⚠️ 内存上下文为空，跳过保存 - 会话: {}", sessionId);
                return;
            }
            
            // 1. 保存用户消息
            if (context.getProcessedRequest() != null) {
                ChatMessage userMessage = ChatMessage.userMessage(
                    context.getProcessedRequest().getRequest().getMessage(),
                    sessionId,
                    "用户"
                );
                addMessageToHistory(sessionId, userMessage);
            }
            
            // 2. 保存AI响应
            if (context.getGeneratedResponse() != null) {
                ChatMessage assistantMessage = ChatMessage.assistantMessage(
                    context.getGeneratedResponse().toString(),
                    sessionId
                );
                addMessageToHistory(sessionId, assistantMessage);
            }
            
            // 3. 更新上下文相关性缓存
            if (memoryContext.getRelevance() != null) {
                contextCache.put(sessionId, memoryContext.getRelevance());
            }
            
            // 4. 执行智能清理
            performIntelligentCleanup(sessionId);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 上下文保存完成 - 会话: {}, 耗时: {}ms", sessionId, duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 上下文保存失败 - 会话: {}, 错误: {}, 耗时: {}ms", 
                sessionId, e.getMessage(), duration);
        }
    }
    
    /**
     * 获取会话历史
     */
    private List<ChatMessage> getSessionHistory(String sessionId) {
        return chatHistory.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * 添加消息到历史记录
     */
    public void addMessageToHistory(String sessionId, ChatMessage message) {
        chatHistory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message);
        log.debug("📝 消息已添加到历史 - 会话: {}, 类型: {}", sessionId, message.getType());
    }
    
    /**
     * 计算上下文相关性
     */
    private ContextRelevance calculateContextRelevance(String sessionId, List<ChatMessage> history, ExecutionContext context) {
        ContextRelevance relevance = new ContextRelevance();
        
        if (history.isEmpty()) {
            return relevance;
        }
        
        String currentMessage = context.getProcessedRequest().getRequest().getMessage();
        
        // 计算每条历史消息与当前消息的相关性
        for (int i = 0; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            double score = calculateMessageRelevance(currentMessage, msg.getContent());
            
            // 时间衰减：越近的消息权重越高
            double timeDecay = Math.exp(-0.1 * (history.size() - i - 1));
            double finalScore = score * timeDecay;
            
            relevance.addScore(i, finalScore);
        }
        
        log.debug("🔍 上下文相关性计算完成 - 会话: {}, 平均相关性: {:.3f}", 
            sessionId, relevance.getAverageScore());
        
        return relevance;
    }
    
    /**
     * 计算消息相关性（简化实现）
     */
    private double calculateMessageRelevance(String currentMessage, String historyMessage) {
        if (currentMessage == null || historyMessage == null) {
            return 0.0;
        }
        
        // 简单的关键词匹配相关性计算
        String[] currentWords = currentMessage.toLowerCase().split("\\s+");
        String[] historyWords = historyMessage.toLowerCase().split("\\s+");
        
        Set<String> currentSet = new HashSet<>(Arrays.asList(currentWords));
        Set<String> historySet = new HashSet<>(Arrays.asList(historyWords));
        
        // 计算交集
        Set<String> intersection = new HashSet<>(currentSet);
        intersection.retainAll(historySet);
        
        // 计算并集
        Set<String> union = new HashSet<>(currentSet);
        union.addAll(historySet);
        
        // Jaccard相似度
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * 压缩上下文
     */
    private List<ChatMessage> compressContext(List<ChatMessage> history, ContextRelevance relevance) {
        if (history.size() <= CONTEXT_WINDOW_SIZE) {
            return new ArrayList<>(history);
        }
        
        // 选择最相关的消息
        List<Integer> relevantIndices = relevance.getTopRelevantIndices(CONTEXT_WINDOW_SIZE, RELEVANCE_THRESHOLD);
        
        // 确保包含最近的几条消息
        int recentCount = Math.min(3, history.size());
        for (int i = history.size() - recentCount; i < history.size(); i++) {
            if (!relevantIndices.contains(i)) {
                relevantIndices.add(i);
            }
        }
        
        // 按时间顺序排序
        relevantIndices.sort(Integer::compareTo);
        
        // 构建压缩后的上下文
        List<ChatMessage> compressedContext = relevantIndices.stream()
            .map(history::get)
            .collect(Collectors.toList());
        
        log.debug("🗜️ 上下文压缩完成 - 原始: {}, 压缩后: {}", history.size(), compressedContext.size());
        
        return compressedContext;
    }
    
    /**
     * 执行智能清理
     */
    private void performIntelligentCleanup(String sessionId) {
        List<ChatMessage> history = chatHistory.get(sessionId);
        if (history == null || history.size() <= MAX_HISTORY_SIZE) {
            return;
        }
        
        // 保留最近的消息和高相关性的消息
        ContextRelevance relevance = contextCache.get(sessionId);
        if (relevance != null) {
            List<Integer> keepIndices = relevance.getTopRelevantIndices(MAX_HISTORY_SIZE - 5, RELEVANCE_THRESHOLD);
            
            // 确保保留最近的5条消息
            for (int i = Math.max(0, history.size() - 5); i < history.size(); i++) {
                if (!keepIndices.contains(i)) {
                    keepIndices.add(i);
                }
            }
            
            keepIndices.sort(Integer::compareTo);
            
            List<ChatMessage> cleanedHistory = keepIndices.stream()
                .map(history::get)
                .collect(Collectors.toList());
            
            chatHistory.put(sessionId, cleanedHistory);
            
            log.info("🧹 智能清理完成 - 会话: {}, 清理前: {}, 清理后: {}", 
                sessionId, history.size(), cleanedHistory.size());
        }
    }
    
    /**
     * 清理过期的上下文缓存
     */
    public void cleanupExpiredContexts() {
        long currentTime = System.currentTimeMillis();
        contextCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getTimestamp() > CONTEXT_TTL);
        
        log.debug("🧹 清理过期上下文缓存，当前缓存数量: {}", contextCache.size());
    }
    
    /**
     * 获取会话统计信息
     */
    public SessionStats getSessionStats(String sessionId) {
        List<ChatMessage> history = getSessionHistory(sessionId);
        ContextRelevance relevance = contextCache.get(sessionId);
        
        return new SessionStats(
            sessionId,
            history.size(),
            relevance != null ? relevance.getAverageScore() : 0.0,
            System.currentTimeMillis()
        );
    }
    
    /**
     * 内存上下文类
     */
    public static class MemoryContext {
        private final String sessionId;
        private final List<ChatMessage> fullHistory;
        private final List<ChatMessage> compressedContext;
        private final ContextRelevance relevance;
        private final long timestamp;
        
        public MemoryContext(String sessionId, List<ChatMessage> fullHistory, 
                           List<ChatMessage> compressedContext, ContextRelevance relevance, long timestamp) {
            this.sessionId = sessionId;
            this.fullHistory = fullHistory;
            this.compressedContext = compressedContext;
            this.relevance = relevance;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public List<ChatMessage> getFullHistory() { return fullHistory; }
        public List<ChatMessage> getCompressedContext() { return compressedContext; }
        public ContextRelevance getRelevance() { return relevance; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 上下文相关性类
     */
    public static class ContextRelevance {
        private final Map<Integer, Double> scores = new HashMap<>();
        private long timestamp = System.currentTimeMillis();
        
        public void addScore(int index, double score) {
            scores.put(index, score);
        }
        
        public double getScore(int index) {
            return scores.getOrDefault(index, 0.0);
        }
        
        public double getAverageScore() {
            return scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        
        public List<Integer> getTopRelevantIndices(int count, double threshold) {
            return scores.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
        
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 会话统计信息类
     */
    public static class SessionStats {
        private final String sessionId;
        private final int messageCount;
        private final double averageRelevance;
        private final long timestamp;
        
        public SessionStats(String sessionId, int messageCount, double averageRelevance, long timestamp) {
            this.sessionId = sessionId;
            this.messageCount = messageCount;
            this.averageRelevance = averageRelevance;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public int getMessageCount() { return messageCount; }
        public double getAverageRelevance() { return averageRelevance; }
        public long getTimestamp() { return timestamp; }
    }
}
