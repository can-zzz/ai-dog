#!/bin/bash

# AI智能助手启动脚本

echo "==================================="
echo "启动AI智能助手..."
echo "==================================="

# 检查Java版本
echo "Java版本:"
java -version

echo ""
echo "正在启动应用..."
echo "访问地址: http://localhost:8081"
echo "按 Ctrl+C 停止应用"
echo ""

# 启动应用
mvn spring-boot:run