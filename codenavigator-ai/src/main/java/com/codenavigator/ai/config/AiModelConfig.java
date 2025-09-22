package com.codenavigator.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiModelConfig {

    private String defaultProvider = "openai";
    private Map<String, ProviderConfig> providers;

    @Data
    public static class ProviderConfig {
        private String apiKey;
        private String baseUrl;
        private String modelName;
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
        private String timeout = "60s";
        private Boolean enabled = true;

        public long getTimeoutInSeconds() {
            if (timeout == null) return 60;

            String timeoutStr = timeout.toLowerCase();
            if (timeoutStr.endsWith("s")) {
                return Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1));
            } else if (timeoutStr.endsWith("m")) {
                return Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)) * 60;
            } else {
                return Long.parseLong(timeoutStr);
            }
        }
    }

    public ProviderConfig getProviderConfig(String provider) {
        if (providers == null) {
            return null;
        }
        return providers.get(provider);
    }

    public ProviderConfig getDefaultProviderConfig() {
        return getProviderConfig(defaultProvider);
    }

    public boolean isProviderEnabled(String provider) {
        ProviderConfig config = getProviderConfig(provider);
        return config != null && config.getEnabled() &&
               config.getApiKey() != null && !config.getApiKey().trim().isEmpty();
    }
}