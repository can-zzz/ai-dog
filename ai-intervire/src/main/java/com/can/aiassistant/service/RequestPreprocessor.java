package com.can.aiassistant.service;

import com.can.aiassistant.dto.ChatRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 请求预处理器
 * 负责请求验证、标准化、缓存检查等预处理工作
 */
@Component
public class RequestPreprocessor {
    
    private static final Logger log = LoggerFactory.getLogger(RequestPreprocessor.class);
    
    // 请求缓存，用于去重和快速响应
    private final Map<String, Object> requestCache = new ConcurrentHashMap<>();
    
    /**
     * 预处理聊天请求
     */
    public ProcessedRequest preprocess(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("🔄 开始预处理请求");
            
            // 1. 请求验证
            validateRequest(request);
            
            // 2. 参数标准化
            ChatRequest normalizedRequest = normalizeRequest(request);
            
            // 3. 生成或验证会话ID
            String sessionId = ensureSessionId(normalizedRequest);
            
            // 4. 缓存检查
            CacheResult cacheResult = checkCache(normalizedRequest, sessionId);
            
            // 5. 路由决策
            ProcessingRoute route = determineRoute(normalizedRequest);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("✅ 请求预处理完成 - 会话: {}, 路由: {}, 耗时: {}ms", 
                sessionId, route, processingTime);
            
            return new ProcessedRequest(
                normalizedRequest, 
                sessionId, 
                route, 
                cacheResult, 
                processingTime
            );
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("❌ 请求预处理失败 - 错误: {}, 耗时: {}ms", e.getMessage(), processingTime);
            throw new RuntimeException("请求预处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证请求参数
     */
    private void validateRequest(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        
        if (!StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        
        if (request.getMessage().length() > 10000) {
            throw new IllegalArgumentException("消息内容过长，最大支持10000字符");
        }
        
        log.debug("✅ 请求验证通过");
    }
    
    /**
     * 标准化请求参数
     */
    private ChatRequest normalizeRequest(ChatRequest request) {
        // 创建标准化的请求副本
        ChatRequest normalized = new ChatRequest();
        
        // 清理和标准化消息内容
        String message = request.getMessage().trim();
        normalized.setMessage(message);
        
        // 标准化会话ID
        normalized.setSessionId(request.getSessionId());
        
        // 标准化布尔参数
        normalized.setSaveHistory(request.getSaveHistory() != null ? request.getSaveHistory() : true);
        normalized.setEnableDeepThinking(request.getEnableDeepThinking() != null ? request.getEnableDeepThinking() : false);
        
        log.debug("✅ 请求参数标准化完成");
        return normalized;
    }
    
    /**
     * 确保会话ID存在
     */
    private String ensureSessionId(ChatRequest request) {
        String sessionId = request.getSessionId();
        
        if (!StringUtils.hasText(sessionId)) {
            sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
            request.setSessionId(sessionId);
            log.debug("🆔 生成新会话ID: {}", sessionId);
        } else {
            log.debug("🆔 使用现有会话ID: {}", sessionId);
        }
        
        return sessionId;
    }
    
    /**
     * 检查缓存
     */
    private CacheResult checkCache(ChatRequest request, String sessionId) {
        // 生成缓存键
        String cacheKey = generateCacheKey(request);
        
        // 检查是否有缓存结果
        Object cachedResult = requestCache.get(cacheKey);
        
        if (cachedResult != null) {
            log.info("💾 缓存命中 - 会话: {}, 键: {}", sessionId, cacheKey);
            return new CacheResult(true, cachedResult, cacheKey);
        } else {
            log.debug("🔍 缓存未命中 - 会话: {}, 键: {}", sessionId, cacheKey);
            return new CacheResult(false, null, cacheKey);
        }
    }
    
    /**
     * 生成缓存键
     */
    private String generateCacheKey(ChatRequest request) {
        // 基于消息内容和关键参数生成缓存键
        return String.format("req_%s_%s_%s", 
            request.getMessage().hashCode(),
            request.getEnableDeepThinking(),
            request.getSaveHistory()
        );
    }
    
    /**
     * 确定处理路由
     */
    private ProcessingRoute determineRoute(ChatRequest request) {
        // 根据请求特征决定处理路由
        if (request.getEnableDeepThinking()) {
            if (isComplexQuery(request.getMessage())) {
                return ProcessingRoute.DEEP_THINKING_WITH_TOOLS;
            } else {
                return ProcessingRoute.DEEP_THINKING_SIMPLE;
            }
        } else {
            if (isSimpleQuery(request.getMessage())) {
                return ProcessingRoute.SIMPLE_CHAT;
            } else {
                return ProcessingRoute.STANDARD_CHAT;
            }
        }
    }
    
    /**
     * 判断是否为复杂查询
     */
    private boolean isComplexQuery(String message) {
        // 简单的复杂度判断逻辑
        return message.length() > 100 || 
               message.contains("分析") || 
               message.contains("比较") || 
               message.contains("解释") ||
               message.contains("如何") ||
               message.contains("为什么");
    }
    
    /**
     * 判断是否为简单查询
     */
    private boolean isSimpleQuery(String message) {
        return message.length() < 20 && 
               (message.contains("你好") || 
                message.contains("谢谢") || 
                message.contains("再见"));
    }
    
    /**
     * 缓存结果
     */
    public void cacheResult(String cacheKey, Object result) {
        if (cacheKey != null && result != null) {
            requestCache.put(cacheKey, result);
            log.debug("💾 结果已缓存 - 键: {}", cacheKey);
        }
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupCache() {
        // 简单的缓存清理逻辑
        if (requestCache.size() > 1000) {
            requestCache.clear();
            log.info("🧹 缓存已清理");
        }
    }
    
    /**
     * 处理后的请求对象
     */
    public static class ProcessedRequest {
        private final ChatRequest request;
        private final String sessionId;
        private final ProcessingRoute route;
        private final CacheResult cacheResult;
        private final long preprocessingTime;
        
        public ProcessedRequest(ChatRequest request, String sessionId, ProcessingRoute route, 
                              CacheResult cacheResult, long preprocessingTime) {
            this.request = request;
            this.sessionId = sessionId;
            this.route = route;
            this.cacheResult = cacheResult;
            this.preprocessingTime = preprocessingTime;
        }
        
        // Getters
        public ChatRequest getRequest() { return request; }
        public String getSessionId() { return sessionId; }
        public ProcessingRoute getRoute() { return route; }
        public CacheResult getCacheResult() { return cacheResult; }
        public long getPreprocessingTime() { return preprocessingTime; }
    }
    
    /**
     * 缓存结果对象
     */
    public static class CacheResult {
        private final boolean hit;
        private final Object data;
        private final String key;
        
        public CacheResult(boolean hit, Object data, String key) {
            this.hit = hit;
            this.data = data;
            this.key = key;
        }
        
        // Getters
        public boolean isHit() { return hit; }
        public Object getData() { return data; }
        public String getKey() { return key; }
    }
    
    /**
     * 处理路由枚举
     */
    public enum ProcessingRoute {
        SIMPLE_CHAT("简单对话"),
        STANDARD_CHAT("标准对话"),
        DEEP_THINKING_SIMPLE("简单深度思考"),
        DEEP_THINKING_WITH_TOOLS("复杂深度思考+工具");
        
        private final String description;
        
        ProcessingRoute(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
}
