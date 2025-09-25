#!/bin/bash

echo "=== AI助手调试测试脚本 ==="
echo ""

# 检查应用是否运行
echo "1. 检查应用健康状态..."
curl -s http://localhost:8081/api/ai/health 2>/dev/null && echo "✅ 应用健康检查通过" || echo "❌ 应用未运行或健康检查失败"
echo ""

# 测试流式聊天
echo "2. 测试流式聊天功能..."
curl -X POST http://localhost:8081/api/stream/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请简单介绍一下自己",
    "saveHistory": true,
    "enableDeepThinking": false
  }' \
  --no-buffer 2>/dev/null || echo "流式聊天测试失败"
echo ""

# 测试普通聊天
echo "3. 测试普通聊天功能..."
response=$(curl -s -X POST http://localhost:8081/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好",
    "saveHistory": true,
    "enableDeepThinking": false
  }' 2>/dev/null)
if [ $? -eq 0 ] && [ -n "$response" ]; then
    echo "✅ 普通聊天测试成功"
    echo "响应: $response"
else
    echo "❌ 普通聊天测试失败"
fi
echo ""

echo "=== 测试完成 ==="
