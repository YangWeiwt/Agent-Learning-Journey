package com.zcy.agentlearning.controller;

import com.zcy.agentlearning.service.ChatService;
import com.zcy.agentlearning.service.AiProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LearningController {

    private final ChatService chatService;

    public LearningController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "application", "Agent Learning Journey"
        );
    }

    @GetMapping("/lessons")
    public List<LessonSummary> lessons() {
        return List.of(
                new LessonSummary(
                        0,
                        "大模型基础概念",
                        "理解 Token、Prompt、上下文窗口、消息角色和生成参数",
                        "00-大模型基础概念.md"
                ),
                new LessonSummary(
                        1,
                        "普通模型调用",
                        "理解 AiController、LoveApp、ChatClient 和大模型之间的调用链",
                        "01-普通模型调用.md"
                ),
                new LessonSummary(
                        2,
                        "模型接入与配置",
                        "掌握 API Key、模型选择、Base URL、超时和常见故障排查",
                        "02-模型接入与配置.md"
                ),
                new LessonSummary(
                        3,
                        "Spring AI 核心对象",
                        "理解 ChatModel、ChatClient、ChatResponse 与 Starter 自动配置",
                        "03-Spring-AI-核心对象.md"
                )
        );
    }

    @PostMapping("/chat")
    public ChatReply chat(@Valid @RequestBody ChatRequest request) {
        AiProvider provider = request.provider() == null ? AiProvider.DEEPSEEK : request.provider();
        String content = chatService.chat(request.message(), request.chatId(), provider);
        return new ChatReply(provider, request.chatId(), content);
    }

    public record LessonSummary(int order, String title, String description, String document) {
    }

    public record ChatRequest(
            @NotBlank(message = "message 不能为空") String message,
            @NotBlank(message = "chatId 不能为空") String chatId,
            AiProvider provider
    ) {
    }

    public record ChatReply(AiProvider provider, String chatId, String content) {
    }
}
