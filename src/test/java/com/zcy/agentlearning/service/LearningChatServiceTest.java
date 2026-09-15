package com.zcy.agentlearning.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningChatServiceTest {

    @Test
    void chatReturnsModelContentAndCarriesHistoryForSameChatId() {
        RecordingChatModel model = new RecordingChatModel("你好，小明", "你刚才说你叫小明");
        LearningChatService service = new LearningChatService(Map.of(AiProvider.DEEPSEEK, model));

        assertEquals("你好，小明", service.chat("我叫小明", "conversation-a", AiProvider.DEEPSEEK));
        assertEquals("你刚才说你叫小明", service.chat("我叫什么？", "conversation-a", AiProvider.DEEPSEEK));

        Prompt secondPrompt = model.prompts.get(1);
        assertTrue(secondPrompt.getInstructions().stream()
                .anyMatch(message -> message.getMessageType() == MessageType.USER
                        && message.getText().equals("我叫小明")));
        assertTrue(secondPrompt.getInstructions().stream()
                .anyMatch(message -> message.getMessageType() == MessageType.ASSISTANT
                        && message.getText().equals("你好，小明")));
    }

    @Test
    void chatHistoryIsIsolatedByChatId() {
        RecordingChatModel model = new RecordingChatModel("第一轮回答", "新会话回答");
        LearningChatService service = new LearningChatService(Map.of(AiProvider.DEEPSEEK, model));

        service.chat("只属于会话 A 的消息", "conversation-a", AiProvider.DEEPSEEK);
        service.chat("会话 B 的问题", "conversation-b", AiProvider.DEEPSEEK);

        Prompt secondPrompt = model.prompts.get(1);
        assertFalse(secondPrompt.getInstructions().stream()
                .anyMatch(message -> message.getText().equals("只属于会话 A 的消息")));
    }

    @Test
    void chatHistoryIsIsolatedByProvider() {
        RecordingChatModel deepseek = new RecordingChatModel("DeepSeek 回答");
        RecordingChatModel minimax = new RecordingChatModel("MiniMax 回答");
        LearningChatService service = new LearningChatService(Map.of(
                AiProvider.DEEPSEEK, deepseek,
                AiProvider.MINIMAX, minimax
        ));

        service.chat("只发给 DeepSeek", "same-chat-id", AiProvider.DEEPSEEK);
        service.chat("发给 MiniMax", "same-chat-id", AiProvider.MINIMAX);

        assertFalse(minimax.prompts.getFirst().getInstructions().stream()
                .anyMatch(message -> message.getText().equals("只发给 DeepSeek")));
    }

    @Test
    void chatRejectsUnconfiguredProvider() {
        LearningChatService service = new LearningChatService(Map.of(
                AiProvider.DEEPSEEK, new RecordingChatModel("回答")
        ));

        assertThrows(ProviderUnavailableException.class,
                () -> service.chat("你好", "chat", AiProvider.MINIMAX));
    }

    private static final class RecordingChatModel implements ChatModel {

        private final List<String> replies;
        private final List<Prompt> prompts = new ArrayList<>();

        private RecordingChatModel(String... replies) {
            this.replies = List.of(replies);
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            String reply = replies.get(prompts.size() - 1);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
        }
    }
}
