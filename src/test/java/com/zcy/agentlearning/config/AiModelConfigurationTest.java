package com.zcy.agentlearning.config;

import com.zcy.agentlearning.service.AiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelConfigurationTest {

    @Test
    void createsBothConfiguredProvidersWithTheirOwnModels() {
        AiProviderProperties properties = new AiProviderProperties();
        configure(properties.getDeepseek(), "deepseek-key", "https://api.deepseek.com",
                "/chat/completions", "deepseek-v4-flash");
        configure(properties.getMinimax(), "minimax-key", "https://api.minimax.io",
                "/v1/chat/completions", "MiniMax-M2.7");

        Map<AiProvider, ChatModel> models = new AiModelConfiguration().providerChatModels(properties);

        assertEquals(2, models.size());
        assertEquals("deepseek-v4-flash", modelName(models.get(AiProvider.DEEPSEEK)));
        assertEquals("MiniMax-M2.7", modelName(models.get(AiProvider.MINIMAX)));
    }

    @Test
    void skipsProviderWithoutApiKey() {
        AiProviderProperties properties = new AiProviderProperties();
        configure(properties.getDeepseek(), "deepseek-key", "https://api.deepseek.com",
                "/chat/completions", "deepseek-v4-flash");
        configure(properties.getMinimax(), "", "https://api.minimax.io",
                "/v1/chat/completions", "MiniMax-M2.7");

        Map<AiProvider, ChatModel> models = new AiModelConfiguration().providerChatModels(properties);

        assertTrue(models.containsKey(AiProvider.DEEPSEEK));
        assertFalse(models.containsKey(AiProvider.MINIMAX));
    }

    private void configure(
            AiProviderProperties.Provider provider,
            String apiKey,
            String baseUrl,
            String completionsPath,
            String model
    ) {
        provider.setApiKey(apiKey);
        provider.setBaseUrl(baseUrl);
        provider.setCompletionsPath(completionsPath);
        provider.setModel(model);
    }

    private String modelName(ChatModel model) {
        return ((OpenAiChatOptions) model.getDefaultOptions()).getModel();
    }
}
