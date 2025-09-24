package com.can.aiassistant.graph;

import lombok.Getter;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 状态边 - 连接两个状态节点的边，包含转换条件
 */
@Getter
public class StateEdge {
    
    private final Predicate<? extends GraphState> condition;
    private final String label;
    
    public StateEdge() {
        this(state -> true, "default");
    }
    
    public StateEdge(Predicate<? extends GraphState> condition) {
        this(condition, "conditional");
    }
    
    public StateEdge(Predicate<? extends GraphState> condition, String label) {
        this.condition = Objects.requireNonNull(condition, "条件不能为空");
        this.label = Objects.requireNonNull(label, "标签不能为空");
    }
    
    /**
     * 检查是否满足转换条件
     */
    @SuppressWarnings("unchecked")
    public boolean test(GraphState state) {
        try {
            return ((Predicate<GraphState>) condition).test(state);
        } catch (ClassCastException e) {
            // 如果类型不匹配，默认不满足条件
            return false;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateEdge stateEdge = (StateEdge) o;
        return Objects.equals(condition, stateEdge.condition) && 
               Objects.equals(label, stateEdge.label);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(condition, label);
    }
    
    @Override
    public String toString() {
        return "StateEdge{" +
                "label='" + label + '\'' +
                '}';
    }
}