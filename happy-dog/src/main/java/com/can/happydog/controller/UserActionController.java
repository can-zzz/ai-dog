package com.can.happydog.controller;

import com.can.happydog.dto.UserAction;
import com.can.happydog.service.UserActionTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 用户行为追踪控制器
 */
@RestController
@RequestMapping("/api/user-actions")
@RequiredArgsConstructor
public class UserActionController {
    
    private final UserActionTracker userActionTracker;
    
    /**
     * 获取统计摘要
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getStatsSummary() {
        Map<String, Object> summary = userActionTracker.getStatsSummary();
        return ResponseEntity.ok(summary);
    }
    
    /**
     * 获取会话行为记录
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<UserAction>> getSessionActions(@PathVariable String sessionId) {
        List<UserAction> actions = userActionTracker.getSessionActions(sessionId);
        return ResponseEntity.ok(actions);
    }
    
    /**
     * 获取每日统计
     */
    @GetMapping("/stats/daily/{date}")
    public ResponseEntity<UserActionTracker.UserActionStats> getDailyStats(@PathVariable String date) {
        UserActionTracker.UserActionStats stats = userActionTracker.getDailyStats(date);
        if (stats != null) {
            return ResponseEntity.ok(stats);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 手动记录其他动作
     */
    @PostMapping("/track/other")
    public ResponseEntity<String> trackOtherAction(
            HttpServletRequest request,
            @RequestParam String description,
            @RequestBody(required = false) Map<String, Object> params) {
        
        userActionTracker.trackOtherAction(request, description, params);
        return ResponseEntity.ok("动作记录成功");
    }
}
