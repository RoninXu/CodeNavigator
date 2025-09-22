package com.codenavigator.ai.service;

import com.codenavigator.ai.config.AiModelConfig;
import com.codenavigator.ai.enums.AiProvider;
import com.codenavigator.ai.service.impl.AiModelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiModelServiceTest {

    @Mock
    private AiModelConfig aiModelConfig;

    @InjectMocks
    private AiModelServiceImpl aiModelService;

    private AiModelConfig.ProviderConfig openAiConfig;
    private AiModelConfig.ProviderConfig deepSeekConfig;

    @BeforeEach
    void setUp() {
        openAiConfig = new AiModelConfig.ProviderConfig();
        openAiConfig.setApiKey("test-openai-key");
        openAiConfig.setBaseUrl("https://api.openai.com/v1");
        openAiConfig.setModelName("gpt-4");
        openAiConfig.setEnabled(true);

        deepSeekConfig = new AiModelConfig.ProviderConfig();
        deepSeekConfig.setApiKey("test-deepseek-key");
        deepSeekConfig.setBaseUrl("https://api.deepseek.com/v1");
        deepSeekConfig.setModelName("deepseek-chat");
        deepSeekConfig.setEnabled(true);

        Map<String, AiModelConfig.ProviderConfig> providers = new HashMap<>();
        providers.put("openai", openAiConfig);
        providers.put("deepseek", deepSeekConfig);

        when(aiModelConfig.getDefaultProvider()).thenReturn("openai");
        when(aiModelConfig.getProviders()).thenReturn(providers);
        when(aiModelConfig.getProviderConfig("openai")).thenReturn(openAiConfig);
        when(aiModelConfig.getProviderConfig("deepseek")).thenReturn(deepSeekConfig);
        when(aiModelConfig.isProviderEnabled("openai")).thenReturn(true);
        when(aiModelConfig.isProviderEnabled("deepseek")).thenReturn(true);
    }

    @Test
    void testGetCurrentProvider() {
        AiProvider currentProvider = aiModelService.getCurrentProvider();
        assertEquals(AiProvider.OPENAI, currentProvider);
    }

    @Test
    void testSwitchProvider() {
        // 初始状态是OpenAI
        assertEquals(AiProvider.OPENAI, aiModelService.getCurrentProvider());

        // 切换到DeepSeek
        aiModelService.switchProvider(AiProvider.DEEPSEEK);
        assertEquals(AiProvider.DEEPSEEK, aiModelService.getCurrentProvider());

        // 切换回OpenAI
        aiModelService.switchProvider(AiProvider.OPENAI);
        assertEquals(AiProvider.OPENAI, aiModelService.getCurrentProvider());
    }

    @Test
    void testGetAvailableProviders() {
        List<AiProvider> providers = aiModelService.getAvailableProviders();
        assertTrue(providers.contains(AiProvider.OPENAI));
        assertTrue(providers.contains(AiProvider.DEEPSEEK));
    }

    @Test
    void testGetProviderStatus() {
        Map<String, Object> status = aiModelService.getProviderStatus(AiProvider.OPENAI);

        assertEquals("openai", status.get("provider"));
        assertEquals("OpenAI", status.get("displayName"));
        assertEquals(true, status.get("available"));
        assertEquals("gpt-4", status.get("modelName"));
        assertEquals(true, status.get("hasApiKey"));
    }

    @Test
    void testSwitchToUnavailableProvider() {
        when(aiModelConfig.isProviderEnabled("claude")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            aiModelService.switchProvider(AiProvider.CLAUDE));
    }

    @Test
    void testProviderConfigValidation() {
        // 测试配置验证
        assertTrue(openAiConfig.getEnabled());
        assertNotNull(openAiConfig.getApiKey());
        assertNotNull(openAiConfig.getBaseUrl());
        assertNotNull(openAiConfig.getModelName());

        assertEquals("gpt-4", openAiConfig.getModelName());
        assertEquals("deepseek-chat", deepSeekConfig.getModelName());
    }
}