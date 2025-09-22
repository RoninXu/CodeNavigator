package com.codenavigator.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiProvider {
    OPENAI("openai", "OpenAI", "GPT系列模型"),
    DEEPSEEK("deepseek", "DeepSeek", "DeepSeek对话模型"),
    CLAUDE("claude", "Claude", "Anthropic Claude模型"),
    GEMINI("gemini", "Gemini", "Google Gemini模型");

    private final String code;
    private final String displayName;
    private final String description;

    public static AiProvider fromCode(String code) {
        for (AiProvider provider : values()) {
            if (provider.getCode().equalsIgnoreCase(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown AI provider: " + code);
    }
}