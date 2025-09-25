package com.can.happydog.service;

import com.can.happydog.dto.UserAction;
import com.can.happydog.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户行为追踪服务
 */
@Slf4j
@Service
public class UserActionTracker {
    
    private static final String ACTIONS_DIR = "user-actions";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final ObjectMapper objectMapper;
    
    // 内存中的行为统计
    private final Map<String, List<UserAction>> sessionActions = new ConcurrentHashMap<>();
    private final Map<String, UserActionStats> dailyStats = new ConcurrentHashMap<>();
    
    public UserActionTracker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 确保用户行为目录存在
        try {
            Path dir = Paths.get(ACTIONS_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                log.info("创建用户行为追踪目录: {}", dir.toAbsolutePath());
            }
            
            // 启动时加载今日统计数据
            loadTodayStats();
            
        } catch (Exception e) {
            log.warn("无法创建用户行为追踪目录: {}", e.getMessage());
        }
    }
    
    /**
     * 记录页面访问
     */
    public void trackPageView(HttpServletRequest request, UserAction.PageType pageType) {
        String sessionId = getOrCreateSessionId(request);
        String userIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();
        
        UserAction action = UserAction.createPageView(sessionId, userIp, userAgent, 
                                                    pageType, requestPath, httpMethod);
        
        // 解析浏览器和操作系统信息
        parseUserAgent(action, userAgent);
        
        recordAction(action);
        
        log.info("📊 页面访问记录 - 会话: {}, 页面: {}, IP: {}, 路径: {}", 
                sessionId, pageType, userIp, requestPath);
    }
    
    /**
     * 记录聊天消息
     */
    public void trackChatMessage(String sessionId, String userIp, String userAgent,
                               String messageContent, ChatMessage.MessageType messageType, 
                               Long responseTime, Integer httpStatus) {
        UserAction action = UserAction.createChatMessage(sessionId, userIp, userAgent, 
                                                       messageContent, messageType);
        action.setResponseTime(responseTime);
        action.setHttpStatus(httpStatus);
        
        // 解析浏览器和操作系统信息
        parseUserAgent(action, userAgent);
        
        recordAction(action);
        
        log.info("💬 聊天消息记录 - 会话: {}, 类型: {}, 内容长度: {}, 响应时间: {}ms", 
                sessionId, messageType, messageContent != null ? messageContent.length() : 0, responseTime);
    }
    
    /**
     * 记录聊天消息（从HTTP请求）
     */
    public void trackChatMessage(HttpServletRequest request, String messageContent, 
                               ChatMessage.MessageType messageType, Long responseTime, Integer httpStatus) {
        String sessionId = getOrCreateSessionId(request);
        String userIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        trackChatMessage(sessionId, userIp, userAgent, messageContent, messageType, responseTime, httpStatus);
    }
    
    /**
     * 记录其他动作
     */
    public void trackOtherAction(HttpServletRequest request, String actionDescription, 
                               Map<String, Object> actionParams) {
        String sessionId = getOrCreateSessionId(request);
        String userIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        UserAction action = UserAction.createOtherAction(sessionId, userIp, userAgent, 
                                                       actionDescription, actionParams);
        action.setRequestPath(request.getRequestURI());
        action.setHttpMethod(request.getMethod());
        
        // 解析浏览器和操作系统信息
        parseUserAgent(action, userAgent);
        
        recordAction(action);
        
        log.info("🔧 其他动作记录 - 会话: {}, 动作: {}, IP: {}", 
                sessionId, actionDescription, userIp);
    }
    
    /**
     * 记录动作到文件和内存
     */
    private void recordAction(UserAction action) {
        try {
            // 1. 记录到内存
            sessionActions.computeIfAbsent(action.getSessionId(), k -> new ArrayList<>()).add(action);
            
            // 限制内存中的动作数量（每个会话最多保留100条）
            List<UserAction> actions = sessionActions.get(action.getSessionId());
            if (actions.size() > 100) {
                actions.subList(0, actions.size() - 100).clear();
            }
            
            // 2. 记录到日期文件
            String dateStr = action.getTimestamp().format(DATE_FORMATTER);
            Path dailyFile = Paths.get(ACTIONS_DIR, "actions-" + dateStr + ".jsonl");
            
            if (!Files.exists(dailyFile)) {
                Files.createFile(dailyFile);
            }
            
            // 写入JSON行格式
            String jsonLine = objectMapper.writeValueAsString(action) + System.lineSeparator();
            Files.write(dailyFile, jsonLine.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            
            // 3. 更新统计信息
            updateDailyStats(dateStr, action);
            
        } catch (Exception e) {
            log.warn("记录用户行为失败 - 会话: {}, 动作: {}, 错误: {}", 
                    action.getSessionId(), action.getActionType(), e.getMessage());
        }
    }
    
    /**
     * 更新每日统计
     */
    private void updateDailyStats(String dateStr, UserAction action) {
        UserActionStats stats = dailyStats.computeIfAbsent(dateStr, k -> new UserActionStats());
        
        stats.totalActions++;
        
        switch (action.getActionType()) {
            case PAGE_VIEW:
                stats.pageViews++;
                if (action.getPageType() != null) {
                    switch (action.getPageType()) {
                        case HOME:
                            stats.homePageViews++;
                            break;
                        case CHAT:
                            stats.chatPageViews++;
                            break;
                        default:
                            stats.otherPageViews++;
                            break;
                    }
                }
                break;
            case CHAT_MESSAGE:
                stats.chatMessages++;
                if (action.getMessageType() == ChatMessage.MessageType.USER) {
                    stats.userMessages++;
                } else {
                    stats.aiMessages++;
                }
                break;
            default:
                stats.otherActions++;
                break;
        }
        
        // 记录独立用户会话
        stats.uniqueSessions.add(action.getSessionId());
        
        // 记录独立IP
        if (StringUtils.hasText(action.getUserIp())) {
            stats.uniqueIps.add(action.getUserIp());
        }
    }
    
    /**
     * 获取会话统计
     */
    public List<UserAction> getSessionActions(String sessionId) {
        return sessionActions.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * 获取每日统计
     */
    public UserActionStats getDailyStats(String date) {
        return dailyStats.get(date);
    }
    
    /**
     * 获取当前统计摘要
     */
    public Map<String, Object> getStatsSummary() {
        String today = LocalDateTime.now().format(DATE_FORMATTER);
        UserActionStats todayStats = dailyStats.get(today);
        
        Map<String, Object> summary = new HashMap<>();
        
        if (todayStats != null) {
            summary.put("today", Map.of(
                "date", today,
                "totalActions", todayStats.totalActions,
                "pageViews", todayStats.pageViews,
                "homePageViews", todayStats.homePageViews,
                "chatPageViews", todayStats.chatPageViews,
                "chatMessages", todayStats.chatMessages,
                "userMessages", todayStats.userMessages,
                "aiMessages", todayStats.aiMessages,
                "uniqueSessions", todayStats.uniqueSessions.size(),
                "uniqueIps", todayStats.uniqueIps.size()
            ));
        } else {
            summary.put("today", Map.of("date", today, "totalActions", 0));
        }
        
        // 活跃会话数
        summary.put("activeSessions", sessionActions.size());
        
        return summary;
    }
    
    /**
     * 从请求中获取或创建会话ID
     */
    private String getOrCreateSessionId(HttpServletRequest request) {
        // 优先从Header中获取
        String sessionId = request.getHeader("X-Session-Id");
        if (StringUtils.hasText(sessionId)) {
            return sessionId;
        }
        
        // 从会话中获取
        if (request.getSession(false) != null) {
            return request.getSession().getId();
        }
        
        // 生成新的会话ID
        return UUID.randomUUID().toString();
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 解析User-Agent信息
     */
    private void parseUserAgent(UserAction action, String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return;
        }
        
        // 简单的浏览器检测
        if (userAgent.contains("Chrome")) {
            action.setBrowser("Chrome");
        } else if (userAgent.contains("Firefox")) {
            action.setBrowser("Firefox");
        } else if (userAgent.contains("Safari")) {
            action.setBrowser("Safari");
        } else if (userAgent.contains("Edge")) {
            action.setBrowser("Edge");
        } else {
            action.setBrowser("Other");
        }
        
        // 简单的操作系统检测
        if (userAgent.contains("Windows")) {
            action.setOperatingSystem("Windows");
        } else if (userAgent.contains("Mac OS")) {
            action.setOperatingSystem("macOS");
        } else if (userAgent.contains("Linux")) {
            action.setOperatingSystem("Linux");
        } else if (userAgent.contains("Android")) {
            action.setOperatingSystem("Android");
        } else if (userAgent.contains("iOS")) {
            action.setOperatingSystem("iOS");
        } else {
            action.setOperatingSystem("Other");
        }
        
        // 设备类型检测
        if (userAgent.contains("Mobile") || userAgent.contains("Android")) {
            action.setDeviceType("Mobile");
        } else if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
            action.setDeviceType("Tablet");
        } else {
            action.setDeviceType("Desktop");
        }
    }
    
    /**
     * 启动时加载今日统计数据
     */
    private void loadTodayStats() {
        String today = LocalDateTime.now().format(DATE_FORMATTER);
        Path todayFile = Paths.get(ACTIONS_DIR, "actions-" + today + ".jsonl");
        
        if (!Files.exists(todayFile)) {
            log.info("📊 今日行为文件不存在，跳过统计加载: {}", todayFile);
            return;
        }
        
        try {
            log.info("📊 开始加载今日行为统计: {}", todayFile);
            List<String> lines = Files.readAllLines(todayFile, StandardCharsets.UTF_8);
            
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;
                
                try {
                    UserAction action = objectMapper.readValue(line, UserAction.class);
                    // 更新统计信息
                    updateDailyStats(today, action);
                    
                    // 加载到内存中的会话行为
                    sessionActions.computeIfAbsent(action.getSessionId(), k -> new ArrayList<>()).add(action);
                    
                } catch (Exception e) {
                    log.warn("解析行为记录失败: {}, 行内容: {}", e.getMessage(), line.substring(0, Math.min(100, line.length())));
                }
            }
            
            UserActionStats stats = dailyStats.get(today);
            if (stats != null) {
                log.info("✅ 成功加载今日统计 - 总行为: {}, 页面访问: {}, 聊天消息: {}, 独立会话: {}, 独立IP: {}", 
                        stats.totalActions, stats.pageViews, stats.chatMessages, 
                        stats.uniqueSessions.size(), stats.uniqueIps.size());
            }
            
        } catch (Exception e) {
            log.error("加载今日行为统计失败: {}", e.getMessage());
        }
    }
    
    /**
     * 用户行为统计类
     */
    public static class UserActionStats {
        public int totalActions = 0;
        public int pageViews = 0;
        public int homePageViews = 0;
        public int chatPageViews = 0;
        public int otherPageViews = 0;
        public int chatMessages = 0;
        public int userMessages = 0;
        public int aiMessages = 0;
        public int otherActions = 0;
        public Set<String> uniqueSessions = new HashSet<>();
        public Set<String> uniqueIps = new HashSet<>();
    }
}
