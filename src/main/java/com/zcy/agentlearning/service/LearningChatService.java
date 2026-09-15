package com.zcy.agentlearning.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Service
public class LearningChatService implements ChatService {

    private static final String SYSTEM_PROMPT = """
            你是一个 AI Agent 开发学习助手。
            使用清晰、准确的中文回答，先给结论，再解释关键概念；涉及代码时给出可运行的 Java 示例。
            不确定的内容要明确说明，不要编造 API、类名或配置项。
            """;

    private final Map<AiProvider, ChatClient> chatClients;

    public LearningChatService(
            @Qualifier("providerChatModels") Map<AiProvider, ChatModel> chatModels
    ) {
        Map<AiProvider, ChatClient> clients = new EnumMap<>(AiProvider.class);
        chatModels.forEach((provider, model) -> clients.put(provider, createChatClient(model)));
        this.chatClients = Collections.unmodifiableMap(clients);
    }

    private ChatClient createChatClient(ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Override
    public String chat(String message, String chatId, AiProvider provider) {
        ChatClient chatClient = chatClients.get(provider);
        if (chatClient == null) {
            throw new ProviderUnavailableException(provider);
        }

        ChatResponse response = chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("模型未返回有效结果");
        }
        return response.getResult().getOutput().getText();
    }
}
