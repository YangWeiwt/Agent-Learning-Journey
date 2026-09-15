# Agent Learning Journey

一个用于学习和实践 AI Agent 开发的 Spring Boot 项目。项目采用 Java 21、Maven 和 Spring Boot 3.4.4，并通过持续增加学习笔记与示例代码记录完整的学习过程。

## 当前内容

- [00｜大模型基础概念](./00-大模型基础概念.md)：理解大模型、Token、Prompt、上下文窗口、消息角色、对话状态和基础生成参数。
- [01｜普通模型调用](./01-普通模型调用.md)：理解 Controller、应用层、Spring AI 客户端和大模型之间的调用链。
- [02｜模型接入与配置](./02-模型接入与配置.md)：掌握 API Key、模型选择、Base URL、超时和鉴权失败排查。
- [03｜Spring AI 核心对象](./03-Spring-AI-核心对象.md)：理解 ChatModel、ChatClient、ChatResponse 和 Starter 自动配置模型 Bean 的过程。
- `GET /api/health`：检查应用是否成功启动。
- `GET /api/lessons`：查看当前已有的学习章节。
- `POST /api/chat`：使用 Spring AI 调用 DeepSeek 或 MiniMax，并通过 `chatId` 维护最近 20 条消息的 Provider 隔离会话记忆。

## 项目结构

```text
Agent-Learning-Journey
├── 00-大模型基础概念.md
├── 01-普通模型调用.md
├── 02-模型接入与配置.md
├── 03-Spring-AI-核心对象.md
├── src/main/java
│   └── com/zcy/agentlearning
│       ├── AgentLearningJourneyApplication.java
│       ├── config
│       │   ├── AiModelConfiguration.java
│       │   └── AiProviderProperties.java
│       ├── controller/LearningController.java
│       └── service
│           ├── AiProvider.java
│           ├── ChatService.java
│           └── LearningChatService.java
├── src/main/resources
│   └── application.yml
├── src/test/java
│   └── com/zcy/agentlearning/controller/LearningControllerTest.java
├── assets
│   ├── 01-controller-to-model.svg
│   └── 01-controller-to-model.png
└── pom.xml
```

## 环境要求

- JDK 21
- Maven 3.9+

确认环境：

```bash
java -version
mvn -version
```

## 启动项目

按需提供 DeepSeek、MiniMax 官方 API Key。至少配置一个；请求未指定 `provider` 时默认使用 DeepSeek：

```bash
export DEEPSEEK_API_KEY="你的 API Key"
export DEEPSEEK_MODEL="deepseek-v4-flash"

export MINIMAX_API_KEY="你的 API Key"
export MINIMAX_MODEL="MiniMax-M2.7"
```

```bash
cd /Users/zcy/IdeaProjects/agent-workspace/Agent-Learning-Journey
mvn spring-boot:run
```

启动成功后访问：

```text
http://localhost:8080/api/health
http://localhost:8080/api/lessons
```

调用 DeepSeek：

```bash
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"provider":"deepseek","message":"ChatClient 和 ChatModel 有什么区别？","chatId":"lesson-03"}'
```

调用 MiniMax：

```bash
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"provider":"minimax","message":"解释多模型路由","chatId":"lesson-03"}'
```

`provider` 仅支持 `deepseek` 和 `minimax`，不传时默认 `deepseek`。同一个 `chatId` 在两个 Provider 下也使用相互隔离的历史；未配置所选 Provider 的 API Key 时接口返回 `503`。API Key 只从环境变量读取，不要写入或提交到配置文件。

## 运行测试

```bash
mvn test
```

## 学习路线

```text
大模型基础概念
→ 普通模型调用
→ 模型接入与配置
→ Spring AI 核心对象
→ 多轮对话与 Chat Memory
→ SSE 流式输出
→ Tool Calling
→ RAG
→ MCP
→ ReAct Agent
→ 安全与工程化
```
