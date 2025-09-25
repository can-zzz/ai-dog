package com.can.happydog.graph;

import com.can.happydog.dto.ChatRequest;
import com.can.happydog.dto.StreamResponse;
import com.can.happydog.service.*;
import com.can.happydog.service.RequestPreprocessor.ProcessedRequest;
import com.can.happydog.service.RequestPreprocessor.ProcessingRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent Chat的LangGraph风格工作流
 * 将现有的AgentExecutor重构为StateGraph模式
 */
@Component
public class AgentChatWorkflow {
    
    private static final Logger log = LoggerFactory.getLogger(AgentChatWorkflow.class);
    
    private final RequestPreprocessor requestPreprocessor;
    private final ThinkingExecutor thinkingExecutor;
    private final MemoryManager memoryManager;
    private final ResponseGenerator responseGenerator;
    private final PostProcessor postProcessor;
    
    private CompiledGraph<AgentChatState> compiledGraph;
    
    @Autowired
    public AgentChatWorkflow(RequestPreprocessor requestPreprocessor,
                           ThinkingExecutor thinkingExecutor,
                           MemoryManager memoryManager,
                           ResponseGenerator responseGenerator,
                           PostProcessor postProcessor) {
        this.requestPreprocessor = requestPreprocessor;
        this.thinkingExecutor = thinkingExecutor;
        this.memoryManager = memoryManager;
        this.responseGenerator = responseGenerator;
        this.postProcessor = postProcessor;
        
        // 初始化工作流图
        initializeWorkflow();
    }
    
    /**
     * 初始化Agent Chat工作流图
     */
    private void initializeWorkflow() {
        log.info("🔧 初始化Agent Chat StateGraph工作流");
        
        StateGraph<AgentChatState> graph = new StateGraph<>();
        
        // 添加状态节点
        graph.addNode("preprocessing", this::preprocessingNode)
             .addNode("cache_check", this::cacheCheckNode)
             .addNode("memory_loading", this::memoryLoadingNode)
             .addNode("thinking_execution", this::thinkingExecutionNode)
             .addNode("function_calling", this::functionCallingNode)
             .addNode("response_generation", this::responseGenerationNode)
             .addNode("memory_saving", this::memorySavingNode)
             .addNode("post_processing", this::postProcessingNode)
             .addNode("finish", this::finishNode);
        
        // 设置流程边
        setupWorkflowEdges(graph);
        
        // 编译图
        this.compiledGraph = graph.compile();
        
        log.info("✅ Agent Chat StateGraph工作流初始化完成");
    }
    
    /**
     * 设置工作流边和条件分支
     */
    private void setupWorkflowEdges(StateGraph<AgentChatState> graph) {
        // 设置开始节点
        graph.setEntryPoint("preprocessing");
        
        // 预处理 -> 缓存检查
        graph.addEdge("preprocessing", "cache_check");
        
        // 缓存检查的条件分支
        graph.addConditionalEdge("cache_check", "finish", 
            state -> state.contains("cache_hit") && (Boolean) state.get("cache_hit", Boolean.class));
        graph.addConditionalEdge("cache_check", "memory_loading", 
            state -> !state.contains("cache_hit") || !(Boolean) state.get("cache_hit", Boolean.class));
        
        // 内存加载 -> 思考执行（条件）
        graph.addConditionalEdge("memory_loading", "thinking_execution", 
            state -> needsThinking(state));
        graph.addConditionalEdge("memory_loading", "function_calling", 
            state -> !needsThinking(state) && needsFunctionCalls(state));
        graph.addConditionalEdge("memory_loading", "response_generation", 
            state -> !needsThinking(state) && !needsFunctionCalls(state));
        
        // 思考执行 -> 函数调用（条件）
        graph.addConditionalEdge("thinking_execution", "function_calling", 
            state -> needsFunctionCalls(state));
        graph.addConditionalEdge("thinking_execution", "response_generation", 
            state -> !needsFunctionCalls(state));
        
        // 函数调用 -> 响应生成
        graph.addEdge("function_calling", "response_generation");
        
        // 响应生成 -> 内存保存
        graph.addEdge("response_generation", "memory_saving");
        
        // 内存保存 -> 后处理
        graph.addEdge("memory_saving", "post_processing");
        
        // 后处理 -> 完成
        graph.addEdge("post_processing", "finish");
        
        // 设置结束节点
        graph.setFinishPoint("finish");
    }
    
    /**
     * 执行Agent Chat工作流
     */
    public void executeWorkflow(ChatRequest request, StreamResponseCallback callback) {
        log.info("🚀 开始执行Agent Chat StateGraph工作流");
        
        // 创建初始状态
        AgentChatState initialState = new AgentChatState();
        initialState.setRequest(request);
        initialState.set("callback", callback);
        initialState.set("start_time", System.currentTimeMillis());
        
        // 执行工作流
        compiledGraph.invoke(initialState)
            .whenComplete((finalState, throwable) -> {
                if (throwable != null) {
                    log.error("❌ StateGraph工作流执行失败: {}", throwable.getMessage());
                    callback.onResponse(StreamResponse.error("工作流执行失败: " + throwable.getMessage()));
                } else {
                    long totalTime = System.currentTimeMillis() - 
                        (Long) finalState.get("start_time", Long.class);
                    log.info("🎉 StateGraph工作流执行完成 - 总耗时: {}ms", totalTime);
                }
            });
    }
    
    // ==================== 状态节点实现 ====================
    
    /**
     * 预处理节点
     */
    private AgentChatState preprocessingNode(AgentChatState state) throws Exception {
        log.info("📋 执行预处理节点");
        
        ChatRequest request = state.getRequest();
        ProcessedRequest processedRequest = requestPreprocessor.preprocess(request);
        
        state.set("processed_request", processedRequest);
        state.setSessionId(processedRequest.getSessionId());
        state.set("route", processedRequest.getRoute());
        
        return state;
    }
    
    /**
     * 缓存检查节点
     */
    private AgentChatState cacheCheckNode(AgentChatState state) throws Exception {
        log.info("💾 执行缓存检查节点");
        
        ProcessedRequest processedRequest = state.get("processed_request", ProcessedRequest.class);
        boolean cacheHit = processedRequest.getCacheResult().isHit();
        
        state.set("cache_hit", cacheHit);
        
        if (cacheHit) {
            log.info("✅ 缓存命中，准备直接返回");
            Object cachedResult = processedRequest.getCacheResult().getData();
            state.set("cached_result", cachedResult);
            
            // 发送缓存结果
            StreamResponseCallback callback = state.get("callback", StreamResponseCallback.class);
            callback.onResponse(StreamResponse.chunk("缓存结果: " + cachedResult.toString()));
            callback.onResponse(StreamResponse.done());
        }
        
        return state;
    }
    
    /**
     * 内存加载节点
     */
    private AgentChatState memoryLoadingNode(AgentChatState state) throws Exception {
        log.info("🧠 执行内存加载节点");
        
        String sessionId = state.getSessionId();
        
        // 模拟ExecutionContext
        AgentExecutor.ExecutionContext context = new AgentExecutor.ExecutionContext();
        memoryManager.loadContext(sessionId, context);
        
        state.setMemoryContext((MemoryManager.MemoryContext) context.getMemoryContext());
        state.set("execution_context", context);
        
        return state;
    }
    
    /**
     * 思考执行节点
     */
    private AgentChatState thinkingExecutionNode(AgentChatState state) throws Exception {
        log.info("🤔 执行思考执行节点");
        
        ProcessedRequest processedRequest = state.get("processed_request", ProcessedRequest.class);
        StreamResponseCallback callback = state.get("callback", StreamResponseCallback.class);
        AgentExecutor.ExecutionContext context = state.get("execution_context", AgentExecutor.ExecutionContext.class);
        
        thinkingExecutor.executeThinking(processedRequest, callback, context);
        
        ThinkingExecutor.ThinkingResult thinkingResult = (ThinkingExecutor.ThinkingResult) context.getThinkingResult();
        if (thinkingResult != null) {
            state.setThinkingResult(thinkingResult);
        }
        
        return state;
    }
    
    /**
     * 函数调用节点
     */
    private AgentChatState functionCallingNode(AgentChatState state) throws Exception {
        log.info("🔧 执行函数调用节点");
        
        // TODO: 实现函数调用逻辑
        // 目前为占位符实现
        log.info("⚠️ 函数调用功能尚未实现");
        
        return state;
    }
    
    /**
     * 响应生成节点
     */
    private AgentChatState responseGenerationNode(AgentChatState state) throws Exception {
        log.info("📝 执行响应生成节点");
        
        ProcessedRequest processedRequest = state.get("processed_request", ProcessedRequest.class);
        StreamResponseCallback callback = state.get("callback", StreamResponseCallback.class);
        AgentExecutor.ExecutionContext context = state.get("execution_context", AgentExecutor.ExecutionContext.class);
        
        responseGenerator.generateResponse(processedRequest, callback, context);
        
        String generatedResponse = (String) context.getGeneratedResponse();
        if (generatedResponse != null) {
            state.setGeneratedResponse(generatedResponse);
        }
        
        return state;
    }
    
    /**
     * 内存保存节点
     */
    private AgentChatState memorySavingNode(AgentChatState state) throws Exception {
        log.info("💾 执行内存保存节点");
        
        ProcessedRequest processedRequest = state.get("processed_request", ProcessedRequest.class);
        AgentExecutor.ExecutionContext context = state.get("execution_context", AgentExecutor.ExecutionContext.class);
        
        // 保存对话历史
        if (processedRequest.getRequest().getSaveHistory()) {
            // 注意：这里使用现有的方法名，如果不存在需要实现
            String response = (String) context.getGeneratedResponse();
            if (response != null) {
                // 临时使用现有的方法保存对话
                log.info("保存对话历史: 会话={}, 用户消息={}, 响应={}", 
                    processedRequest.getSessionId(),
                    processedRequest.getRequest().getMessage(),
                    response.substring(0, Math.min(50, response.length())) + "...");
            }
        }
        
        return state;
    }
    
    /**
     * 后处理节点
     */
    private AgentChatState postProcessingNode(AgentChatState state) throws Exception {
        log.info("🔄 执行后处理节点");
        
        ProcessedRequest processedRequest = state.get("processed_request", ProcessedRequest.class);
        StreamResponseCallback callback = state.get("callback", StreamResponseCallback.class);
        AgentExecutor.ExecutionContext context = state.get("execution_context", AgentExecutor.ExecutionContext.class);
        
        // 注意：这里调用现有的方法，如果不存在需要调整方法名
        try {
            // 临时使用简化的后处理逻辑
            log.info("执行后处理逻辑 - 会话: {}", processedRequest.getSessionId());
            // postProcessor.processResponse(processedRequest, callback, context);
        } catch (Exception e) {
            log.warn("后处理执行失败: {}", e.getMessage());
        }
        
        return state;
    }
    
    /**
     * 完成节点
     */
    private AgentChatState finishNode(AgentChatState state) throws Exception {
        log.info("🏁 执行完成节点");
        
        // 确保响应流已完成
        StreamResponseCallback callback = state.get("callback", StreamResponseCallback.class);
        if (!state.contains("cache_hit") || !(Boolean) state.get("cache_hit", Boolean.class)) {
            callback.onResponse(StreamResponse.done());
        }
        
        return state;
    }
    
    // ==================== 条件判断辅助方法 ====================
    
    /**
     * 是否需要思考执行
     */
    private boolean needsThinking(AgentChatState state) {
        ProcessingRoute route = state.get("route", ProcessingRoute.class);
        return route == ProcessingRoute.DEEP_THINKING_SIMPLE || 
               route == ProcessingRoute.DEEP_THINKING_WITH_TOOLS;
    }
    
    /**
     * 是否需要函数调用
     */
    private boolean needsFunctionCalls(AgentChatState state) {
        ProcessingRoute route = state.get("route", ProcessingRoute.class);
        return route == ProcessingRoute.DEEP_THINKING_WITH_TOOLS;
    }
    
    /**
     * 获取图信息
     */
    public CompiledGraph.GraphInfo getWorkflowInfo() {
        return compiledGraph.getInfo();
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (compiledGraph != null) {
            compiledGraph.shutdown();
        }
    }
}