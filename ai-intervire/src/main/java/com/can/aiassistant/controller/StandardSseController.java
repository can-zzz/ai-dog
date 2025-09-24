package com.can.aiassistant.controller;

import com.can.aiassistant.dto.ChatRequest;
import com.can.aiassistant.dto.StandardSseResponse;
import com.can.aiassistant.service.AgentExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 标准SSE流式输出控制器
 * 支持标准的 event: + data: 格式
 */
@RestController
@RequestMapping("/api/standard-sse")
@CrossOrigin(origins = "*")
public class StandardSseController {
    
    private static final Logger log = LoggerFactory.getLogger(StandardSseController.class);
    
    private final AgentExecutor agentExecutor;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.stream.timeout:300000}")
    private long streamTimeout;
    
    @Autowired
    public StandardSseController(AgentExecutor agentExecutor, ObjectMapper objectMapper) {
        this.agentExecutor = agentExecutor;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * 标准SSE格式的流式聊天接口
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter standardSseChat(@Valid @RequestBody ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "unknown";
        String sseId = UUID.randomUUID().toString();
        String conversationId = UUID.randomUUID().toString();
        
        log.info("=== 标准SSE流式聊天请求开始 ===");
        log.info("📥 请求参数 - 会话: {}, SSE ID: {}, 消息: {}", 
            sessionId, sseId, 
            request.getMessage().substring(0, Math.min(50, request.getMessage().length())) + "...");
        
        SseEmitter emitter = new SseEmitter(streamTimeout);
        
        // 设置超时处理
        emitter.onTimeout(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("⏰ 标准SSE聊天超时 - 会话: {}, 处理时长: {}ms", sessionId, duration);
            emitter.completeWithError(new RuntimeException("请求超时，请稍后重试"));
        });
        
        // 设置错误处理
        emitter.onError((throwable) -> {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 标准SSE聊天发生错误 - 会话: {}, 错误: {}, 处理时长: {}ms", 
                sessionId, throwable.getMessage(), duration);
        });
        
        // 设置完成处理
        emitter.onCompletion(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 标准SSE聊天完成 - 会话: {}, 总处理时长: {}ms", sessionId, duration);
            log.info("=== 标准SSE流式聊天请求结束 ===");
        });
        
        executorService.execute(() -> {
            AtomicInteger eventIndex = new AtomicInteger(0);
            try {
                
                // 1. 发送开始事件
                sendSseEvent(emitter, StandardSseResponse.opened(sseId, conversationId));
                
                // 2. 如果启用深度思考，发送搜索事件
                if (request.getEnableDeepThinking()) {
                    sendSseEvent(emitter, StandardSseResponse.onlineSearch(
                        eventIndex.incrementAndGet(),
                        "thoughtDetail",
                        "正在深度思考中...",
                        "分析问题并准备详细回答"
                    ));
                }
                
                // 3. 执行AI处理并发送消息事件
                agentExecutor.execute(request, response -> {
                    try {
                        if (response.getError() != null) {
                            // 发送错误消息
                            sendSseEvent(emitter, StandardSseResponse.message(
                                sseId, 
                                eventIndex.incrementAndGet(), 
                                "抱歉，处理您的请求时出现错误: " + response.getError()
                            ));
                            emitter.complete();
                            return;
                        }
                        
                        if (response.getCurrentStep() != null) {
                            // 发送思考步骤作为搜索事件
                            sendSseEvent(emitter, StandardSseResponse.onlineSearch(
                                eventIndex.incrementAndGet(),
                                "thoughtDetail",
                                response.getCurrentStep().getTitle(),
                                response.getCurrentStep().getContent()
                            ));
                        }
                        
                        if (response.getContent() != null && !response.getContent().isEmpty()) {
                            // 发送内容消息
                            sendSseEvent(emitter, StandardSseResponse.message(
                                sseId, 
                                eventIndex.incrementAndGet(), 
                                response.getContent()
                            ));
                        }
                        
                        if (response.isDone()) {
                            // 发送完成事件
                            sendSseEvent(emitter, StandardSseResponse.finished(sseId, eventIndex.incrementAndGet()));
                            emitter.complete();
                        }
                        
                    } catch (IOException e) {
                        log.error("Error sending standard SSE response: " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                });
                
            } catch (Exception e) {
                log.error("Standard SSE chat error: " + e.getMessage());
                try {
                    sendSseEvent(emitter, StandardSseResponse.message(
                        sseId, 
                        eventIndex.incrementAndGet(), 
                        "系统错误: " + e.getMessage()
                    ));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 发送标准SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, StandardSseResponse response) throws IOException {
        if (response.getData() != null) {
            String jsonData = objectMapper.writeValueAsString(response.getData());
            
            // 发送标准SSE格式: event: + data:
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name(response.getEventType().getValue())
                .data(jsonData);
                
            emitter.send(eventBuilder);
            log.debug("📤 发送SSE事件: {} - 数据长度: {}", 
                response.getEventType().getValue(), jsonData.length());
        } else {
            // 对于ping等无数据事件
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name(response.getEventType().getValue());
                
            emitter.send(eventBuilder);
            log.debug("📤 发送SSE事件: {} (无数据)", response.getEventType().getValue());
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public String health() {
        return "Standard SSE API is healthy";
    }
}
