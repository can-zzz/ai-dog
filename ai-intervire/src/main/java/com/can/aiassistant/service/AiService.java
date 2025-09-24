package com.can.aiassistant.service;

import com.can.aiassistant.dto.ChatMessage;
import com.can.aiassistant.dto.ChatRequest;
import com.can.aiassistant.dto.ChatResponse;
import com.can.aiassistant.dto.StreamResponse;
import com.can.aiassistant.dto.ThinkingStep;
import com.can.aiassistant.exception.AiAssistantException;
import org.springframework.beans.factory.annotation.Value;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI服务类
 */
@Service
public class AiService {
    
    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    
    private final RestTemplate restTemplate;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // 内存存储聊天历史
    private final Map<String, List<ChatMessage>> chatHistory = new ConcurrentHashMap<>();
    
    @Value("${ai.api-key}")
    private String apiKey;
    
    @Value("${ai.base-url}")
    private String baseUrl;
    
    @Value("${ai.model}")
    private String model;
    
    @Value("${ai.system-prompt}")
    private String systemPrompt;
    
    @Value("${ai.deep-thinking.enabled:false}")
    private boolean deepThinkingEnabled;
    
    @Value("${ai.deep-thinking.thinking-model:qwen-max}")
    private String thinkingModel;
    
    @Value("${ai.deep-thinking.max-thinking-steps:5}")
    private int maxThinkingSteps;
    
    @Value("${ai.deep-thinking.thinking-prompt}")
    private String thinkingPrompt;

    @Value("${ai.stream.chunk-size:10}")
    private int streamChunkSize;

    @Value("${ai.stream.delay:50}")
    private int streamDelay;

    @Value("${ai.stream.timeout:300000}")
    private long streamTimeout;

    public AiService(RestTemplate restTemplate, CloseableHttpClient httpClient, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 流式聊天
     */
    public void streamChat(ChatRequest request, StreamResponseCallback callback) {
        long overallStartTime = System.currentTimeMillis();
        // 生成会话ID（在try块外定义，以便在catch块中使用）
        String sessionId = StringUtils.hasText(request.getSessionId()) 
            ? request.getSessionId() 
            : UUID.randomUUID().toString();
            
        try {
            
            log.info("🚀 AiService.streamChat 开始处理 - 会话: {}", sessionId);
            
            // 保存用户消息到历史记录
            if (request.getSaveHistory()) {
                ChatMessage userMessage = ChatMessage.userMessage(
                    request.getMessage(), sessionId, "用户"
                );
                addMessageToHistory(sessionId, userMessage);
                log.debug("💾 用户消息已保存到历史记录 - 会话: {}", sessionId);
            }
            
            // 检查是否启用深度思考
            if (request.getEnableDeepThinking() && deepThinkingEnabled) {
                log.info("🧠 开始深度思考流程 - 会话: {}, 模型: {}", sessionId, thinkingModel);
                long thinkingStartTime = System.currentTimeMillis();
                
                // 执行深度思考流程
                streamThinkingSteps(request.getMessage(), sessionId, callback);
                
                long thinkingDuration = System.currentTimeMillis() - thinkingStartTime;
                log.info("🧠 深度思考流程完成 - 会话: {}, 耗时: {}ms", sessionId, thinkingDuration);
            }
            
            // 构建消息
            log.debug("📝 开始构建消息列表 - 会话: {}", sessionId);
            List<Map<String, String>> messages = buildMessages(request.getMessage(), sessionId);
            
            // 确保每个消息都包含sessionId
            for (Map<String, String> message : messages) {
                message.put("sessionId", sessionId);
            }
            
            // 调用流式API
            log.info("📡 开始调用AI模型流式API - 会话: {}, 消息数量: {}, 模型: {}", 
                sessionId, messages.size(), model);
            long streamStartTime = System.currentTimeMillis();
            
            streamCallAiModel(messages, callback);
            
            long streamDuration = System.currentTimeMillis() - streamStartTime;
            log.info("📡 AI模型流式调用完成 - 会话: {}, 耗时: {}ms", sessionId, streamDuration);
            
            // 发送完成信号
            callback.onResponse(StreamResponse.done());
            
            long overallDuration = System.currentTimeMillis() - overallStartTime;
            log.info("🎉 streamChat 总处理完成 - 会话: {}, 总耗时: {}ms", sessionId, overallDuration);
            
        } catch (Exception e) {
            long overallDuration = System.currentTimeMillis() - overallStartTime;
            log.error("❌ streamChat 处理失败 - 会话: {}, 错误: {}, 总耗时: {}ms", 
                sessionId, e.getMessage(), overallDuration);
            callback.onResponse(StreamResponse.error(e.getMessage()));
        }
    }

    /**
     * 流式输出思考步骤（使用自定义提示词）
     */
    public void streamThinkingStepsWithPrompt(String userMessage, String sessionId, String customPrompt, StreamResponseCallback callback) {
        long thinkingStartTime = System.currentTimeMillis();
        try {
            log.info("🧠 开始构建自定义深度思考消息 - 会话: {}", sessionId);
            
            // 构建使用自定义提示词的深度思考消息
            List<Map<String, String>> thinkingMessages = buildThinkingMessagesWithPrompt(userMessage, sessionId, customPrompt);
            log.debug("📝 自定义深度思考消息构建完成 - 会话: {}, 消息数量: {}", sessionId, thinkingMessages.size());
            
            // 发送开始思考的进度反馈
            callback.onResponse(StreamResponse.thinking(
                ThinkingStep.analyze("策略思考", 
                    "正在使用专门的策略提示词进行深度分析...")
            ));
            
            long modelCallStart = System.currentTimeMillis();
            
            // 使用流式调用进行深度思考
            streamCallThinkingModel(thinkingMessages, thinkingModel, sessionId, callback);
            
            long modelCallDuration = System.currentTimeMillis() - modelCallStart;
            log.info("🤖 策略思考模型调用完成 - 会话: {}, 耗时: {}ms", sessionId, modelCallDuration);
            
            long totalThinkingTime = System.currentTimeMillis() - thinkingStartTime;
            log.info("✅ 策略思考步骤完成 - 会话: {}, 总耗时: {}ms", sessionId, totalThinkingTime);
            
            // 清理跟踪信息
            sentThinkingSteps.remove(sessionId);
            
        } catch (Exception e) {
            long totalThinkingTime = System.currentTimeMillis() - thinkingStartTime;
            log.error("❌ 策略思考步骤失败 - 会话: {}, 错误: {}, 耗时: {}ms", 
                sessionId, e.getMessage(), totalThinkingTime);
            
            // 清理跟踪信息
            sentThinkingSteps.remove(sessionId);
            
            callback.onResponse(StreamResponse.thinking(
                ThinkingStep.analyze("思考过程", "正在分析您的问题...")
            ));
        }
    }

    /**
     * 流式输出思考步骤（使用默认提示词）
     */
    public void streamThinkingSteps(String userMessage, String sessionId, StreamResponseCallback callback) {
        long thinkingStartTime = System.currentTimeMillis();
        try {
            log.info("🧠 开始构建深度思考消息 - 会话: {}", sessionId);
            
            // 构建深度思考的消息
            List<Map<String, String>> thinkingMessages = buildThinkingMessages(userMessage, sessionId);
            log.debug("📝 深度思考消息构建完成 - 会话: {}, 消息数量: {}", sessionId, thinkingMessages.size());
            
            // 调用AI模型进行深度思考
            log.info("🤖 开始调用思考模型 - 会话: {}, 模型: {}", sessionId, thinkingModel);
            
            // 发送开始思考的进度反馈
            callback.onResponse(StreamResponse.thinking(
                ThinkingStep.analyze("开始深度思考", 
                    "正在调用" + thinkingModel + "模型进行深度分析，预计需要30-60秒，请耐心等待...")
            ));
            
            long modelCallStart = System.currentTimeMillis();
            
            // 使用流式调用进行深度思考
            streamCallThinkingModel(thinkingMessages, thinkingModel, sessionId, callback);
            
            long modelCallDuration = System.currentTimeMillis() - modelCallStart;
            log.info("🤖 流式思考模型调用完成 - 会话: {}, 耗时: {}ms", sessionId, modelCallDuration);
            
            long totalThinkingTime = System.currentTimeMillis() - thinkingStartTime;
            log.info("✅ 思考步骤流式输出完成 - 会话: {}, 总耗时: {}ms", sessionId, totalThinkingTime);
            
            // 清理跟踪信息
            sentThinkingSteps.remove(sessionId);
            
        } catch (Exception e) {
            long totalThinkingTime = System.currentTimeMillis() - thinkingStartTime;
            log.error("❌ 思考步骤流式输出失败 - 会话: {}, 错误: {}, 耗时: {}ms", 
                sessionId, e.getMessage(), totalThinkingTime);
            
            // 清理跟踪信息
            sentThinkingSteps.remove(sessionId);
            
            callback.onResponse(StreamResponse.thinking(
                ThinkingStep.analyze("思考过程", "正在分析您的问题...")
            ));
        }
    }

    /**
     * 流式调用思考模型
     */
    private void streamCallThinkingModel(List<Map<String, String>> messages, String modelName, String sessionId, StreamResponseCallback callback) {
        final long streamCallStart = System.currentTimeMillis();
        
        try {
            log.info("🔍 开始验证思考模型调用参数 - 会话: {}, 模型: {}", sessionId, modelName);
            
            // 验证参数
            if (messages == null || messages.isEmpty()) {
                throw new AiAssistantException("消息列表不能为空");
            }
            
            if (!StringUtils.hasText(modelName)) {
                throw new AiAssistantException("AI模型配置不能为空");
            }
            
            if (!StringUtils.hasText(baseUrl)) {
                throw new AiAssistantException("AI服务基础URL配置不能为空");
            }
            
            if (!StringUtils.hasText(apiKey)) {
                throw new AiAssistantException("AI服务API密钥配置不能为空");
            }
            
            log.info("📡 开始流式思考模型调用 - 会话: {}, 模型: {}, 消息数: {}", 
                sessionId, modelName, messages.size());
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4000); // 思考过程需要更多tokens
            requestBody.put("stream", true);
            
            // 创建POST请求
            HttpPost httpPost = new HttpPost(baseUrl + "/chat/completions");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));
            
            // 执行请求并处理流式响应
            log.debug("🌐 开始执行思考模型HTTP请求 - 会话: {}", sessionId);
            long httpRequestStart = System.currentTimeMillis();
            
            httpClient.execute(httpPost, response -> {
                try {
                    long httpResponseTime = System.currentTimeMillis() - httpRequestStart;
                    log.info("🌐 思考模型HTTP响应已接收 - 会话: {}, 响应时间: {}ms, 状态码: {}", 
                        sessionId, httpResponseTime, response.getCode());
                    
                    StringBuilder currentThinkingContent = new StringBuilder();
                    StringBuilder contentBuffer = new StringBuilder(); // 用于缓冲内容
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.getEntity().getContent())
                    );
                    String line;
                    int chunkCount = 0;
                    long firstChunkTime = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            chunkCount++;
                            if (firstChunkTime == 0) {
                                firstChunkTime = System.currentTimeMillis() - httpRequestStart;
                                log.info("⚡ 思考模型首个数据块已接收 - 会话: {}, 首块延迟: {}ms", sessionId, firstChunkTime);
                            }
                            
                            String data = line.substring(6).trim();
                            if (data.equals("[DONE]")) {
                                log.info("🏁 思考模型流式响应结束 - 会话: {}, 总块数: {}", sessionId, chunkCount);
                                break;
                            }
                            
                            try {
                                // 解析JSON响应
                                Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                                if (chunk == null) {
                                    log.warn("⚠️ 思考模型接收到空数据块 - 会话: {}, 块序号: {}", sessionId, chunkCount);
                                    continue;
                                }
                                
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                                
                                if (choices != null && !choices.isEmpty()) {
                                    Map<String, Object> choice = choices.get(0);
                                    if (choice == null) {
                                        log.warn("⚠️ 思考模型接收到空选择 - 会话: {}, 块序号: {}", sessionId, chunkCount);
                                        continue;
                                    }
                                    
                                    Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                                    
                                    if (delta != null && delta.containsKey("content")) {
                                        String content = (String) delta.get("content");
                                        if (content != null) {
                                            currentThinkingContent.append(content);
                                            contentBuffer.append(content);
                                            
                                            // 流式发送思考内容用于累积显示
                                            if (content.length() > 0) {
                                                try {
                                                    callback.onResponse(StreamResponse.thinking(
                                                        ThinkingStep.reason("思考中", content)
                                                    ));
                                                } catch (Exception ex) {
                                                    // 如果callback失败（如连接已断开），停止处理
                                                    log.debug("⚠️ 思考内容发送失败，停止处理 - 会话: {}, 错误: {}", sessionId, ex.getMessage());
                                                    return null; // 提前结束流式响应处理
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("⚠️ 处理思考模型数据块时出错 - 会话: {}, 错误: {}", sessionId, e.getMessage());
                                continue;
                            }
                        }
                        Thread.sleep(streamDelay);
                    }
                    
                    // 思考完成，发送完成标识
                    try {
                        callback.onResponse(StreamResponse.thinking(
                            ThinkingStep.analyze("思考完成", "深度思考已完成，正在生成回答...")
                        ));
                    } catch (Exception ex) {
                        log.debug("⚠️ 思考完成标识发送失败 - 会话: {}, 错误: {}", sessionId, ex.getMessage());
                    }
                    
                    long totalStreamTime = System.currentTimeMillis() - streamCallStart;
                    log.info("✅ 思考模型流式响应处理完成 - 会话: {}, 总耗时: {}ms, 总块数: {}, 首块延迟: {}ms, 思考内容长度: {}字符", 
                        sessionId, totalStreamTime, chunkCount, firstChunkTime, currentThinkingContent.length());
                    
                    return null;
                } catch (Exception e) {
                    long totalStreamTime = System.currentTimeMillis() - streamCallStart;
                    log.error("❌ 思考模型流式响应处理失败 - 会话: {}, 错误: {}, 耗时: {}ms", 
                        sessionId, e.getMessage(), totalStreamTime);
                    throw new RuntimeException(e);
                }
            });
            
        } catch (Exception e) {
            long totalStreamTime = System.currentTimeMillis() - streamCallStart;
            log.error("❌ 思考模型流式调用失败 - 会话: {}, 错误: {}, 总耗时: {}ms", 
                sessionId, e.getMessage(), totalStreamTime);
            throw new AiAssistantException("调用思考模型失败: " + e.getMessage(), e);
        }
    }

    // 用于跟踪每个会话已发送的思考步骤数量
    private final Map<String, Integer> sentThinkingSteps = new ConcurrentHashMap<>();
    
    /**
     * 实时解析并发送思考步骤
     */
    private void parseAndSendThinkingSteps(String currentContent, String sessionId, StreamResponseCallback callback) {
        try {
            // 查找完整的思考步骤标记
            String[] sections = currentContent.split("【|】");
            
            List<ThinkingStep> steps = new ArrayList<>();
            
            for (int i = 0; i < sections.length - 1; i += 2) {
                if (i + 1 < sections.length) {
                    String title = sections[i].trim();
                    String content = sections[i + 1].trim();
                    
                    if (!title.isEmpty() && !content.isEmpty() && content.length() > 10) {
                        // 只有当内容足够完整时才发送
                        ThinkingStep.StepType type = determineStepType(title);
                        ThinkingStep step = new ThinkingStep(type, title, content);
                        steps.add(step);
                    }
                }
            }
            
            // 获取已发送的步骤数量
            int sentCount = sentThinkingSteps.getOrDefault(sessionId, 0);
            
            // 只发送新的步骤
            for (int i = sentCount; i < steps.size(); i++) {
                ThinkingStep step = steps.get(i);
                log.debug("📤 实时发送思考步骤 {}/{} - 会话: {}, 类型: {}, 标题: {}", 
                    i + 1, steps.size(), sessionId, step.getType(), step.getTitle());
                
                try {
                    callback.onResponse(StreamResponse.thinking(step));
                    Thread.sleep(streamDelay);
                    
                    // 更新已发送的步骤数量
                    sentThinkingSteps.put(sessionId, i + 1);
                } catch (Exception ex) {
                    // 如果callback失败（如连接已断开），停止发送
                    log.debug("⚠️ 思考步骤发送失败，停止处理 - 会话: {}, 错误: {}", sessionId, ex.getMessage());
                    break;
                }
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 实时解析思考步骤失败 - 会话: {}, 错误: {}", sessionId, e.getMessage());
        }
    }

    /**
     * 流式调用AI模型
     */
    public void streamCallAiModel(List<Map<String, String>> messages, StreamResponseCallback callback) {
        final long streamCallStart = System.currentTimeMillis();
        final String sessionId;
        
        // 尝试从消息中获取sessionId
        if (!messages.isEmpty() && messages.get(0).containsKey("sessionId")) {
            sessionId = messages.get(0).get("sessionId");
        } else {
            sessionId = "unknown";
        }
        
        try {
            log.info("🔍 开始验证流式调用参数 - 会话: {}", sessionId);
            
            // 验证参数
            if (messages == null || messages.isEmpty()) {
                throw new AiAssistantException("消息列表不能为空");
            }
            
            if (!StringUtils.hasText(model)) {
                throw new AiAssistantException("AI模型配置不能为空");
            }
            
            if (!StringUtils.hasText(baseUrl)) {
                throw new AiAssistantException("AI服务基础URL配置不能为空");
            }
            
            if (!StringUtils.hasText(apiKey)) {
                throw new AiAssistantException("AI服务API密钥配置不能为空");
            }
            
            log.info("📡 开始流式AI模型调用 - 会话: {}, 模型: {}, 消息数: {}", 
                sessionId, model, messages.size());
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", true);
            
            // 创建POST请求
            HttpPost httpPost = new HttpPost(baseUrl + "/chat/completions");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));
            
            // 执行请求并处理流式响应
            log.debug("🌐 开始执行HTTP请求 - 会话: {}", sessionId);
            long httpRequestStart = System.currentTimeMillis();
            
            httpClient.execute(httpPost, response -> {
                try {
                    long httpResponseTime = System.currentTimeMillis() - httpRequestStart;
                    log.info("🌐 HTTP响应已接收 - 会话: {}, 响应时间: {}ms, 状态码: {}", 
                        sessionId, httpResponseTime, response.getCode());
                    
                    StringBuilder currentMessage = new StringBuilder();
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.getEntity().getContent())
                    );
                    String line;
                    int chunkCount = 0;
                    long firstChunkTime = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            chunkCount++;
                            if (firstChunkTime == 0) {
                                firstChunkTime = System.currentTimeMillis() - httpRequestStart;
                                log.info("⚡ 首个数据块已接收 - 会话: {}, 首块延迟: {}ms", sessionId, firstChunkTime);
                            }
                            
                            String data = line.substring(6).trim();
                            if (data.equals("[DONE]")) {
                                log.info("🏁 流式响应结束标记 - 会话: {}, 总块数: {}", sessionId, chunkCount);
                                break;
                            }
                            
                            try {
                                // 解析JSON响应
                                Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                                if (chunk == null) {
                                    log.warn("⚠️ 接收到空数据块 - 会话: {}, 块序号: {}", sessionId, chunkCount);
                                    continue;
                                }
                                
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                                
                                if (choices != null && !choices.isEmpty()) {
                                    Map<String, Object> choice = choices.get(0);
                                    if (choice == null) {
                                        log.warn("Received null choice from AI model");
                                        continue;
                                    }
                                    
                                    Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                                    
                                    if (delta != null && delta.containsKey("content")) {
                                        String content = (String) delta.get("content");
                                        if (content != null) {
                                            currentMessage.append(content);
                                            
                                            // 发送内容块
                                            try {
                                                callback.onResponse(StreamResponse.chunk(content));
                                            } catch (Exception ex) {
                                                // 如果callback失败（如连接已断开），停止处理
                                                log.debug("⚠️ 内容块发送失败，停止处理 - 会话: {}, 错误: {}", sessionId, ex.getMessage());
                                                return null; // 提前结束流式响应处理
                                            }
                                            
                                            // 注意：历史记录现在由新架构的MemoryManager管理
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Error processing chunk: " + e.getMessage());
                                // 继续处理下一个chunk，而不是中断整个流
                                continue;
                            }
                        }
                        Thread.sleep(streamDelay);
                    }
                    
                    long totalStreamTime = System.currentTimeMillis() - streamCallStart;
                    log.info("✅ 流式响应处理完成 - 会话: {}, 总耗时: {}ms, 总块数: {}, 首块延迟: {}ms, 消息长度: {}字符", 
                        sessionId, totalStreamTime, chunkCount, firstChunkTime, currentMessage.length());
                    
                    // 发送完成信号
                    try {
                        callback.onResponse(StreamResponse.done());
                        log.debug("✅ 发送完成信号 - 会话: {}", sessionId);
                    } catch (Exception ex) {
                        log.debug("⚠️ 发送完成信号失败 - 会话: {}, 错误: {}", sessionId, ex.getMessage());
                    }
                    
                    return null;
                } catch (Exception e) {
                    long totalStreamTime = System.currentTimeMillis() - streamCallStart;
                    log.error("❌ 流式响应处理失败 - 会话: {}, 错误: {}, 耗时: {}ms", 
                        sessionId, e.getMessage(), totalStreamTime);
                    throw new RuntimeException(e);
                }
            });
            
        } catch (Exception e) {
            long totalStreamTime = System.currentTimeMillis() - streamCallStart;
            log.error("❌ 流式AI模型调用失败 - 会话: {}, 错误: {}, 总耗时: {}ms", 
                sessionId, e.getMessage(), totalStreamTime);
            throw new AiAssistantException("调用AI模型失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理聊天请求
     */
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 生成会话ID
            String sessionId = StringUtils.hasText(request.getSessionId()) 
                ? request.getSessionId() 
                : UUID.randomUUID().toString();
            
            // 保存用户消息到历史记录
            if (request.getSaveHistory()) {
                ChatMessage userMessage = ChatMessage.userMessage(
                    request.getMessage(), sessionId, "用户"
                );
                addMessageToHistory(sessionId, userMessage);
            }
            
            // 检查是否启用深度思考
            if (request.getEnableDeepThinking() && deepThinkingEnabled) {
                // 执行深度思考流程
                List<ThinkingStep> thinkingSteps = performDeepThinking(request.getMessage(), sessionId);
                
                // 基于思考结果生成最终回答
                String response = generateFinalAnswer(request.getMessage(), sessionId, thinkingSteps);
                
                // 保存AI回复到历史记录
                if (request.getSaveHistory()) {
                    ChatMessage assistantMessage = ChatMessage.assistantMessage(
                        response, sessionId
                    );
                    addMessageToHistory(sessionId, assistantMessage);
                }
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                log.info("Deep thinking chat completed for session: " + sessionId + ", processing time: " + processingTime + "ms");
                
                return ChatResponse.successWithThinking(response, sessionId, thinkingModel, processingTime, thinkingSteps);
                
            } else {
                // 普通聊天流程
                List<Map<String, String>> messages = buildMessages(request.getMessage(), sessionId);
                
                // 调用AI模型
                String response = callAiModel(messages);
                
                // 保存AI回复到历史记录
                if (request.getSaveHistory()) {
                    ChatMessage assistantMessage = ChatMessage.assistantMessage(
                        response, sessionId
                    );
                    addMessageToHistory(sessionId, assistantMessage);
                }
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                log.info("Chat completed for session: " + sessionId + ", processing time: " + processingTime + "ms");
                
                return ChatResponse.success(response, sessionId, model, processingTime);
            }
            
        } catch (Exception e) {
            log.error("Chat processing failed: " + e.getMessage());
            throw new AiAssistantException("AI处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建消息列表
     */
    private List<Map<String, String>> buildMessages(String userMessage, String sessionId) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        systemMessage.put("sessionId", sessionId); // 添加sessionId
        messages.add(systemMessage);
        
        // 添加历史对话
        List<ChatMessage> history = getHistory(sessionId);
        for (ChatMessage msg : history) {
            Map<String, String> historyMessage = new HashMap<>();
            if (msg.getType() == ChatMessage.MessageType.USER) {
                historyMessage.put("role", "user");
            } else if (msg.getType() == ChatMessage.MessageType.ASSISTANT) {
                historyMessage.put("role", "assistant");
            } else {
                continue; // 跳过系统消息
            }
            historyMessage.put("content", msg.getContent());
            historyMessage.put("sessionId", sessionId); // 添加sessionId
            messages.add(historyMessage);
        }
        
        // 添加当前用户消息
        Map<String, String> currentMessage = new HashMap<>();
        currentMessage.put("role", "user");
        currentMessage.put("content", userMessage);
        currentMessage.put("sessionId", sessionId); // 添加sessionId
        messages.add(currentMessage);
        
        return messages;
    }
    
    /**
     * 调用AI模型
     */
    private String callAiModel(List<Map<String, String>> messages) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions", 
                request, 
                Map.class
            );
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    String content = (String) message.get("content");
                    
                    if (content != null && !content.trim().isEmpty()) {
                        return content.trim();
                    }
                }
            }
            
            throw new AiAssistantException("AI模型返回空响应");
            
        } catch (Exception e) {
            log.error("AI model call failed: " + e.getMessage());
            throw new AiAssistantException("调用AI模型失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行深度思考流程
     */
    private List<ThinkingStep> performDeepThinking(String userMessage, String sessionId) {
        List<ThinkingStep> thinkingSteps = new ArrayList<>();
        
        try {
            // 构建深度思考的消息
            List<Map<String, String>> thinkingMessages = buildThinkingMessages(userMessage, sessionId);
            
            // 调用AI模型进行深度思考
            String thinkingResponse = callAiModelWithModel(thinkingMessages, thinkingModel);
            
            // 解析思考步骤
            thinkingSteps = parseThinkingSteps(thinkingResponse);
            
            log.info("Deep thinking completed with " + thinkingSteps.size() + " steps");
            
        } catch (Exception e) {
            log.error("Deep thinking failed: " + e.getMessage());
            // 如果深度思考失败，添加一个默认的思考步骤
            thinkingSteps.add(ThinkingStep.analyze("思考过程", "正在分析您的问题..."));
        }
        
        return thinkingSteps;
    }
    
    /**
     * 构建深度思考的消息列表（使用自定义提示词）
     */
    private List<Map<String, String>> buildThinkingMessagesWithPrompt(String userMessage, String sessionId, String customPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加自定义思考系统提示
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", customPrompt);
        systemMessage.put("sessionId", sessionId);
        messages.add(systemMessage);
        
        // 添加用户问题
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "请对以下问题进行深度思考：\n\n" + userMessage);
        userMsg.put("sessionId", sessionId);
        messages.add(userMsg);
        
        return messages;
    }
    
    /**
     * 构建深度思考的消息列表（使用默认提示词）
     */
    private List<Map<String, String>> buildThinkingMessages(String userMessage, String sessionId) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加思考系统提示
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", thinkingPrompt);
        systemMessage.put("sessionId", sessionId); // 添加sessionId
        messages.add(systemMessage);
        
        // 添加用户问题
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "请对以下问题进行深度思考：\n\n" + userMessage);
        userMsg.put("sessionId", sessionId); // 添加sessionId
        messages.add(userMsg);
        
        return messages;
    }
    
    /**
     * 解析思考步骤
     */
    private List<ThinkingStep> parseThinkingSteps(String thinkingResponse) {
        List<ThinkingStep> steps = new ArrayList<>();
        
        // 简单的解析逻辑，根据标记分割思考步骤
        String[] sections = thinkingResponse.split("【|】");
        
        for (int i = 0; i < sections.length - 1; i += 2) {
            if (i + 1 < sections.length) {
                String title = sections[i].trim();
                String content = sections[i + 1].trim();
                
                if (!title.isEmpty() && !content.isEmpty()) {
                    ThinkingStep.StepType type = determineStepType(title);
                    steps.add(new ThinkingStep(type, title, content));
                }
            }
        }
        
        // 如果解析失败，创建一个默认步骤
        if (steps.isEmpty()) {
            steps.add(ThinkingStep.reason("深度思考", thinkingResponse));
        }
        
        return steps;
    }
    
    /**
     * 根据标题确定思考步骤类型
     */
    private ThinkingStep.StepType determineStepType(String title) {
        if (title.contains("分析") || title.contains("理解")) {
            return ThinkingStep.StepType.ANALYZE;
        } else if (title.contains("搜集") || title.contains("信息") || title.contains("背景")) {
            return ThinkingStep.StepType.RESEARCH;
        } else if (title.contains("推理") || title.contains("思考") || title.contains("逻辑")) {
            return ThinkingStep.StepType.REASON;
        } else if (title.contains("综合") || title.contains("整理") || title.contains("整合")) {
            return ThinkingStep.StepType.SYNTHESIZE;
        } else if (title.contains("验证") || title.contains("检查") || title.contains("确认")) {
            return ThinkingStep.StepType.VALIDATE;
        } else {
            return ThinkingStep.StepType.REASON;
        }
    }
    
    /**
     * 根据内容智能生成步骤标题
     */
    private String generateStepTitle(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "思考步骤";
        }
        
        String trimmedContent = content.trim();
        
        // 如果内容以"###"开始，提取标题
        if (trimmedContent.startsWith("###")) {
            String[] lines = trimmedContent.split("\n");
            String titleLine = lines[0].replace("###", "").trim();
            if (!titleLine.isEmpty()) {
                return titleLine.length() > 20 ? titleLine.substring(0, 20) + "..." : titleLine;
            }
        }
        
        // 根据内容关键词判断
        String lowerContent = trimmedContent.toLowerCase();
        if (lowerContent.contains("宏观") || lowerContent.contains("角度") || lowerContent.contains("视角")) {
            return "宏观分析";
        } else if (lowerContent.contains("定义") || lowerContent.contains("概念")) {
            return "概念解析";
        } else if (lowerContent.contains("时间") || lowerContent.contains("历史")) {
            return "时间维度分析";
        } else if (lowerContent.contains("空间") || lowerContent.contains("地理")) {
            return "空间维度分析";
        } else if (lowerContent.contains("政治") || lowerContent.contains("经济") || lowerContent.contains("社会")) {
            return "多维度分析";
        } else if (lowerContent.contains("总结") || lowerContent.contains("结论")) {
            return "总结思考";
        } else {
            // 取内容的前20个字符作为标题
            String shortTitle = trimmedContent.length() > 20 ? 
                trimmedContent.substring(0, 20) + "..." : trimmedContent;
            // 移除换行符
            return shortTitle.replaceAll("\n", " ");
        }
    }
    
    /**
     * 根据内容确定步骤类型
     */
    private ThinkingStep.StepType determineStepTypeFromContent(String content) {
        if (content == null) return ThinkingStep.StepType.REASON;
        
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("分析") || lowerContent.contains("角度") || lowerContent.contains("维度")) {
            return ThinkingStep.StepType.ANALYZE;
        } else if (lowerContent.contains("定义") || lowerContent.contains("概念") || lowerContent.contains("范围")) {
            return ThinkingStep.StepType.RESEARCH;
        } else if (lowerContent.contains("思考") || lowerContent.contains("理解") || lowerContent.contains("认识")) {
            return ThinkingStep.StepType.REASON;
        } else if (lowerContent.contains("综合") || lowerContent.contains("整理") || lowerContent.contains("总结")) {
            return ThinkingStep.StepType.SYNTHESIZE;
        } else if (lowerContent.contains("验证") || lowerContent.contains("检验") || lowerContent.contains("确认")) {
            return ThinkingStep.StepType.VALIDATE;
        } else {
            return ThinkingStep.StepType.REASON;
        }
    }
    
    /**
     * 基于思考结果生成最终答案
     */
    private String generateFinalAnswer(String userMessage, String sessionId, List<ThinkingStep> thinkingSteps) {
        try {
            // 构建包含思考结果的消息
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt + "\n\n你已经完成了深度思考，现在请基于思考结果给出简洁明了的最终答案。");
            systemMessage.put("sessionId", sessionId); // 添加sessionId
            messages.add(systemMessage);
            
            // 添加历史对话
            List<ChatMessage> history = getHistory(sessionId);
            for (ChatMessage msg : history) {
                Map<String, String> historyMessage = new HashMap<>();
                if (msg.getType() == ChatMessage.MessageType.USER) {
                    historyMessage.put("role", "user");
                } else if (msg.getType() == ChatMessage.MessageType.ASSISTANT) {
                    historyMessage.put("role", "assistant");
                } else {
                    continue;
                }
                historyMessage.put("content", msg.getContent());
                historyMessage.put("sessionId", sessionId); // 添加sessionId
                messages.add(historyMessage);
            }
            
            // 添加思考过程摘要
            StringBuilder thinkingSummary = new StringBuilder();
            thinkingSummary.append("基于以下思考过程：\n\n");
            for (ThinkingStep step : thinkingSteps) {
                thinkingSummary.append("【").append(step.getTitle()).append("】：")
                             .append(step.getContent()).append("\n\n");
            }
            thinkingSummary.append("现在请回答用户的问题：").append(userMessage);
            
            Map<String, String> thinkingMessage = new HashMap<>();
            thinkingMessage.put("role", "user");
            thinkingMessage.put("content", thinkingSummary.toString());
            thinkingMessage.put("sessionId", sessionId); // 添加sessionId
            messages.add(thinkingMessage);
            
            return callAiModel(messages);
            
        } catch (Exception e) {
            log.error("Generate final answer failed: " + e.getMessage());
            return "抱歉，在生成最终答案时遇到了问题。请稍后重试。";
        }
    }
    
    /**
     * 使用指定模型调用AI
     */
    private String callAiModelWithModel(List<Map<String, String>> messages, String modelName) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4000); // 思考过程可能需要更多tokens
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions", 
                request, 
                Map.class
            );
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    String content = (String) message.get("content");
                    
                    if (content != null && !content.trim().isEmpty()) {
                        return content.trim();
                    }
                }
            }
            
            throw new AiAssistantException("AI模型返回空响应");
            
        } catch (Exception e) {
            log.error("AI model call failed: " + e.getMessage());
            throw new AiAssistantException("调用AI模型失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 添加消息到历史记录
     */
    private void addMessageToHistory(String sessionId, ChatMessage message) {
        chatHistory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message);
        
        // 限制历史记录数量（保留最近20条）
        List<ChatMessage> history = chatHistory.get(sessionId);
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }
    }
    
    /**
     * 获取会话历史
     */
    public List<ChatMessage> getHistory(String sessionId) {
        return chatHistory.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * 清除会话历史
     */
    public void clearHistory(String sessionId) {
        chatHistory.remove(sessionId);
        log.info("Cleared history for session: " + sessionId);
    }
    
    /**
     * 获取会话统计信息
     */
    public Map<String, Object> getSessionStats(String sessionId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionId", sessionId);
        stats.put("messageCount", getHistory(sessionId).size());
        stats.put("model", model);
        return stats;
    }
    
    /**
     * 健康检查方法
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("model", model);
        health.put("baseUrl", baseUrl);
        health.put("deepThinkingEnabled", deepThinkingEnabled);
        health.put("streamChunkSize", streamChunkSize);
        health.put("streamDelay", streamDelay);
        
        // 检查关键配置
        boolean configValid = StringUtils.hasText(apiKey) && 
                             StringUtils.hasText(baseUrl) && 
                             StringUtils.hasText(model);
        health.put("configValid", configValid);
        
        if (!configValid) {
            health.put("status", "DOWN");
            health.put("error", "配置不完整");
        }
        
        return health;
    }
}
