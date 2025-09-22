package com.codenavigator.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRequest {
    
    private String userId;
    private String message;
    private String sessionId;
    private ConversationType type;
    private String preferredProvider; // AI模型提供商偏好
    private Map<String, Object> context;
    
    public enum ConversationType {
        LEARNING_GOAL_SETTING,
        LEARNING_PATH_QUERY,
        TASK_ASSISTANCE,
        CODE_REVIEW,
        GENERAL_QUESTION
    }
}