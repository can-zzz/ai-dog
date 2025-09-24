package com.can.aiassistant.controller;

import com.can.aiassistant.dto.ChatRequest;
import com.can.aiassistant.dto.StreamResponse;
import com.can.aiassistant.service.AiService;
import com.can.aiassistant.service.AgentExecutor;
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
    
    @Value("${ai.stream.timeout:300000}")
    private long streamTimeout;
    
    @Autowired
    public StreamController(AiService aiService, AgentExecutor agentExecutor) {
        this.aiService = aiService;
        this.agentExecutor = agentExecutor;
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
     * 健康检查接口
     */
    @GetMapping("/health")
    public StreamResponse health() {
        return StreamResponse.chunk("Stream API is healthy");
    }
}
