package com.can.happydog.controller;

import com.can.happydog.dto.ChatMessage;
import com.can.happydog.dto.ChatRequest;
import com.can.happydog.dto.ChatResponse;
import com.can.happydog.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI控制器
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {
    
    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    
    private final AiService aiService;
    
    @Autowired
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }
    
    /**
     * 聊天接口
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: " + request.getMessage());
        
        ChatResponse response = aiService.chat(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取会话历史
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        log.info("Getting history for session: " + sessionId);
        
        List<ChatMessage> history = aiService.getHistory(sessionId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * 清除会话历史
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearHistory(@PathVariable String sessionId) {
        log.info("Clearing history for session: " + sessionId);
        
        aiService.clearHistory(sessionId);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "message", "会话历史已清除",
            "sessionId", sessionId
        );
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取会话统计信息
     */
    @GetMapping("/stats/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionStats(@PathVariable String sessionId) {
        log.info("Getting stats for session: " + sessionId);
        
        Map<String, Object> stats = aiService.getSessionStats(sessionId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = aiService.healthCheck();
        health.put("service", "AI Assistant");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
}
