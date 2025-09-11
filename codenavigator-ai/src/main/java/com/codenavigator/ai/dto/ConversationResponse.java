package com.codenavigator.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.codenavigator.common.enums.DifficultyLevel;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    
    private String message;
    private ResponseType type;
    private String sessionId;
    private Map<String, Object> data;
    private List<SuggestedAction> suggestedActions;
    private Double confidence;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestedAction {
        private String label;
        private String action;
        private Map<String, Object> parameters;
    }
    
    public enum ResponseType {
        TEXT_RESPONSE,
        LEARNING_PATH_GENERATED,
        TASK_BREAKDOWN,
        CODE_ANALYSIS,
        ERROR_MESSAGE,
        CLARIFICATION_NEEDED
    }
}