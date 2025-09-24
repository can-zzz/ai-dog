package com.can.aiassistant.service;

import com.can.aiassistant.dto.StreamResponse;
import com.can.aiassistant.dto.ThinkingStep;
import com.can.aiassistant.service.RequestPreprocessor.ProcessedRequest;
import com.can.aiassistant.service.AgentExecutor.ExecutionContext;
import com.can.aiassistant.config.PromptTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 思考执行器
 * 负责深度思考的缓存检查、路由决策和思考模型调用
 */
@Component
public class ThinkingExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(ThinkingExecutor.class);
    
    // 思考结果缓存
    private final Map<String, ThinkingResult> thinkingCache = new ConcurrentHashMap<>();
    
    // 原有的AiService，用于实际的思考调用
    private final AiService aiService;
    
    // 提示词模板管理
    private final PromptTemplates promptTemplates;
    
    @Value("${ai.deep-thinking.cache-enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${ai.deep-thinking.cache-ttl:3600000}") // 1小时
    private long cacheTTL;
    
    @Autowired
    public ThinkingExecutor(AiService aiService, PromptTemplates promptTemplates) {
        this.aiService = aiService;
        this.promptTemplates = promptTemplates;
    }
    
    /**
     * 执行思考流程
     */
    public void executeThinking(ProcessedRequest processedRequest, StreamResponseCallback callback, ExecutionContext context) {
        long thinkingStartTime = System.currentTimeMillis();
        String sessionId = processedRequest.getSessionId();
        String message = processedRequest.getRequest().getMessage();
        
        try {
            log.info("🧠 思考执行器开始 - 会话: {}", sessionId);
            
            // 1. 思考缓存检查
            ThinkingResult cachedThinking = checkThinkingCache(message, sessionId);
            if (cachedThinking != null) {
                handleCachedThinking(cachedThinking, callback, context);
                return;
            }
            
            // 2. 思考路由决策
            ThinkingStrategy strategy = determineThinkingStrategy(processedRequest);
            log.info("🎯 思考策略: {} - 会话: {}", strategy, sessionId);
            
            // 3. 执行思考
            ThinkingResult thinkingResult = executeThinkingWithStrategy(processedRequest, strategy, callback);
            
            // 4. 缓存思考结果
            cacheThinkingResult(message, thinkingResult);
            
            // 5. 更新执行上下文
            context.setThinkingResult(thinkingResult);
            
            long thinkingDuration = System.currentTimeMillis() - thinkingStartTime;
            log.info("✅ 思考执行完成 - 会话: {}, 策略: {}, 耗时: {}ms", 
                sessionId, strategy, thinkingDuration);
            
        } catch (Exception e) {
            long thinkingDuration = System.currentTimeMillis() - thinkingStartTime;
            log.error("❌ 思考执行失败 - 会话: {}, 错误: {}, 耗时: {}ms", 
                sessionId, e.getMessage(), thinkingDuration);
            
            // 发送错误的思考步骤
            callback.onResponse(StreamResponse.thinking(
                ThinkingStep.analyze("思考过程", "思考过程中遇到问题，正在尝试其他方式...")
            ));
            
            throw e;
        }
    }
    
    /**
     * 检查思考缓存
     */
    private ThinkingResult checkThinkingCache(String message, String sessionId) {
        if (!cacheEnabled) {
            return null;
        }
        
        String cacheKey = generateThinkingCacheKey(message);
        ThinkingResult cachedResult = thinkingCache.get(cacheKey);
        
        if (cachedResult != null && !isExpired(cachedResult)) {
            log.info("💾 思考缓存命中 - 会话: {}, 键: {}", sessionId, cacheKey);
            return cachedResult;
        } else if (cachedResult != null && isExpired(cachedResult)) {
            // 清理过期缓存
            thinkingCache.remove(cacheKey);
            log.debug("🧹 清理过期思考缓存 - 键: {}", cacheKey);
        }
        
        log.debug("🔍 思考缓存未命中 - 会话: {}, 键: {}", sessionId, cacheKey);
        return null;
    }
    
    /**
     * 处理缓存的思考结果
     */
    private void handleCachedThinking(ThinkingResult cachedThinking, StreamResponseCallback callback, ExecutionContext context) {
        log.info("⚡ 使用缓存的思考结果");
        
        // 发送缓存提示
        callback.onResponse(StreamResponse.thinking(
            ThinkingStep.analyze("智能缓存", "发现相似问题的思考结果，正在快速加载...")
        ));
        
        // 快速播放思考步骤
        for (ThinkingStep step : cachedThinking.getSteps()) {
            callback.onResponse(StreamResponse.thinking(step));
            try {
                Thread.sleep(50); // 快速播放
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        context.setThinkingResult(cachedThinking);
    }
    
    /**
     * 确定思考策略
     */
    private ThinkingStrategy determineThinkingStrategy(ProcessedRequest processedRequest) {
        String message = processedRequest.getRequest().getMessage();
        
        // 基于消息特征确定思考策略
        if (isFactualQuery(message)) {
            return ThinkingStrategy.FACTUAL_ANALYSIS;
        } else if (isCreativeQuery(message)) {
            return ThinkingStrategy.CREATIVE_THINKING;
        } else if (isProblemSolvingQuery(message)) {
            return ThinkingStrategy.PROBLEM_SOLVING;
        } else if (isComparisonQuery(message)) {
            return ThinkingStrategy.COMPARATIVE_ANALYSIS;
        } else {
            return ThinkingStrategy.GENERAL_THINKING;
        }
    }
    
    /**
     * 根据策略执行思考
     */
    private ThinkingResult executeThinkingWithStrategy(ProcessedRequest processedRequest, 
                                                     ThinkingStrategy strategy, 
                                                     StreamResponseCallback callback) {
        String sessionId = processedRequest.getSessionId();
        String message = processedRequest.getRequest().getMessage();
        
        // 发送策略通知
        callback.onResponse(StreamResponse.thinking(
            ThinkingStep.analyze("思考策略", "采用" + strategy.getDescription() + "进行深度分析")
        ));
        
        // 获取对应的思考提示词
        String thinkingPrompt = promptTemplates.getThinkingStrategyPrompt(strategy.name());
        
        // 调用原有的思考流程，但增加策略指导
        List<ThinkingStep> steps = new ArrayList<>();
        
        // 使用原有的AiService进行思考，但这里我们需要重构调用方式
        // 暂时使用简化的实现
        try {
            // 这里应该调用优化后的思考流程
            aiService.streamThinkingSteps(message, sessionId, callback);
            
            // 创建思考结果（简化实现）
            steps.add(ThinkingStep.analyze("策略执行", "使用" + strategy.getDescription() + "完成分析"));
            
        } catch (Exception e) {
            log.error("思考策略执行失败: {}", e.getMessage());
            steps.add(ThinkingStep.analyze("思考过程", "正在分析您的问题..."));
        }
        
        return new ThinkingResult(steps, strategy, System.currentTimeMillis());
    }
    
    /**
     * 缓存思考结果
     */
    private void cacheThinkingResult(String message, ThinkingResult result) {
        if (!cacheEnabled) {
            return;
        }
        
        String cacheKey = generateThinkingCacheKey(message);
        thinkingCache.put(cacheKey, result);
        
        log.debug("💾 思考结果已缓存 - 键: {}, 步骤数: {}", cacheKey, result.getSteps().size());
        
        // 清理过期缓存
        cleanupExpiredCache();
    }
    
    /**
     * 生成思考缓存键
     */
    private String generateThinkingCacheKey(String message) {
        // 基于消息内容生成缓存键，考虑语义相似性
        return "thinking_" + message.hashCode();
    }
    
    /**
     * 检查缓存是否过期
     */
    private boolean isExpired(ThinkingResult result) {
        return System.currentTimeMillis() - result.getTimestamp() > cacheTTL;
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanupExpiredCache() {
        if (thinkingCache.size() > 100) { // 当缓存过多时进行清理
            thinkingCache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
            log.debug("🧹 清理过期思考缓存，当前缓存数量: {}", thinkingCache.size());
        }
    }
    
    // 查询类型判断方法
    private boolean isFactualQuery(String message) {
        return message.contains("什么是") || message.contains("定义") || message.contains("介绍");
    }
    
    private boolean isCreativeQuery(String message) {
        return message.contains("创意") || message.contains("设计") || message.contains("想象");
    }
    
    private boolean isProblemSolvingQuery(String message) {
        return message.contains("如何") || message.contains("怎么") || message.contains("解决");
    }
    
    private boolean isComparisonQuery(String message) {
        return message.contains("比较") || message.contains("区别") || message.contains("对比");
    }
    
    /**
     * 思考结果类
     */
    public static class ThinkingResult {
        private final List<ThinkingStep> steps;
        private final ThinkingStrategy strategy;
        private final long timestamp;
        
        public ThinkingResult(List<ThinkingStep> steps, ThinkingStrategy strategy, long timestamp) {
            this.steps = steps;
            this.strategy = strategy;
            this.timestamp = timestamp;
        }
        
        public List<ThinkingStep> getSteps() { return steps; }
        public ThinkingStrategy getStrategy() { return strategy; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 思考策略枚举
     */
    public enum ThinkingStrategy {
        FACTUAL_ANALYSIS("事实分析"),
        CREATIVE_THINKING("创意思维"),
        PROBLEM_SOLVING("问题解决"),
        COMPARATIVE_ANALYSIS("对比分析"),
        GENERAL_THINKING("通用思考");
        
        private final String description;
        
        ThinkingStrategy(String description) {
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
