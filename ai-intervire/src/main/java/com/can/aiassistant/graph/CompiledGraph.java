package com.can.aiassistant.graph;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 编译后的图 - 提供优化的执行接口
 */
public class CompiledGraph<T extends GraphState> {
    
    private static final Logger log = LoggerFactory.getLogger(CompiledGraph.class);
    
    private final StateGraph<T> graph;
    
    public CompiledGraph(StateGraph<T> graph) {
        this.graph = graph;
    }
    
    /**
     * 执行图
     */
    public CompletableFuture<T> invoke(T initialState) {
        log.info("🚀 调用编译图执行 - 状态: {}", initialState);
        return graph.execute(initialState);
    }
    
    /**
     * 流式执行图（支持中间状态回调）
     */
    public CompletableFuture<T> stream(T initialState, StateCallback<T> callback) {
        log.info("🌊 流式执行编译图 - 状态: {}", initialState);
        
        return graph.execute(initialState)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    callback.onError(throwable);
                } else {
                    callback.onComplete(result);
                }
            });
    }
    
    /**
     * 获取图信息
     */
    public GraphInfo getInfo() {
        return new GraphInfo(
            graph.getNodes().size(),
            graph.getEdgeCount(),
            graph.getStartNode().getId(),
            graph.getEndNode() != null ? graph.getEndNode().getId() : null
        );
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        graph.shutdown();
    }
    
    /**
     * 状态回调接口
     */
    public interface StateCallback<T extends GraphState> {
        default void onStateChange(String nodeId, T state) {}
        default void onComplete(T finalState) {}
        default void onError(Throwable throwable) {}
    }
    
    /**
     * 图信息
     */
    @Getter
    public static class GraphInfo {
        private final int nodeCount;
        private final int edgeCount;
        private final String startNodeId;
        private final String endNodeId;
        
        public GraphInfo(int nodeCount, int edgeCount, String startNodeId, String endNodeId) {
            this.nodeCount = nodeCount;
            this.edgeCount = edgeCount;
            this.startNodeId = startNodeId;
            this.endNodeId = endNodeId;
        }
        
        // 注意：所有字段的getter已通过@Getter注解自动生成
        
        @Override
        public String toString() {
            return "GraphInfo{" +
                    "nodeCount=" + nodeCount +
                    ", edgeCount=" + edgeCount +
                    ", startNodeId='" + startNodeId + '\'' +
                    ", endNodeId='" + endNodeId + '\'' +
                    '}';
        }
    }
}