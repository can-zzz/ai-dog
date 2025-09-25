# 🐕 快乐小狗

基于Spring Boot构建的快乐小狗，支持多轮对话、深度思考、上下文理解等功能。

## 功能特性

- 🤖 **智能对话** - 基于通义千问模型的自然语言交互
- 🧠 **深度思考** - AI在回答前进行结构化的深度思考分析
- 💬 **多轮对话** - 支持上下文记忆和连续对话
- 📱 **响应式UI** - 现代化的聊天界面，可视化思考过程
- 🔧 **RESTful API** - 完整的API接口
- 📊 **会话管理** - 历史记录和统计功能

## 技术栈

- **后端**: Spring Boot 3.2.3
- **AI模型**: 阿里云通义千问 (Qwen)
- **前端**: Bootstrap 5, JavaScript
- **构建工具**: Maven
- **Java版本**: 17

## 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+
- 阿里云DashScope API Key

### 2. 配置API Key

编辑 `src/main/resources/application.yml` 文件，替换你的API Key：

```yaml
ai:
  api-key: your-api-key-here
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

或者：

```bash
mvn clean package
java -jar target/ai-assistant-0.0.1-SNAPSHOT.jar
```

### 4. 访问应用

- **主页**: http://localhost:8081
- **聊天界面**: http://localhost:8081/chat
- **健康检查**: http://localhost:8081/api/ai/health

## API 接口

### 聊天接口

```bash
POST /api/ai/chat
Content-Type: application/json

{
  "message": "请解释一下量子计算的基本原理",
  "sessionId": "session-123",
  "saveHistory": true,
  "enableDeepThinking": true
}
```

### 深度思考功能

启用深度思考时，AI会按照以下步骤进行结构化分析：

1. **分析问题** - 识别关键信息和潜在需求
2. **搜集信息** - 基于已有知识搜集相关背景
3. **推理思考** - 运用逻辑推理，多角度思考
4. **综合整理** - 整合信息，形成完整思路
5. **验证答案** - 检查准确性、完整性和实用性

响应将包含 `thinkingSteps` 字段，展示完整的思考过程。

### 历史记录

```bash
# 获取历史记录
GET /api/ai/history/{sessionId}

# 清除历史记录
DELETE /api/ai/history/{sessionId}
```

## 项目结构

```
ai-intervire/
├── src/main/java/com/can/aiassistant/
│   ├── config/           # 配置类
│   ├── controller/       # 控制器
│   ├── dto/             # 数据传输对象
│   ├── exception/       # 异常处理
│   ├── service/         # 业务服务
│   └── AiAssistantApplication.java
├── src/main/resources/
│   ├── templates/       # Thymeleaf模板
│   └── application.yml  # 配置文件
└── pom.xml
```

## 配置说明

### application.yml

```yaml
ai:
  api-key: ${DASHSCOPE_API_KEY:your-api-key}
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  model: qwen-turbo
  temperature: 0.7
  max-tokens: 2000
  system-prompt: "你是一个智能助手，能够回答各种问题并提供帮助。请用中文回答。"
  deep-thinking:
    enabled: true
    thinking-model: qwen-max
    max-thinking-steps: 5
    thinking-prompt: |
      你是一个具有深度思考能力的AI助手。请按照以下步骤进行深入思考：
      1. 【分析问题】：仔细分析用户的问题，识别关键信息和潜在需求
      2. 【搜集信息】：基于已有知识，搜集相关的背景信息和事实
      3. 【推理思考】：运用逻辑推理，从多个角度思考问题
      4. 【综合整理】：整合所有信息，形成完整的思路
      5. 【验证答案】：检查答案的准确性、完整性和实用性

server:
  port: 8081
```

## 使用说明

1. 启动应用后访问 http://localhost:8081
2. 点击"开始聊天"进入聊天界面
3. 开启右上角的"深度思考"开关（可选）
4. 在输入框中输入问题，按回车或点击发送按钮
5. 如果开启了深度思考，AI会显示详细的思考过程
6. AI助手会根据上下文和思考结果给出回答
7. 可以点击"清除历史"清空对话记录

### 深度思考模式说明

- **普通模式**：直接回答问题，响应速度快
- **深度思考模式**：AI会展示完整的思考过程，包括问题分析、信息搜集、推理思考、综合整理和答案验证等步骤
- 深度思考模式适合复杂问题、学术讨论、决策分析等场景
- 可以随时切换模式，不影响对话历史

## 故障排除

### 常见问题

1. **API Key 错误**: 检查 `application.yml` 中的API Key配置
2. **端口冲突**: 修改 `server.port` 配置
3. **网络连接问题**: 检查网络连接和防火墙设置

### 日志配置

```yaml
logging:
  level:
    com.can.aiassistant: DEBUG
```

## 许可证

MIT License