package com.can.aiassistant.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * LangGraph风格的状态图执行引擎
 * 支持DAG图的状态管理、并行执行和条件分支
 */
public class StateGraph<T extends GraphState> {
    
    private static final Logger log = LoggerFactory.getLogger(StateGraph.class);
    
    private final Map<String, StateNode<T>> nodes;
    private final Map<StateNode<T>, List<EdgeInfo<T>>> edges;
    private final ExecutorService executor;
    private StateNode<T> startNode;
    private StateNode<T> endNode;
    
    public StateGraph() {
        this.nodes = new HashMap<>();
        this.edges = new HashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * 边信息
     */
    private static class EdgeInfo<T extends GraphState> {
        private final StateNode<T> target;
        private final Predicate<T> condition;
        
        public EdgeInfo(StateNode<T> target, Predicate<T> condition) {
            this.target = target;
            this.condition = condition;
        }
        
        public StateNode<T> getTarget() { return target; }
        public Predicate<T> getCondition() { return condition; }
    }
    
    /**
     * 添加状态节点
     */
    public StateGraph<T> addNode(String nodeId, StateFunction<T> function) {
        StateNode<T> node = new StateNode<>(nodeId, function);
        nodes.put(nodeId, node);
        edges.put(node, new ArrayList<>());
        log.debug("添加状态节点: {}", nodeId);
        return this;
    }
    
    /**
     * 添加条件边
     */
    public StateGraph<T> addConditionalEdge(String fromNodeId, String toNodeId, 
                                           Predicate<T> condition) {
        StateNode<T> fromNode = nodes.get(fromNodeId);
        StateNode<T> toNode = nodes.get(toNodeId);
        
        if (fromNode == null || toNode == null) {
            throw new IllegalArgumentException("节点不存在: " + fromNodeId + " -> " + toNodeId);
        }
        
        edges.get(fromNode).add(new EdgeInfo<>(toNode, condition));
        log.debug("添加条件边: {} -> {} (条件: {})", fromNodeId, toNodeId, condition);
        return this;
    }
    
    /**
     * 添加普通边
     */
    public StateGraph<T> addEdge(String fromNodeId, String toNodeId) {
        return addConditionalEdge(fromNodeId, toNodeId, state -> true);
    }
    
    /**
     * 设置开始节点
     */
    public StateGraph<T> setEntryPoint(String nodeId) {
        this.startNode = nodes.get(nodeId);
        if (this.startNode == null) {
            throw new IllegalArgumentException("开始节点不存在: " + nodeId);
        }
        log.debug("设置开始节点: {}", nodeId);
        return this;
    }
    
    /**
     * 设置结束节点
     */
    public StateGraph<T> setFinishPoint(String nodeId) {
        this.endNode = nodes.get(nodeId);
        if (this.endNode == null) {
            throw new IllegalArgumentException("结束节点不存在: " + nodeId);
        }
        log.debug("设置结束节点: {}", nodeId);
        return this;
    }
    
    /**
     * 编译图（验证图的有效性）
     */
    public CompiledGraph<T> compile() {
        validateGraph();
        log.info("状态图编译完成，包含 {} 个节点", nodes.size());
        return new CompiledGraph<>(this);
    }
    
    /**
     * 执行状态图
     */
    public CompletableFuture<T> execute(T initialState) {
        log.info("🎯 开始执行状态图 - 初始状态: {}", initialState.getClass().getSimpleName());
        return executeNode(startNode, initialState);
    }
    
    /**
     * 执行单个节点
     */
    private CompletableFuture<T> executeNode(StateNode<T> node, T state) {
        log.info("📍 执行状态节点: {}", node.getId());
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 执行节点函数
                T newState = node.getFunction().apply(state);
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ 节点执行完成: {} - 耗时: {}ms", node.getId(), duration);
                
                return newState;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("❌ 节点执行失败: {} - 耗时: {}ms, 错误: {}", 
                    node.getId(), duration, e.getMessage());
                throw new RuntimeException("节点执行失败: " + node.getId(), e);
            }
        }, executor).thenCompose(newState -> {
            // 查找下一个节点
            StateNode<T> nextNode = findNextNode(node, newState);
            
            if (nextNode == null || nextNode.equals(endNode)) {
                log.info("🏁 状态图执行完成");
                return CompletableFuture.completedFuture(newState);
            }
            
            // 递归执行下一个节点
            return executeNode(nextNode, newState);
        });
    }
    
    /**
     * 查找下一个要执行的节点
     */
    private StateNode<T> findNextNode(StateNode<T> currentNode, T state) {
        List<EdgeInfo<T>> outgoingEdges = edges.get(currentNode);
        
        if (outgoingEdges != null) {
            for (EdgeInfo<T> edgeInfo : outgoingEdges) {
                if (edgeInfo.getCondition().test(state)) {
                    StateNode<T> targetNode = edgeInfo.getTarget();
                    log.debug("🔄 状态转换: {} -> {}", currentNode.getId(), targetNode.getId());
                    return targetNode;
                }
            }
        }
        
        log.debug("🛑 没有找到下一个节点，执行结束");
        return null;
    }
    
    /**
     * 验证图的有效性
     */
    private void validateGraph() {
        if (startNode == null) {
            throw new IllegalStateException("必须设置开始节点");
        }
        
        if (nodes.isEmpty()) {
            throw new IllegalStateException("图中必须至少有一个节点");
        }
        
        // 检查是否有孤立节点
        for (StateNode<T> node : nodes.values()) {
            if (!edges.containsKey(node)) {
                log.warn("⚠️ 发现孤立节点: {}", node.getId());
            }
        }
        
        log.debug("✅ 图验证通过");
    }
    
    /**
     * 获取图的拓扑排序
     */
    public List<StateNode<T>> getTopologicalOrder() {
        // 简化实现：返回节点列表（可以后续优化为真正的拓扑排序）
        return new ArrayList<>(nodes.values());
    }
    
    /**
     * 关闭执行器
     */
    public void shutdown() {
        executor.shutdown();
        log.info("状态图执行器已关闭");
    }
    
    // Getters
    public Map<String, StateNode<T>> getNodes() { return nodes; }
    public Map<StateNode<T>, List<EdgeInfo<T>>> getEdges() { return edges; }
    public StateNode<T> getStartNode() { return startNode; }
    public StateNode<T> getEndNode() { return endNode; }
    
    /**
     * 获取边的数量
     */
    public int getEdgeCount() {
        return edges.values().stream().mapToInt(List::size).sum();
    }
}
