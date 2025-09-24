package com.can.aiassistant.graph;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 图状态基类 - 在状态图执行过程中传递的状态信息
 */
@Getter
@Setter
public abstract class GraphState {

    protected Map<String, Object> data = new HashMap<>();
    protected long timestamp = System.currentTimeMillis();

    /**
     * 获取状态数据
     */
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        throw new ClassCastException("Cannot cast " + value.getClass() + " to " + type);
    }

    /**
     * 设置状态数据
     */
    public void set(String key, Object value) {
        data.put(key, value);
    }

    /**
     * 检查是否包含某个键
     */
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    /**
     * 移除状态数据
     */
    public void remove(String key) {
        data.remove(key);
    }

    /**
     * 获取所有状态数据
     */
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    /**
     * 创建状态副本
     */
    public abstract GraphState copy();

    /**
     * 合并另一个状态
     */
    public void merge(GraphState other) {
        this.data.putAll(other.data);
    }

    // 注意：timestamp的getter已通过@Getter注解自动生成

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "data=" + data.keySet() +
                ", timestamp=" + timestamp +
                '}';
    }
}

