# 03｜Spring AI 核心对象

这篇笔记梳理 Spring AI 中最常用的三个对象：`ChatModel`、`ChatClient` 和 `ChatResponse`，并说明 Spring Boot Starter 如何根据配置自动创建模型 Bean。

本仓库可运行代码使用 Spring AI `1.0.0` 的 OpenAI 兼容客户端，手动注册 DeepSeek 和 MiniMax 两个 `OpenAiChatModel`，再由应用层按 `provider` 路由。本文还保留 `ai-agent-demo` 使用 Spring AI Alibaba `1.0.0.2` 和 `DashScopeChatModel` 的案例；不同 Provider 都实现同一个 `ChatModel` 接口。升级框架时应重新核对自动配置条件、Bean 名称和 API 变化。

## 一张图理解对象关系

```text
application.yml / 环境变量
            │
            ▼
Spring Boot Starter 自动配置
            │ 创建
            ▼
ChatModel Bean（OpenAiChatModel 或 DashScopeChatModel）
            ▲
            │ 持有并调用
ChatClient ─┼─ 组织 system/user 消息、Options、Advisor、Tools
            │
            ▼
          Prompt
            │
            ▼
模型平台请求 → DeepSeek / MiniMax / 百炼 → 模型平台响应
            │
            ▼
       ChatResponse
            │
            ├─ Generation / AssistantMessage：回答、工具调用
            └─ Metadata：Token 用量、模型信息、结束原因等
```

最简职责划分：

| 对象 | 一句话职责 |
|---|---|
| `ChatModel` | 用统一 Java 接口屏蔽不同模型厂商的调用差异 |
| `ChatClient` | 以链式 API 组织 Prompt、Advisor、Tools，并调用 `ChatModel` |
| `ChatResponse` | 承载标准化后的完整模型响应，而不只是回答文本 |
| Starter 自动配置 | 根据依赖和配置创建厂商客户端与 `ChatModel` Bean |

## ChatModel：统一的模型抽象

`ChatModel` 是 Spring AI 面向聊天模型定义的核心接口。1.0 系列中的核心结构可以简化为：

```java
public interface ChatModel extends Model<Prompt, ChatResponse>, StreamingChatModel {

    default String call(String message) { ... }

    ChatResponse call(Prompt prompt);
}
```

它统一了模型调用的输入和输出：

```text
输入：Prompt
输出：ChatResponse
```

不同 Provider 提供各自实现，例如：

```text
DashScopeChatModel
OpenAiChatModel
OllamaChatModel
AnthropicChatModel
```

业务代码面向 `ChatModel` 接口编程，而不是直接依赖某个厂商 HTTP 请求结构。切换 Provider 时，Controller 和应用服务的主要调用方式可以保持一致，但模型能力、Options 和响应元数据并不会因此完全相同。

### ChatModel 负责什么

- 接收 Spring AI 标准的 `Prompt`。
- 将消息和 Options 转换为厂商请求格式。
- 调用厂商 API 或本地模型服务。
- 将厂商响应转换成标准 `ChatResponse`。
- 暴露同步调用和流式调用能力。

### ChatModel 不负责什么

- 不负责 HTTP Controller 参数绑定。
- 不自动维护业务会话和长期记忆。
- 不负责业务 Prompt 模板的完整组织流程。
- 不保证所有 Provider 支持相同的工具、模态和参数。
- 不应该承载恋爱助手、客服助手等具体业务规则。

### 直接调用 ChatModel

项目中的 `SpringAiAiInvoke` 直接注入并调用模型：

```java
@Resource
private ChatModel dashscopeChatModel;

public void chat() {
    AssistantMessage message = dashscopeChatModel
            .call(new Prompt("你好，我是鱼皮"))
            .getResult()
            .getOutput();
}
```

这种方式适合理解底层调用、编写基础设施代码或需要精确控制 `Prompt` 的场景。复杂业务通常更适合使用 `ChatClient`。

## ChatClient：面向应用的调用门面

`ChatClient` 是构建在 `ChatModel` 之上的高层链式 API。它负责把应用层需要的内容组装成 `Prompt`，再把请求交给底层 `ChatModel`。

常见能力包括：

- 添加 system、user 等消息；
- 设置默认 Prompt 和单次 Prompt；
- 传入模型 Options；
- 使用 Prompt 模板和变量；
- 配置 Chat Memory、RAG 等 Advisor；
- 注册 Tool Calling 工具；
- 选择同步或流式调用；
- 返回文本、完整响应或结构化 Java 对象。

### 创建 ChatClient

可以基于指定的模型创建：

```java
ChatClient chatClient = ChatClient.builder(chatModel)
        .defaultSystem("你是一个 Java 学习助手")
        .build();
```

也可以注入 Spring AI 自动配置的 Builder：

```java
public LearningService(ChatClient.Builder builder) {
    this.chatClient = builder.build();
}
```

当前 `LoveApp` 采用第一种方式：构造器接收 `ChatModel`，再显式创建专用 `ChatClient`。

```java
public LoveApp(ChatModel dashscopeChatModel) {
    chatClient = ChatClient.builder(dashscopeChatModel)
            .defaultSystem(SYSTEM_PROMPT)
            .defaultAdvisors(
                    MessageChatMemoryAdvisor.builder(chatMemory).build(),
                    new MyLoggerAdvisor()
            )
            .build();
}
```

这段代码表达了三个层次：

1. `dashscopeChatModel` 决定请求最终发给哪个模型 Provider。
2. `ChatClient` 保存应用级默认 system Prompt 和 Advisor。
3. 每次调用再提供用户消息、会话 ID 和单次 Options。

### 一次调用怎样形成 Prompt

项目中的同步调用：

```java
ChatResponse chatResponse = chatClient
        .prompt()
        .user(message)
        .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
        .call()
        .chatResponse();
```

可以理解为：

```text
prompt()
  → 创建本次 Prompt 构建过程
user(message)
  → 加入本轮 UserMessage
advisors(...)
  → 让 Chat Memory 根据 chatId 加入历史消息
call()
  → 选择同步调用路径
chatResponse()
  → 执行调用并返回完整 ChatResponse
```

在 Spring AI 1.0 的 `ChatClient` API 中，`.call()` 主要选择同步调用规格，真正触发并取得结果的是后面的终止方法，例如 `.content()`、`.chatResponse()` 或 `.entity()`。

### 常见返回方式

```java
// 只要回答文本
String text = chatClient.prompt()
        .user("你好")
        .call()
        .content();

// 需要回答和元数据
ChatResponse response = chatClient.prompt()
        .user("你好")
        .call()
        .chatResponse();

// 流式文本
Flux<String> stream = chatClient.prompt()
        .user("你好")
        .stream()
        .content();
```

选择原则：

| 需求 | 返回方式 |
|---|---|
| 页面只展示最终文本 | `.call().content()` |
| 需要 Token、工具调用、结束原因 | `.call().chatResponse()` |
| 需要转换为 Java 对象 | `.call().entity(...)` |
| 需要逐段推送到前端 | `.stream().content()` |
| 流式过程中还需完整元数据 | `.stream().chatResponse()` |

## ChatResponse：完整模型响应

`ChatResponse` 不是简单的字符串包装。它统一承载模型生成结果和响应级元数据，核心结构可以简化为：

```text
ChatResponse
├── List<Generation> results
│   └── Generation
│       ├── AssistantMessage output
│       │   ├── text
│       │   └── toolCalls
│       └── ChatGenerationMetadata
└── ChatResponseMetadata metadata
    ├── usage
    ├── model
    ├── id
    └── provider-specific metadata
```

对应的核心 API 结构是：

```java
public class ChatResponse implements ModelResponse<Generation> {
    private final ChatResponseMetadata chatResponseMetadata;
    private final List<Generation> generations;
}
```

### 取得回答文本

当前项目使用：

```java
String text = chatResponse
        .getResult()
        .getOutput()
        .getText();
```

其中：

- `getResult()`：取得一个主要 `Generation`。
- `getOutput()`：取得该结果中的 `AssistantMessage`。
- `getText()`：取得助手文本。

如果 Provider 返回多个候选结果，应使用 `getResults()` 遍历，而不是只读取 `getResult()`。

### 取得 Token 用量

```java
var usage = chatResponse.getMetadata().getUsage();

Integer promptTokens = usage.getPromptTokens();
Integer completionTokens = usage.getCompletionTokens();
Integer totalTokens = usage.getTotalTokens();
```

不同 Spring AI 小版本的具体方法名可能变化，应以当前依赖的 JavaDoc 为准。Provider 没有返回某项数据时，Spring AI 也无法凭空补齐完整元数据，因此业务代码要容忍空值或 `Usage.NULL`。

Token 用量适合用于：

- 请求成本统计；
- 监控异常长 Prompt；
- 按用户或会话实施配额；
- 比较不同模型的成本和效率；
- 排查上下文持续膨胀的问题。

### ChatResponse 与 ChatClientResponse 的区别

不要混淆两个名称相近的对象：

| 对象 | 内容 |
|---|---|
| `ChatResponse` | 模型生成结果和模型响应元数据 |
| `ChatClientResponse` | `ChatResponse` 加上 ChatClient/Advisor 执行上下文 |

例如 RAG 场景除了模型答案，还可能需要取得 Advisor 检索到的文档，此时 `ChatClientResponse` 比单纯的 `ChatResponse` 更合适。

## Starter 如何自动创建 ChatModel Bean

Starter 是一组“依赖 + 自动配置 + 配置属性”的组合。当前项目引入：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
</dependency>
```

同时在 `application.yml` 中提供：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
```

Spring Boot 启动时的过程可以简化为：

```text
1. Maven 将 Starter 和 DashScope 相关类放入 classpath
2. Spring Boot 发现 Starter 声明的 AutoConfiguration
3. @ConfigurationProperties 绑定 spring.ai.dashscope.*
4. 自动配置检查 classpath、开关、配置和现有 Bean
5. 创建 DashScope API 客户端及默认 Chat Options
6. 创建 DashScopeChatModel Bean
7. Spring AI 基于 ChatModel 提供 ChatClient.Builder Bean
8. LoveApp 等组件通过依赖注入取得 ChatModel
```

其中，配置与对象的对应关系大致是：

| 配置 | 自动配置中的用途 |
|---|---|
| `spring.ai.dashscope.api-key` | 构造并鉴权 DashScope 客户端 |
| `spring.ai.dashscope.base-url` | 覆盖模型服务地址，未配置时使用默认值 |
| `spring.ai.dashscope.chat.options.model` | 创建默认 Chat Options 时选择模型 |
| temperature、max tokens 等 | 作为模型启动时默认 Options |

### 自动配置通常带有条件

Starter 不会在所有情况下无条件创建 Bean。典型条件包括：

- 相关类存在于 classpath；
- 对应模型功能没有被关闭；
- 配置项满足创建条件；
- 用户没有声明需要优先使用的同类型 Bean。

这些条件通常由 `@ConditionalOnClass`、`@ConditionalOnProperty` 和 `@ConditionalOnMissingBean` 等注解表达。具体条件及 Bean 名称由所用 Starter 版本决定。

### 为什么代码中没有 new DashScopeChatModel

因为 Starter 已在自动配置中完成实例化并注册到 Spring 容器。业务代码只声明依赖：

```java
public LoveApp(ChatModel dashscopeChatModel) {
}
```

Spring 在创建 `LoveApp` 时，从容器查找兼容的 `ChatModel` Bean 并注入。这就是“依赖配置和 Starter 自动创建模型 Bean”的含义。

### 配置错误会在哪一层失败

| 问题 | 常见失败阶段 |
|---|---|
| 没有引入 Starter | 容器中没有对应 `ChatModel`，依赖注入失败 |
| API Key 属性完全缺失 | 自动配置创建模型 Bean 时可能直接启动失败 |
| API Key 格式错误 | Bean 可能创建成功，首次远程调用返回 401 |
| 模型名错误或无权限 | 首次调用返回 404、400 或 403 |
| Base URL 不可达 | 首次调用出现连接、DNS 或超时错误 |
| 同时存在多个 `ChatModel` | 按类型注入时出现候选 Bean 歧义 |
| 用户自定义同类型 Bean | 自动配置可能回退，由自定义 Bean 接管 |

“应用启动成功”只能说明本地 Bean 大概率创建成功，不代表 API Key、网络、模型名和远程权限已经验证成功。模型接入仍需要一次真实的健康调用。

## 启动配置与单次请求配置

模型配置分为两个层次：

```text
application.yml
  → 启动时默认 Chat Options

Prompt / ChatClient.options(...)
  → 当前请求的运行时 Options
```

例如默认使用 `qwen-plus`，某次请求临时设置不同 temperature：

```java
ChatResponse response = chatClient.prompt()
        .user("生成三个标题")
        .options(DashScopeChatOptions.builder()
                .withTemperature(0.8)
                .build())
        .call()
        .chatResponse();
```

运行时 Options 通常覆盖启动时默认值。不要为了单个请求临时需求修改全局 `application.yml`。

## 当前项目的完整对象链

以 `LoveApp.doChat` 为例：

```text
application.yml
  │ api-key、model=qwen-plus
  ▼
Spring AI Alibaba AutoConfiguration
  ▼
DashScopeChatModel Bean
  ▼ 注入
LoveApp 构造器
  ▼
ChatClient.builder(dashscopeChatModel)
  │ defaultSystem
  │ MessageChatMemoryAdvisor
  │ MyLoggerAdvisor
  ▼
LoveApp.doChat(message, chatId)
  ▼
Prompt（系统消息 + 历史消息 + 用户消息 + Options）
  ▼
DashScopeChatModel.call(prompt)
  ▼
百炼 qwen-plus
  ▼
ChatResponse
  ▼
Generation → AssistantMessage → text
  ▼
Controller 返回 String
```

## 常见误区

| 误区 | 正确认识 |
|---|---|
| `ChatClient` 就是具体厂商的 HTTP 客户端 | 它是基于 `ChatModel` 的应用层链式门面 |
| `ChatModel` 只返回字符串 | 标准调用返回 `ChatResponse`，字符串只是便捷形式 |
| `ChatResponse` 只包含回答 | 它还可包含多个 Generation、工具调用和响应元数据 |
| 引入 Starter 就一定能调用成功 | 还需要正确 Key、模型权限、网络和 Base URL |
| application.yml 直接创建了对象 | 配置先绑定为属性，再由 AutoConfiguration 创建 Bean |
| 切换模型只要改 Java 类型 | 同一 Provider 内常改 Model ID；跨 Provider 还要更换 Starter 和配置 |
| 注入参数名可以永远区分多个模型 | 更稳妥的是使用 `@Qualifier` 或显式定义具名 `ChatClient` |

## 自测问题

1. `ChatModel` 的标准输入和输出分别是什么？
2. 为什么业务应用通常使用 `ChatClient`，而不是到处直接调用 `ChatModel`？
3. `.call().content()` 与 `.call().chatResponse()` 的使用场景有什么区别？
4. `Generation` 和 `AssistantMessage` 分别位于响应结构的哪一层？
5. API Key 缺失和 API Key 无效为什么可能在不同阶段失败？
6. Starter 创建 `ChatModel` Bean 通常依赖哪些条件？
7. 为什么加入第二个模型 Starter 后，原有构造器注入可能失败？
8. 全局默认 Options 与单次请求 Options 应分别放在哪里？

## 参考资料

- [Spring AI 1.0：Chat Model API](https://docs.spring.io/spring-ai/reference/1.0/api/chatmodel.html)
- [Spring AI 1.0：Chat Client API](https://docs.spring.io/spring-ai/reference/1.0/api/chatclient.html)
- [Spring AI 1.0：AI Metadata](https://docs.spring.io/spring-ai/reference/1.0/api/aimetadata.html)
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)
