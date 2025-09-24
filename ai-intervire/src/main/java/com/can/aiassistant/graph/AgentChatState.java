package com.can.aiassistant.graph;

import com.can.aiassistant.dto.ChatRequest;
import com.can.aiassistant.service.ThinkingExecutor.ThinkingResult;
import com.can.aiassistant.service.MemoryManager.MemoryContext;
import lombok.Getter;
import lombok.Setter;

/**
 * Agent Chat的状态类
 */
@Getter
@Setter
public class AgentChatState extends GraphState {
    
    // 常用状态键定义
    public static final String REQUEST = "request";
    public static final String SESSION_ID = "sessionId";
    public static final String ROUTE = "route";
    public static final String THINKING_RESULT = "thinkingResult";
    public static final String MEMORY_CONTEXT = "memoryContext";
    public static final String FUNCTION_CALL_RESULT = "functionCallResult";
    public static final String GENERATED_RESPONSE = "generatedResponse";
    public static final String PROCESSING_ROUTE = "processingRoute";
    public static final String CACHE_RESULT = "cacheResult";
    public static final String ERROR = "error";
    public static final String EXECUTION_METRICS = "executionMetrics";
    
    public AgentChatState() {
        super();
    }
    
    // 便捷方法
    public ChatRequest getRequest() {
        return get(REQUEST, ChatRequest.class);
    }
    
    public void setRequest(ChatRequest request) {
        set(REQUEST, request);
    }
    
    public String getSessionId() {
        return get(SESSION_ID, String.class);
    }
    
    public void setSessionId(String sessionId) {
        set(SESSION_ID, sessionId);
    }
    
    public ThinkingResult getThinkingResult() {
        return get(THINKING_RESULT, ThinkingResult.class);
    }
    
    public void setThinkingResult(ThinkingResult result) {
        set(THINKING_RESULT, result);
    }
    
    public MemoryContext getMemoryContext() {
        return get(MEMORY_CONTEXT, MemoryContext.class);
    }
    
    public void setMemoryContext(MemoryContext context) {
        set(MEMORY_CONTEXT, context);
    }
    
    public String getGeneratedResponse() {
        return get(GENERATED_RESPONSE, String.class);
    }
    
    public void setGeneratedResponse(String response) {
        set(GENERATED_RESPONSE, response);
    }
    
    public boolean hasError() {
        return contains(ERROR);
    }
    
    public String getError() {
        return get(ERROR, String.class);
    }
    
    public void setError(String error) {
        set(ERROR, error);
    }
    
    @Override
    public GraphState copy() {
        AgentChatState copy = new AgentChatState();
        copy.data.putAll(this.data);
        copy.timestamp = this.timestamp;
        return copy;
    }
}
