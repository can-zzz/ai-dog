package com.can.aiassistant.service;

import com.can.aiassistant.dto.StreamResponse;
import com.can.aiassistant.dto.ChatMessage;
import com.can.aiassistant.service.RequestPreprocessor.ProcessedRequest;
import com.can.aiassistant.service.RequestPreprocessor.ProcessingRoute;
import com.can.aiassistant.service.AgentExecutor.ExecutionContext;
import com.can.aiassistant.service.MemoryManager.MemoryContext;
import com.can.aiassistant.config.PromptTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 响应生成器
 * 负责根据上下文和工具结果生成最终响应，支持RAG生成和通用生成
 */
@Component
public class ResponseGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(ResponseGenerator.class);
    
    private final AiService aiService;
    private final PromptTemplates promptTemplates;
    
    @Value("${ai.model}")
    private String model;
    
    @Value("${ai.system-prompt}")
    private String systemPrompt;
    
    @Autowired
    public ResponseGenerator(AiService aiService, PromptTemplates promptTemplates) {
        this.aiService = aiService;
        this.promptTemplates = promptTemplates;
    }
    
    /**
     * 生成响应
     */
    public void generateResponse(ProcessedRequest processedRequest, StreamResponseCallback callback, ExecutionContext context) {
        long generationStartTime = System.currentTimeMillis();
        String sessionId = processedRequest.getSessionId();
        
        try {
            log.info("📝 响应生成开始 - 会话: {}, 路由: {}", sessionId, processedRequest.getRoute());
            
            // 根据路由选择生成策略
            GenerationStrategy strategy = determineGenerationStrategy(processedRequest, context);
            log.info("🎯 生成策略: {} - 会话: {}", strategy, sessionId);
            
            // 执行响应生成
            String response = executeGenerationStrategy(processedRequest, context, strategy, callback);
            
            // 更新执行上下文
            context.setGeneratedResponse(response);
            
            long generationDuration = System.currentTimeMillis() - generationStartTime;
            log.info("✅ 响应生成完成 - 会话: {}, 策略: {}, 响应长度: {}, 耗时: {}ms", 
                sessionId, strategy, response.length(), generationDuration);
            
        } catch (Exception e) {
            long generationDuration = System.currentTimeMillis() - generationStartTime;
            log.error("❌ 响应生成失败 - 会话: {}, 错误: {}, 耗时: {}ms", 
                sessionId, e.getMessage(), generationDuration);
            
            // 发送错误响应
            callback.onResponse(StreamResponse.error("响应生成失败: " + e.getMessage()));
            throw e;
        }
    }
    
    /**
     * 确定生成策略
     */
    private GenerationStrategy determineGenerationStrategy(ProcessedRequest processedRequest, ExecutionContext context) {
        // 检查是否有工具调用结果
        if (context.getFunctionCallResult() != null) {
            return GenerationStrategy.RAG_GENERATION;
        }
        
        // 检查是否有思考结果
        if (context.getThinkingResult() != null) {
            return GenerationStrategy.THINKING_BASED_GENERATION;
        }
        
        // 检查路由类型
        ProcessingRoute route = processedRequest.getRoute();
        switch (route) {
            case SIMPLE_CHAT:
                return GenerationStrategy.SIMPLE_GENERATION;
            case STANDARD_CHAT:
                return GenerationStrategy.CONTEXT_AWARE_GENERATION;
            case DEEP_THINKING_SIMPLE:
            case DEEP_THINKING_WITH_TOOLS:
                return GenerationStrategy.ENHANCED_GENERATION;
            default:
                return GenerationStrategy.STANDARD_GENERATION;
        }
    }
    
    /**
     * 执行生成策略
     */
    private String executeGenerationStrategy(ProcessedRequest processedRequest, ExecutionContext context, 
                                           GenerationStrategy strategy, StreamResponseCallback callback) {
        switch (strategy) {
            case RAG_GENERATION:
                return executeRAGGeneration(processedRequest, context, callback);
            case THINKING_BASED_GENERATION:
                return executeThinkingBasedGeneration(processedRequest, context, callback);
            case CONTEXT_AWARE_GENERATION:
                return executeContextAwareGeneration(processedRequest, context, callback);
            case ENHANCED_GENERATION:
                return executeEnhancedGeneration(processedRequest, context, callback);
            case SIMPLE_GENERATION:
                return executeSimpleGeneration(processedRequest, context, callback);
            default:
                return executeStandardGeneration(processedRequest, context, callback);
        }
    }
    
    /**
     * RAG生成（基于工具结果）
     */
    private String executeRAGGeneration(ProcessedRequest processedRequest, ExecutionContext context, StreamResponseCallback callback) {
        log.info("🔍 执行RAG生成");
        
        // 发送RAG提示
        callback.onResponse(StreamResponse.chunk("正在基于搜索结果生成回答..."));
        
        // 构建包含工具结果的消息
        List<Map<String, String>> messages = buildRAGMessages(processedRequest, context);
        
        // 流式调用AI模型
        StringBuilder response = new StringBuilder();
        aiService.streamCallAiModel(messages, new StreamResponseCallback() {
            @Override
            public void onResponse(StreamResponse streamResponse) {
                if (streamResponse.getContent() != null) {
                    response.append(streamResponse.getContent());
                }
                callback.onResponse(streamResponse);
            }
        });
        
        return response.toString();
    }
    
    /**
     * 基于思考的生成
     */
    private String executeThinkingBasedGeneration(ProcessedRequest processedRequest, ExecutionContext context, StreamResponseCallback callback) {
        log.info("🤔 执行基于思考的生成");
        
        // 发送思考基础生成提示
        callback.onResponse(StreamResponse.chunk("基于深度思考结果生成回答..."));
        
        // 构建包含思考结果的消息
        List<Map<String, String>> messages = buildThinkingBasedMessages(processedRequest, context);
        
        // 流式调用AI模型
        StringBuilder response = new StringBuilder();
        aiService.streamCallAiModel(messages, new StreamResponseCallback() {
            @Override
            public void onResponse(StreamResponse streamResponse) {
                if (streamResponse.getContent() != null) {
                    response.append(streamResponse.getContent());
                }
                callback.onResponse(streamResponse);
            }
        });
        
        return response.toString();
    }
    
    /**
     * 上下文感知生成
     */
    private String executeContextAwareGeneration(ProcessedRequest processedRequest, ExecutionContext context, StreamResponseCallback callback) {
        log.info("🧠 执行上下文感知生成");
        
        // 构建包含上下文的消息
        List<Map<String, String>> messages = buildContextAwareMessages(processedRequest, context);
        
        // 流式调用AI模型
        StringBuilder response = new StringBuilder();
        aiService.streamCallAiModel(messages, new StreamResponseCallback() {
            @Override
            public void onResponse(StreamResponse streamResponse) {
                if (streamResponse.getContent() != null) {
                    response.append(streamResponse.getContent());
                }
                callback.onResponse(streamResponse);
            }
        });
        
        return response.toString();
    }
    
    /**
     * 增强生成
     */
    private String executeEnhancedGeneration(ProcessedRequest processedRequest, ExecutionContext context, StreamResponseCallback callback) {
        log.info("⚡ 执行增强生成");
        
        // 发送增强生成提示
        callback.onResponse(StreamResponse.chunk("正在生成增强回答..."));
        
        // 构建增强的消息
        List<Map<String, String>> messages = buildEnhancedMessages(processedRequest, context);
        
        // 流式调用AI模型
        StringBuilder response = new StringBuilder();
        aiService.streamCallAiModel(messages, new StreamResponseCallback() {
            @Override
            public void onResponse(StreamResponse streamResponse) {
                if (streamResponse.getContent() != null) {
                    response.append(streamResponse.getContent());
                }
                callback.onResponse(streamResponse);
            }
        });
        
        return response.toString();
    }
    
    /**
     * 简单生成
     */
    private String executeSimpleGeneration(ProcessedRequest processedRequest, ExecutionContext context, StreamResponseCallback callback) {
        log.info("💬 执行简单生成");
        
        // 构建简单消息
        List<Map<String, String>> messages = buildSimpleMessages(processedRequest);
        
        // 流式调用AI模型
        StringBuilder response = new StringBuilder();
        aiService.streamCallAiModel(messages, new StreamResponseCallback() {
            @Override
            public void onResponse(StreamResponse streamResponse) {
                if (streamResponse.getContent() != null) {
                    response.append(streamResponse.getContent());
                }
                callback.onResponse(streamResponse);
            }
        });
        
        return response.toString();
    }
    
    /**
     * 标准生成
     */
    private String executeStandardGeneration(ProcessedRequest processedRequest, ExecutionContext context, StreamResponseCallback callback) {
        log.info("📄 执行标准生成");
        
        // 构建标准消息
        List<Map<String, String>> messages = buildStandardMessages(processedRequest, context);
        
        // 流式调用AI模型
        StringBuilder response = new StringBuilder();
        aiService.streamCallAiModel(messages, new StreamResponseCallback() {
            @Override
            public void onResponse(StreamResponse streamResponse) {
                if (streamResponse.getContent() != null) {
                    response.append(streamResponse.getContent());
                }
                callback.onResponse(streamResponse);
            }
        });
        
        return response.toString();
    }
    
    /**
     * 构建RAG消息
     */
    private List<Map<String, String>> buildRAGMessages(ProcessedRequest processedRequest, ExecutionContext context) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt + "\n\n请基于提供的搜索结果和工具调用结果来回答用户问题。");
        messages.add(systemMessage);
        
        // 添加工具结果
        if (context.getFunctionCallResult() != null) {
            Map<String, String> toolMessage = new HashMap<>();
            toolMessage.put("role", "system");
            toolMessage.put("content", "工具调用结果：\n" + context.getFunctionCallResult().toString());
            messages.add(toolMessage);
        }
        
        // 用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", processedRequest.getRequest().getMessage());
        messages.add(userMessage);
        
        return messages;
    }
    
    /**
     * 构建基于思考的消息
     */
    private List<Map<String, String>> buildThinkingBasedMessages(ProcessedRequest processedRequest, ExecutionContext context) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt + "\n\n请基于之前的深度思考结果来生成最终回答。");
        messages.add(systemMessage);
        
        // 添加思考结果
        if (context.getThinkingResult() != null) {
            Map<String, String> thinkingMessage = new HashMap<>();
            thinkingMessage.put("role", "system");
            thinkingMessage.put("content", "深度思考结果：\n" + context.getThinkingResult().toString());
            messages.add(thinkingMessage);
        }
        
        // 用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", processedRequest.getRequest().getMessage());
        messages.add(userMessage);
        
        return messages;
    }
    
    /**
     * 构建上下文感知消息
     */
    private List<Map<String, String>> buildContextAwareMessages(ProcessedRequest processedRequest, ExecutionContext context) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        
        // 添加历史上下文
        MemoryContext memoryContext = (MemoryContext) context.getMemoryContext();
        if (memoryContext != null && !memoryContext.getCompressedContext().isEmpty()) {
            for (ChatMessage msg : memoryContext.getCompressedContext()) {
                Map<String, String> historyMessage = new HashMap<>();
                historyMessage.put("role", msg.getType().toString().toLowerCase());
                historyMessage.put("content", msg.getContent());
                messages.add(historyMessage);
            }
        }
        
        // 用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", processedRequest.getRequest().getMessage());
        messages.add(userMessage);
        
        return messages;
    }
    
    /**
     * 构建增强消息
     */
    private List<Map<String, String>> buildEnhancedMessages(ProcessedRequest processedRequest, ExecutionContext context) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 增强的系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt + "\n\n请提供详细、准确、有帮助的回答。考虑多个角度，提供实用的建议。");
        messages.add(systemMessage);
        
        // 添加所有可用的上下文信息
        addAllAvailableContext(messages, context);
        
        // 用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", processedRequest.getRequest().getMessage());
        messages.add(userMessage);
        
        return messages;
    }
    
    /**
     * 构建简单消息
     */
    private List<Map<String, String>> buildSimpleMessages(ProcessedRequest processedRequest) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 简化的系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个友好的AI助手，请简洁地回答用户问题。");
        messages.add(systemMessage);
        
        // 用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", processedRequest.getRequest().getMessage());
        messages.add(userMessage);
        
        return messages;
    }
    
    /**
     * 构建标准消息
     */
    private List<Map<String, String>> buildStandardMessages(ProcessedRequest processedRequest, ExecutionContext context) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 使用提示词模板的标准系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", promptTemplates.getBasicChatSystemPrompt());
        messages.add(systemMessage);
        
        // 用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", processedRequest.getRequest().getMessage());
        messages.add(userMessage);
        
        return messages;
    }
    
    /**
     * 添加所有可用的上下文信息
     */
    private void addAllAvailableContext(List<Map<String, String>> messages, ExecutionContext context) {
        // 添加思考结果
        if (context.getThinkingResult() != null) {
            Map<String, String> thinkingMessage = new HashMap<>();
            thinkingMessage.put("role", "system");
            thinkingMessage.put("content", "思考过程：\n" + context.getThinkingResult().toString());
            messages.add(thinkingMessage);
        }
        
        // 添加工具结果
        if (context.getFunctionCallResult() != null) {
            Map<String, String> toolMessage = new HashMap<>();
            toolMessage.put("role", "system");
            toolMessage.put("content", "工具结果：\n" + context.getFunctionCallResult().toString());
            messages.add(toolMessage);
        }
        
        // 添加历史上下文
        MemoryContext memoryContext = (MemoryContext) context.getMemoryContext();
        if (memoryContext != null && !memoryContext.getCompressedContext().isEmpty()) {
            for (ChatMessage msg : memoryContext.getCompressedContext()) {
                Map<String, String> historyMessage = new HashMap<>();
                historyMessage.put("role", msg.getType().toString().toLowerCase());
                historyMessage.put("content", msg.getContent());
                messages.add(historyMessage);
            }
        }
    }
    
    /**
     * 生成策略枚举
     */
    public enum GenerationStrategy {
        RAG_GENERATION("RAG生成"),
        THINKING_BASED_GENERATION("基于思考生成"),
        CONTEXT_AWARE_GENERATION("上下文感知生成"),
        ENHANCED_GENERATION("增强生成"),
        SIMPLE_GENERATION("简单生成"),
        STANDARD_GENERATION("标准生成");
        
        private final String description;
        
        GenerationStrategy(String description) {
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
