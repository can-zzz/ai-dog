package com.can.happydog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户行为记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAction {
    
    /**
     * 行为类型枚举
     */
    public enum ActionType {
        PAGE_VIEW,      // 页面访问
        CHAT_MESSAGE,   // 聊天消息
        BUTTON_CLICK,   // 按钮点击
        FORM_SUBMIT,    // 表单提交
        FILE_UPLOAD,    // 文件上传
        OTHER          // 其他动作
    }
    
    /**
     * 页面类型枚举
     */
    public enum PageType {
        HOME,          // 主页
        CHAT,          // 聊天页面
        ABOUT,         // 关于页面
        OTHER          // 其他页面
    }
    
    /**
     * 动作ID（自动生成）
     */
    private String actionId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户IP地址
     */
    private String userIp;
    
    /**
     * User-Agent信息
     */
    private String userAgent;
    
    /**
     * 行为类型
     */
    private ActionType actionType;
    
    /**
     * 页面类型（如果是页面访问）
     */
    private PageType pageType;
    
    /**
     * 动作描述
     */
    private String actionDescription;
    
    /**
     * 请求路径
     */
    private String requestPath;
    
    /**
     * HTTP方法
     */
    private String httpMethod;
    
    /**
     * 聊天消息内容（如果是聊天动作）
     */
    private String messageContent;
    
    /**
     * 消息类型（如果是聊天动作）
     */
    private ChatMessage.MessageType messageType;
    
    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;
    
    /**
     * HTTP状态码
     */
    private Integer httpStatus;
    
    /**
     * 额外的动作参数
     */
    private Map<String, Object> actionParams;
    
    /**
     * 动作时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 浏览器信息
     */
    private String browser;
    
    /**
     * 操作系统信息
     */
    private String operatingSystem;
    
    /**
     * 设备类型
     */
    private String deviceType;
    
    /**
     * 创建页面访问记录
     */
    public static UserAction createPageView(String sessionId, String userIp, String userAgent, 
                                          PageType pageType, String requestPath, String httpMethod) {
        UserAction action = new UserAction();
        action.setActionId(java.util.UUID.randomUUID().toString());
        action.setSessionId(sessionId);
        action.setUserIp(userIp);
        action.setUserAgent(userAgent);
        action.setActionType(ActionType.PAGE_VIEW);
        action.setPageType(pageType);
        action.setActionDescription("访问" + getPageDescription(pageType) + "页面");
        action.setRequestPath(requestPath);
        action.setHttpMethod(httpMethod);
        action.setTimestamp(LocalDateTime.now());
        return action;
    }
    
    /**
     * 创建聊天消息记录
     */
    public static UserAction createChatMessage(String sessionId, String userIp, String userAgent,
                                             String messageContent, ChatMessage.MessageType messageType) {
        UserAction action = new UserAction();
        action.setActionId(java.util.UUID.randomUUID().toString());
        action.setSessionId(sessionId);
        action.setUserIp(userIp);
        action.setUserAgent(userAgent);
        action.setActionType(ActionType.CHAT_MESSAGE);
        action.setActionDescription("发送" + (messageType == ChatMessage.MessageType.USER ? "用户" : "AI") + "消息");
        action.setMessageContent(messageContent);
        action.setMessageType(messageType);
        action.setTimestamp(LocalDateTime.now());
        return action;
    }
    
    /**
     * 创建其他动作记录
     */
    public static UserAction createOtherAction(String sessionId, String userIp, String userAgent,
                                             String actionDescription, Map<String, Object> actionParams) {
        UserAction action = new UserAction();
        action.setActionId(java.util.UUID.randomUUID().toString());
        action.setSessionId(sessionId);
        action.setUserIp(userIp);
        action.setUserAgent(userAgent);
        action.setActionType(ActionType.OTHER);
        action.setActionDescription(actionDescription);
        action.setActionParams(actionParams);
        action.setTimestamp(LocalDateTime.now());
        return action;
    }
    
    /**
     * 获取页面描述
     */
    private static String getPageDescription(PageType pageType) {
        switch (pageType) {
            case HOME: return "主页";
            case CHAT: return "聊天";
            case ABOUT: return "关于";
            default: return "其他";
        }
    }
}
