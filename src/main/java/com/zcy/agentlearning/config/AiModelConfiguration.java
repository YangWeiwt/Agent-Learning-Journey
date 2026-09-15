package com.zcy.agentlearning.config;

import com.zcy.agentlearning.service.AiProvider;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiModelConfiguration {

    @Bean
    public Map<AiProvider, ChatModel> providerChatModels(AiProviderProperties properties) {
        Map<AiProvider, ChatModel> models = new EnumMap<>(AiProvider.class);
        register(models, AiProvider.DEEPSEEK, properties.getDeepseek());
        register(models, AiProvider.MINIMAX, properties.getMinimax());
        return Collections.unmodifiableMap(models);
    }

    private void register(
            Map<AiProvider, ChatModel> models,
            AiProvider provider,
            AiProviderProperties.Provider properties
    ) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            return;
        }

        OpenAiApi api = OpenAiApi.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .completionsPath(properties.getCompletionsPath())
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .build();

        models.put(provider, OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build());
    }
}
