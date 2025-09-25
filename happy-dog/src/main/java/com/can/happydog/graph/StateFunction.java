package com.can.happydog.graph;

/**
 * 状态函数接口 - 定义状态节点的执行逻辑
 */
@FunctionalInterface
public interface StateFunction<T extends GraphState> {
    
    /**
     * 执行状态函数
     * 
     * @param state 输入状态
     * @return 输出状态
     * @throws Exception 执行过程中的异常
     */
    T apply(T state) throws Exception;
}
