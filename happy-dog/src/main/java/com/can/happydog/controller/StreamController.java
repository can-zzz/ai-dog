package com.can.happydog.controller;

import com.can.happydog.dto.ChatRequest;
import com.can.happydog.dto.StreamResponse;
import com.can.happydog.service.AiService;
import com.can.happydog.service.AgentExecutor;
import com.can.happydog.graph.AgentChatWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    
    @Value("${ai.stream.timeout:300000}")
    private long streamTimeout;
    
    @Autowired
    public StreamController(AiService aiService, AgentExecutor agentExecutor, AgentChatWorkflow agentChatWorkflow) {
        this.aiService = aiService;
        this.agentExecutor = agentExecutor;
        this.agentChatWorkflow = agentChatWorkflow;
    }
    
    /**
     * 流式聊天接口
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "unknown";
        
        log.info("=== 流式聊天请求开始 ===");
        log.info("会话ID: {}", sessionId);
        log.info("用户消息: {}", request.getMessage());
        log.info("深度思考: {}", request.getEnableDeepThinking());
        log.info("超时配置: {}ms", streamTimeout);
        
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
            try {
                // 使用新的智能体执行器
                agentExecutor.execute(request, response -> {
                    try {
                        emitter.send(response);
                        
                        if (response.isDone() || response.getError() != null) {
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
    public SseEmitter streamChatWithGraph(@Valid @RequestBody ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "unknown";
        
        log.info("=== StateGraph流式聊天请求开始 ===");
        log.info("📥 请求参数 - 会话: {}, 消息: {}, 深度思考: {}", 
            sessionId, 
            request.getMessage().substring(0, Math.min(50, request.getMessage().length())) + "...",
            request.getEnableDeepThinking());
        
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
            try {
                // 使用新的StateGraph工作流
                agentChatWorkflow.executeWorkflow(request, response -> {
                    try {
                        emitter.send(response);
                        
                        if (response.isDone() || response.getError() != null) {
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
