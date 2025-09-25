package com.can.happydog.service;

import com.can.happydog.dto.StreamResponse;
import com.can.happydog.service.RequestPreprocessor.ProcessedRequest;
import com.can.happydog.service.RequestPreprocessor.ProcessingRoute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 智能体执行器
 * 协调各个执行阶段，实现完整的智能体处理流程
 */
@Component
public class AgentExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(AgentExecutor.class);
    
    private final RequestPreprocessor requestPreprocessor;
    private final ThinkingExecutor thinkingExecutor;
    private final MemoryManager memoryManager;
    private final ResponseGenerator responseGenerator;
    private final PostProcessor postProcessor;
    
    @Autowired
    public AgentExecutor(RequestPreprocessor requestPreprocessor,
                        ThinkingExecutor thinkingExecutor,
                        MemoryManager memoryManager,
                        ResponseGenerator responseGenerator,
                        PostProcessor postProcessor) {
        this.requestPreprocessor = requestPreprocessor;
        this.thinkingExecutor = thinkingExecutor;
        this.memoryManager = memoryManager;
        this.responseGenerator = responseGenerator;
        this.postProcessor = postProcessor;
    }
    
    /**
     * 执行智能体处理流程
     */
    public void execute(com.can.happydog.dto.ChatRequest request, StreamResponseCallback callback) {
        long executionStartTime = System.currentTimeMillis();
        ExecutionContext context = new ExecutionContext();
        
        try {
            log.info("🤖 智能体执行开始");
            
            // 阶段1: 预处理
            ProcessedRequest processedRequest = executePreprocessing(request, context);
            
            // 检查缓存命中
            if (processedRequest.getCacheResult().isHit()) {
                handleCacheHit(processedRequest, callback, context);
                return;
            }
            
            // 阶段2: 内存管理 - 加载历史上下文
            executeMemoryLoading(processedRequest, context);
            
            // 阶段3: 思考执行（如果需要）
            if (needsThinking(processedRequest.getRoute())) {
                executeThinking(processedRequest, callback, context);
            }
            
            // 阶段4: 函数调用（如果需要）
            if (needsFunctionCalls(processedRequest.getRoute())) {
                executeFunctionCalls(processedRequest, callback, context);
            }
            
            // 阶段5: 响应生成
            executeResponseGeneration(processedRequest, callback, context);
            
            // 阶段6: 内存管理 - 保存对话历史
            executeMemorySaving(processedRequest, context);
            
            // 阶段7: 后处理
            executePostProcessing(processedRequest, callback, context);
            
            long totalExecutionTime = System.currentTimeMillis() - executionStartTime;
            log.info("🎉 智能体执行完成 - 会话: {}, 总耗时: {}ms", 
                processedRequest.getSessionId(), totalExecutionTime);
            
        } catch (Exception e) {
            long totalExecutionTime = System.currentTimeMillis() - executionStartTime;
            log.error("❌ 智能体执行失败 - 错误: {}, 耗时: {}ms", e.getMessage(), totalExecutionTime);
            
            // 发送错误响应
            callback.onResponse(StreamResponse.error("处理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 执行预处理阶段
     */
    private ProcessedRequest executePreprocessing(com.can.happydog.dto.ChatRequest request, ExecutionContext context) {
        log.info("📋 执行预处理阶段");
        long startTime = System.currentTimeMillis();
        
        ProcessedRequest processedRequest = requestPreprocessor.preprocess(request);
        context.setProcessedRequest(processedRequest);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ 预处理完成 - 路由: {}, 耗时: {}ms", 
            processedRequest.getRoute(), duration);
        
        return processedRequest;
    }
    
    /**
     * 处理缓存命中
     */
    private void handleCacheHit(ProcessedRequest processedRequest, StreamResponseCallback callback, ExecutionContext context) {
        log.info("💾 处理缓存命中 - 会话: {}", processedRequest.getSessionId());
        
        // 从缓存获取结果并直接返回
        Object cachedResult = processedRequest.getCacheResult().getData();
        
        // 这里需要根据缓存数据类型进行适当的处理
        // 简化处理，直接发送缓存结果
        callback.onResponse(StreamResponse.chunk("缓存结果: " + cachedResult.toString()));
        callback.onResponse(StreamResponse.done());
    }
    
    /**
     * 执行内存加载
     */
    private void executeMemoryLoading(ProcessedRequest processedRequest, ExecutionContext context) {
        log.info("🧠 执行内存加载阶段 - 会话: {}", processedRequest.getSessionId());
        long startTime = System.currentTimeMillis();
        
        // 加载对话历史和相关上下文
        memoryManager.loadContext(processedRequest.getSessionId(), context);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ 内存加载完成 - 耗时: {}ms", duration);
    }
    
    /**
     * 执行思考阶段
     */
    private void executeThinking(ProcessedRequest processedRequest, StreamResponseCallback callback, ExecutionContext context) {
        log.info("🤔 执行思考阶段 - 会话: {}", processedRequest.getSessionId());
        long startTime = System.currentTimeMillis();
        
        thinkingExecutor.executeThinking(processedRequest, callback, context);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ 思考阶段完成 - 耗时: {}ms", duration);
    }
    
    /**
     * 执行函数调用
     */
    private void executeFunctionCalls(ProcessedRequest processedRequest, StreamResponseCallback callback, ExecutionContext context) {
        log.info("🔧 执行函数调用阶段 - 会话: {}", processedRequest.getSessionId());
        long startTime = System.currentTimeMillis();
        
        // TODO: 实现函数调用执行器
        // functionCallExecutor.executeFunctionCalls(processedRequest, callback, context);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ 函数调用完成 - 耗时: {}ms", duration);
    }
    
    /**
     * 执行响应生成
     */
    private void executeResponseGeneration(ProcessedRequest processedRequest, StreamResponseCallback callback, ExecutionContext context) {
        log.info("📝 执行响应生成阶段 - 会话: {}", processedRequest.getSessionId());
        long startTime = System.currentTimeMillis();
        
        responseGenerator.generateResponse(processedRequest, callback, context);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ 响应生成完成 - 耗时: {}ms", duration);
    }
    
    /**
     * 执行内存保存
     */
    private void executeMemorySaving(ProcessedRequest processedRequest, ExecutionContext context) {
        log.info("💾 执行内存保存阶段 - 会话: {}", processedRequest.getSessionId());
        long startTime = System.currentTimeMillis();
        
        memoryManager.saveContext(processedRequest.getSessionId(), context);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ 内存保存完成 - 耗时: {}ms", duration);
    }
    
    /**
     * 执行后处理
     */
    private void executePostProcessing(ProcessedRequest processedRequest, StreamResponseCallback callback, ExecutionContext context) {
        log.info("🔄 执行后处理阶段 - 会话: {}", processedRequest.getSessionId());
        long startTime = System.currentTimeMillis();
        
        postProcessor.postProcess(processedRequest, callback, context);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ 后处理完成 - 耗时: {}ms", duration);
    }
    
    /**
     * 判断是否需要思考
     */
    private boolean needsThinking(ProcessingRoute route) {
        return route == ProcessingRoute.DEEP_THINKING_SIMPLE || 
               route == ProcessingRoute.DEEP_THINKING_WITH_TOOLS;
    }
    
    /**
     * 判断是否需要函数调用
     */
    private boolean needsFunctionCalls(ProcessingRoute route) {
        return route == ProcessingRoute.DEEP_THINKING_WITH_TOOLS;
    }
    
    /**
     * 执行上下文
     */
    public static class ExecutionContext {
        private ProcessedRequest processedRequest;
        private Object memoryContext;
        private Object thinkingResult;
        private Object functionCallResult;
        private Object generatedResponse;
        private long totalTokens;
        private long totalCost;
        
        // Getters and Setters
        public ProcessedRequest getProcessedRequest() { return processedRequest; }
        public void setProcessedRequest(ProcessedRequest processedRequest) { this.processedRequest = processedRequest; }
        
        public Object getMemoryContext() { return memoryContext; }
        public void setMemoryContext(Object memoryContext) { this.memoryContext = memoryContext; }
        
        public Object getThinkingResult() { return thinkingResult; }
        public void setThinkingResult(Object thinkingResult) { this.thinkingResult = thinkingResult; }
        
        public Object getFunctionCallResult() { return functionCallResult; }
        public void setFunctionCallResult(Object functionCallResult) { this.functionCallResult = functionCallResult; }
        
        public Object getGeneratedResponse() { return generatedResponse; }
        public void setGeneratedResponse(Object generatedResponse) { this.generatedResponse = generatedResponse; }
        
        public long getTotalTokens() { return totalTokens; }
        public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }
        
        public long getTotalCost() { return totalCost; }
        public void setTotalCost(long totalCost) { this.totalCost = totalCost; }
    }
}
