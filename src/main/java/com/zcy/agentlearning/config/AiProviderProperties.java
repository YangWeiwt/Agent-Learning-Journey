package com.zcy.agentlearning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProviderProperties {

    private Provider deepseek = new Provider();
    private Provider minimax = new Provider();

    public Provider getDeepseek() {
        return deepseek;
    }

    public void setDeepseek(Provider deepseek) {
        this.deepseek = deepseek;
    }

    public Provider getMinimax() {
        return minimax;
    }

    public void setMinimax(Provider minimax) {
        this.minimax = minimax;
    }

    public static class Provider {

        private String apiKey;
        private String baseUrl;
        private String completionsPath;
        private String model;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getCompletionsPath() {
            return completionsPath;
        }

        public void setCompletionsPath(String completionsPath) {
            this.completionsPath = completionsPath;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
