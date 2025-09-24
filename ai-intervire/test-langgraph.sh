#!/bin/bash

# LangGraph集成测试脚本

echo "🚀 开始测试LangGraph集成..."

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 10

# 测试1: 检查工作流信息
echo ""
echo "📊 测试1: 获取StateGraph工作流信息"
curl -s "http://localhost:8081/api/stream/workflow-info" | jq '.' || echo "JSON解析失败，原始响应："

# 测试2: 传统模式测试
echo ""
echo "📋 测试2: 传统模式聊天测试"
curl -X POST "http://localhost:8081/api/stream/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，这是传统模式测试",
    "sessionId": "traditional-test-001",
    "enableDeepThinking": false,
    "saveHistory": true
  }' \
  --max-time 30 \
  | head -n 10

# 测试3: StateGraph模式测试  
echo ""
echo "🎯 测试3: StateGraph模式聊天测试"
curl -X POST "http://localhost:8081/api/stream/chat-graph" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，这是StateGraph模式测试",
    "sessionId": "graph-test-001", 
    "enableDeepThinking": false,
    "saveHistory": true
  }' \
  --max-time 30 \
  | head -n 10

# 测试4: StateGraph深度思考测试
echo ""
echo "🧠 测试4: StateGraph深度思考测试"
curl -X POST "http://localhost:8081/api/stream/chat-graph" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请解释机器学习和深度学习的区别",
    "sessionId": "graph-deep-test-001",
    "enableDeepThinking": true,
    "saveHistory": true
  }' \
  --max-time 60 \
  | head -n 20

echo ""
echo "✅ LangGraph集成测试完成！"
echo ""
echo "📝 如需查看完整日志，请检查应用日志"
echo "🌐 访问 http://localhost:8081/graph-test 进行Web界面测试"
