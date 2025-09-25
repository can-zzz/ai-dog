package com.can.happydog.controller;

import com.can.happydog.dto.ChatMessage;
import com.can.happydog.dto.ChatRequest;
import com.can.happydog.dto.StreamResponse;
import com.can.happydog.service.AiService;
import com.can.happydog.service.AgentExecutor;
import com.can.happydog.service.UserActionTracker;
import com.can.happydog.graph.AgentChatWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流式输出控制器
 */
@RestController
@RequestMapping("/api/stream")
@CrossOrigin(origins = "*")
public class StreamController {
    
    private static final Logger log = LoggerFactory.getLogger(StreamController.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final AiService aiService;
    private final AgentExecutor agentExecutor;
    private final AgentChatWorkflow agentChatWorkflow;
    private final UserActionTracker userActionTracker;
    
    @Value("${ai.stream.timeout:300000}")
    private long streamTimeout;
    
    @Autowired
    public StreamController(AiService aiService, AgentExecutor agentExecutor, 
                           AgentChatWorkflow agentChatWorkflow, UserActionTracker userActionTracker) {
        this.aiService = aiService;
        this.agentExecutor = agentExecutor;
        this.agentChatWorkflow = agentChatWorkflow;
        this.userActionTracker = userActionTracker;
    }
    
    /**
     * 流式聊天接口
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "unknown";
        
        log.info("=== 流式聊天请求开始 ===");
        log.info("会话ID: {}", sessionId);
        log.info("用户消息: {}", request.getMessage());
        log.info("深度思考: {}", request.getEnableDeepThinking());
        log.info("超时配置: {}ms", streamTimeout);
        
        // 记录用户消息
        log.info("🔍 [DEBUG] 开始记录用户消息 - 会话: {}, 消息: {}", sessionId, request.getMessage());
        try {
            userActionTracker.trackChatMessage(httpRequest, request.getMessage(), 
                                             ChatMessage.MessageType.USER, null, null);
            log.info("✅ [DEBUG] 用户消息记录成功 - 会话: {}", sessionId);
        } catch (Exception e) {
            log.error("❌ [DEBUG] 用户消息记录失败 - 会话: {}, 错误: {}", sessionId, e.getMessage());
        }
        
        // 使用配置的超时时间
        SseEmitter emitter = new SseEmitter(streamTimeout);
        
        // 设置超时处理
        emitter.onTimeout(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("⚠️ 流式聊天请求超时 - 会话: {}, 消息: {}, 处理时长: {}ms", 
                sessionId, request.getMessage(), duration);
            emitter.completeWithError(new RuntimeException("请求超时，请稍后重试"));
        });
        
        // 设置错误处理
        emitter.onError((throwable) -> {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 流式聊天发生错误 - 会话: {}, 错误: {}, 处理时长: {}ms", 
                sessionId, throwable.getMessage(), duration);
        });
        
        // 设置完成处理
        emitter.onCompletion(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 流式聊天完成 - 会话: {}, 总处理时长: {}ms", sessionId, duration);
            log.info("=== 流式聊天请求结束 ===");
        });
        
        executorService.execute(() -> {
            StringBuilder fullResponseContent = new StringBuilder();
            
            try {
                // 使用新的智能体执行器
                agentExecutor.execute(request, response -> {
                    try {
                        emitter.send(response);
                        
                        // 收集响应内容用于记录
                        if (response.getContent() != null) {
                            fullResponseContent.append(response.getContent());
                        }
                        
                        if (response.isDone() || response.getError() != null) {
                            // 记录AI回复
                            long responseTime = System.currentTimeMillis() - startTime;
                            String finalContent = response.getError() != null 
                                ? "AI回复出错: " + response.getError() 
                                : fullResponseContent.toString();
                            int httpStatus = response.getError() != null ? 500 : 200;
                            
                            log.info("🔍 [DEBUG] 开始记录AI回复 - 会话: {}, 内容长度: {}, 响应时间: {}ms", 
                                    sessionId, finalContent.length(), responseTime);
                            try {
                                userActionTracker.trackChatMessage(httpRequest, finalContent, 
                                                                 ChatMessage.MessageType.ASSISTANT, responseTime, httpStatus);
                                log.info("✅ [DEBUG] AI回复记录成功 - 会话: {}", sessionId);
                            } catch (Exception e) {
                                log.error("❌ [DEBUG] AI回复记录失败 - 会话: {}, 错误: {}", sessionId, e.getMessage());
                            }
                            
                            emitter.complete();
                        }
                    } catch (IOException e) {
                        log.error("Error sending stream response: " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                });
            } catch (Exception e) {
                log.error("Stream chat error: " + e.getMessage());
                try {
                    // 记录错误响应
                    long responseTime = System.currentTimeMillis() - startTime;
                    userActionTracker.trackChatMessage(httpRequest, "流式聊天错误: " + e.getMessage(), 
                                                     ChatMessage.MessageType.ASSISTANT, responseTime, 500);
                    
                    emitter.send(StreamResponse.error(e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * StateGraph模式的流式聊天接口
     */
    @PostMapping(value = "/chat-graph", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatWithGraph(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "unknown";
        
        log.info("=== StateGraph流式聊天请求开始 ===");
        log.info("📥 请求参数 - 会话: {}, 消息: {}, 深度思考: {}", 
            sessionId, 
            request.getMessage().substring(0, Math.min(50, request.getMessage().length())) + "...",
            request.getEnableDeepThinking());
        
        // 记录用户消息
        userActionTracker.trackChatMessage(httpRequest, request.getMessage(), 
                                         ChatMessage.MessageType.USER, null, null);
        
        SseEmitter emitter = new SseEmitter(streamTimeout);
        
        // 设置超时处理
        emitter.onTimeout(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("⏰ StateGraph流式聊天超时 - 会话: {}, 处理时长: {}ms", sessionId, duration);
            emitter.completeWithError(new RuntimeException("请求超时，请稍后重试"));
        });
        
        // 设置错误处理
        emitter.onError((throwable) -> {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ StateGraph流式聊天发生错误 - 会话: {}, 错误: {}, 处理时长: {}ms", 
                sessionId, throwable.getMessage(), duration);
        });
        
        // 设置完成处理
        emitter.onCompletion(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ StateGraph流式聊天完成 - 会话: {}, 总处理时长: {}ms", sessionId, duration);
            log.info("=== StateGraph流式聊天请求结束 ===");
        });
        
        executorService.execute(() -> {
            StringBuilder fullResponseContent = new StringBuilder();
            
            try {
                // 使用新的StateGraph工作流
                agentChatWorkflow.executeWorkflow(request, response -> {
                    try {
                        emitter.send(response);
                        
                        // 收集响应内容用于记录
                        if (response.getContent() != null) {
                            fullResponseContent.append(response.getContent());
                        }
                        
                        if (response.isDone() || response.getError() != null) {
                            // 记录AI回复
                            long responseTime = System.currentTimeMillis() - startTime;
                            String finalContent = response.getError() != null 
                                ? "StateGraph回复出错: " + response.getError() 
                                : fullResponseContent.toString();
                            int httpStatus = response.getError() != null ? 500 : 200;
                            
                            userActionTracker.trackChatMessage(httpRequest, finalContent, 
                                                             ChatMessage.MessageType.ASSISTANT, responseTime, httpStatus);
                            
                            emitter.complete();
                        }
                    } catch (IOException e) {
                        log.error("Error sending StateGraph stream response: " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                });
            } catch (Exception e) {
                log.error("StateGraph stream chat error: " + e.getMessage());
                try {
                    // 记录错误响应
                    long responseTime = System.currentTimeMillis() - startTime;
                    userActionTracker.trackChatMessage(httpRequest, "StateGraph流式聊天错误: " + e.getMessage(), 
                                                     ChatMessage.MessageType.ASSISTANT, responseTime, 500);
                    
                    emitter.send(StreamResponse.error(e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 获取StateGraph工作流信息
     */
    @GetMapping("/workflow-info")
    public Object getWorkflowInfo() {
        return agentChatWorkflow.getWorkflowInfo();
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public StreamResponse health() {
        return StreamResponse.chunk("Stream API is healthy");
    }
}
