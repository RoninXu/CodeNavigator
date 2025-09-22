package com.codenavigator.ai.service;

import com.codenavigator.ai.enums.AiProvider;

import java.util.List;
import java.util.Map;

public interface AiModelService {

    /**
     * 发送消息到AI模型
     */
    String sendMessage(String message);

    /**
     * 使用指定提供商发送消息
     */
    String sendMessage(String message, AiProvider provider);

    /**
     * 获取当前使用的AI提供商
     */
    AiProvider getCurrentProvider();

    /**
     * 切换AI提供商
     */
    void switchProvider(AiProvider provider);

    /**
     * 获取所有可用的AI提供商
     */
    List<AiProvider> getAvailableProviders();

    /**
     * 获取提供商状态信息
     */
    Map<String, Object> getProviderStatus(AiProvider provider);

    /**
     * 获取所有提供商状态
     */
    Map<AiProvider, Map<String, Object>> getAllProviderStatus();

    /**
     * 测试提供商连接
     */
    boolean testProvider(AiProvider provider);
}