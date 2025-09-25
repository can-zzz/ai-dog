package com.can.happydog.controller;

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
    
}