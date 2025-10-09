package com.codenavigator.ai.service.impl;

import com.codenavigator.ai.config.AiModelConfig;
import com.codenavigator.ai.enums.AiProvider;
import com.codenavigator.ai.service.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private final AiModelConfig aiModelConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicReference<AiProvider> currentProvider = new AtomicReference<>();

    @Override
    public String sendMessage(String message) {
        AiProvider provider = getCurrentProvider();
        return sendMessage(message, provider);
    }

    @Override
    public String sendMessage(String message, AiProvider provider) {
        log.info("Sending message to {} provider", provider.getDisplayName());

        AiModelConfig.ProviderConfig config = aiModelConfig.getProviderConfig(provider.getCode());
        if (config == null || !config.getEnabled()) {
            throw new IllegalStateException("Provider " + provider.getDisplayName() + " is not available");
        }

        try {
            return callAiApi(message, provider, config);
        } catch (Exception e) {
            log.error("Error calling {} API", provider.getDisplayName(), e);
            throw new RuntimeException("Failed to get response from " + provider.getDisplayName(), e);
        }
    }

    @Override
    public AiProvider getCurrentProvider() {
        if (currentProvider.get() == null) {
            // 初始化为默认提供商
            AiProvider defaultProvider = AiProvider.fromCode(aiModelConfig.getDefaultProvider());
            currentProvider.set(defaultProvider);
        }
        return currentProvider.get();
    }

    @Override
    public void switchProvider(AiProvider provider) {
        if (!isProviderAvailable(provider)) {
            throw new IllegalArgumentException("Provider " + provider.getDisplayName() + " is not available");
        }

        log.info("Switching AI provider from {} to {}",
                getCurrentProvider().getDisplayName(), provider.getDisplayName());

        currentProvider.set(provider);
    }

    @Override
    public List<AiProvider> getAvailableProviders() {
        return Arrays.stream(AiProvider.values())
                .filter(this::isProviderAvailable)
                .toList();
    }

    @Override
    public Map<String, Object> getProviderStatus(AiProvider provider) {
        Map<String, Object> status = new HashMap<>();
        status.put("provider", provider.getCode());
        status.put("displayName", provider.getDisplayName());
        status.put("description", provider.getDescription());
        status.put("available", isProviderAvailable(provider));
        status.put("current", provider.equals(getCurrentProvider()));

        AiModelConfig.ProviderConfig config = aiModelConfig.getProviderConfig(provider.getCode());
        if (config != null) {
            status.put("modelName", config.getModelName());
            status.put("temperature", config.getTemperature());
            status.put("maxTokens", config.getMaxTokens());
            status.put("baseUrl", config.getBaseUrl());
            status.put("hasApiKey", config.getApiKey() != null && !config.getApiKey().trim().isEmpty());
        }

        return status;
    }

    @Override
    public Map<AiProvider, Map<String, Object>> getAllProviderStatus() {
        Map<AiProvider, Map<String, Object>> allStatus = new HashMap<>();
        for (AiProvider provider : AiProvider.values()) {
            allStatus.put(provider, getProviderStatus(provider));
        }
        return allStatus;
    }

    @Override
    public boolean testProvider(AiProvider provider) {
        try {
            log.info("Testing provider: {}", provider.getDisplayName());
            String testResponse = sendMessage("Hello", provider);
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            log.warn("Provider {} test failed", provider.getDisplayName(), e);
            return false;
        }
    }

    private boolean isProviderAvailable(AiProvider provider) {
        return aiModelConfig.isProviderEnabled(provider.getCode());
    }

    private String callAiApi(String message, AiProvider provider, AiModelConfig.ProviderConfig config) {
        switch (provider) {
            case OPENAI:
                return callOpenAiApi(message, config);
            case DEEPSEEK:
                return callDeepSeekApi(message, config);
            default:
                throw new UnsupportedOperationException("Provider " + provider + " is not implemented yet");
        }
    }

    private String callOpenAiApi(String message, AiModelConfig.ProviderConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getApiKey());
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModelName());
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String url = config.getBaseUrl() + "/chat/completions";
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        return extractResponseContent(response.getBody());
    }

    private String callDeepSeekApi(String message, AiModelConfig.ProviderConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getApiKey());
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModelName());
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String url = config.getBaseUrl() + "/chat/completions";
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        return extractResponseContent(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private String extractResponseContent(Map<String, Object> responseBody) {
        if (responseBody == null) {
            throw new RuntimeException("Empty response from AI service");
        }

        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in AI response");
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String content = (String) message.get("content");

            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Empty content in AI response");
            }

            return content.trim();
        } catch (ClassCastException | NullPointerException e) {
            log.error("Error parsing AI response: {}", responseBody, e);
            throw new RuntimeException("Invalid response format from AI service", e);
        }
    }
}