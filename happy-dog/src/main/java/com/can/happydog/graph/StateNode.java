package com.can.happydog.graph;

import lombok.Getter;
import java.util.Objects;

/**
 * 状态节点 - 图中的一个执行单元
 */
@Getter
public class StateNode<T extends GraphState> {
    
    private final String id;
    private final StateFunction<T> function;
    private final NodeType type;
    
    public StateNode(String id, StateFunction<T> function) {
        this(id, function, NodeType.NORMAL);
    }
    
    public StateNode(String id, StateFunction<T> function, NodeType type) {
        this.id = Objects.requireNonNull(id, "节点ID不能为空");
        this.function = Objects.requireNonNull(function, "节点函数不能为空");
        this.type = Objects.requireNonNull(type, "节点类型不能为空");
    }
    
    /**
     * 节点类型
     */
    public enum NodeType {
        START("开始节点"),
        NORMAL("普通节点"),
        CONDITIONAL("条件节点"),
        PARALLEL("并行节点"),
        MERGE("合并节点"),
        END("结束节点");
        
        @Getter
        private final String description;
        
        NodeType(String description) {
            this.description = description;
        }
    }
    
    // 手动添加getter方法（Lombok注解可能需要重新编译）
    public String getId() { return id; }
    public StateFunction<T> getFunction() { return function; }
    public NodeType getType() { return type; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateNode<?> stateNode = (StateNode<?>) o;
        return Objects.equals(id, stateNode.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "StateNode{" +
                "id='" + id + '\'' +
                ", type=" + type +
                '}';
    }
}
