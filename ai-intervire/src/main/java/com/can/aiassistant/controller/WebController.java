package com.can.aiassistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Web页面控制器
 */
@Controller
public class WebController {
    
    /**
     * 主页
     */
    @GetMapping("/")
    public String index(Model model) {
        String sessionId = UUID.randomUUID().toString();
        model.addAttribute("sessionId", sessionId);
        return "index";
    }
    
    /**
     * 聊天页面
     */
    @GetMapping("/chat")
    public String chat(Model model) {
        String sessionId = UUID.randomUUID().toString();
        model.addAttribute("sessionId", sessionId);
        return "chat";
    }
    
    /**
     * 带会话ID的聊天页面
     */
    @GetMapping("/chat/{sessionId}")
    public String chatWithSession(@PathVariable String sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        return "chat";
    }
    
    /**
     * 诊断页面
     */
    @GetMapping("/diagnostic")
    public String diagnostic() {
        return "diagnostic";
    }
    
    /**
     * 流式调试页面
     */
    @GetMapping("/stream-debug")
    public String streamDebug() {
        return "stream-debug";
    }
    
    /**
     * 简单测试页面
     */
    @GetMapping("/simple-test")
    public String simpleTest() {
        return "simple-test";
    }
    
    /**
     * StateGraph测试页面
     */
    @GetMapping("/graph-test")
    public String graphTest(Model model) {
        String sessionId = "graph-test-" + UUID.randomUUID().toString().substring(0, 8);
        model.addAttribute("sessionId", sessionId);
        return "graph-test";
    }

    /**
     * 简化聊天页面
     */
    @GetMapping("/chat-simple")
    public String chatSimple(Model model) {
        String sessionId = UUID.randomUUID().toString();
        model.addAttribute("sessionId", sessionId);
        return "chat-simple";
    }

    /**
     * 标准SSE聊天页面
     */
    @GetMapping("/standard-sse-chat")
    public String standardSseChat(Model model) {
        String sessionId = UUID.randomUUID().toString();
        model.addAttribute("sessionId", sessionId);
        return "standard-sse-chat";
    }

    /**
     * 调试聊天页面
     */
    @GetMapping("/debug-chat")
    public String debugChat(Model model) {
        String sessionId = "debug-" + UUID.randomUUID().toString().substring(0, 8);
        model.addAttribute("sessionId", sessionId);
        return "debug-chat";
    }
}