package com.can.happydog.controller;

import com.can.happydog.dto.UserAction;
import com.can.happydog.service.UserActionTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Web页面控制器
 */
@Controller
@RequiredArgsConstructor
public class WebController {
    
    private final UserActionTracker userActionTracker;
    
    /**
     * 主页
     */
    @GetMapping("/")
    public String index(Model model, HttpServletRequest request) {
        String sessionId = UUID.randomUUID().toString();
        model.addAttribute("sessionId", sessionId);
        
        // 记录主页访问
        userActionTracker.trackPageView(request, UserAction.PageType.HOME);
        
        return "index";
    }
    
    /**
     * 聊天页面
     */
    @GetMapping("/chat")
    public String chat(Model model, HttpServletRequest request) {
        String sessionId = UUID.randomUUID().toString();
        model.addAttribute("sessionId", sessionId);
        
        // 记录聊天页面访问
        userActionTracker.trackPageView(request, UserAction.PageType.CHAT);
        
        return "chat";
    }
    
    /**
     * 带会话ID的聊天页面
     */
    @GetMapping("/chat/{sessionId}")
    public String chatWithSession(@PathVariable String sessionId, Model model, HttpServletRequest request) {
        model.addAttribute("sessionId", sessionId);
        
        // 记录聊天页面访问
        userActionTracker.trackPageView(request, UserAction.PageType.CHAT);
        
        return "chat";
    }
    
    /**
     * 用户行为统计页面
     */
    @GetMapping("/user-actions")
    public String userActions(HttpServletRequest request) {
        // 记录统计页面访问
        userActionTracker.trackPageView(request, UserAction.PageType.OTHER);
        
        return "user-actions";
    }
    
}