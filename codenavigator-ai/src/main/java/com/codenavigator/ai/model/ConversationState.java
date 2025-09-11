package com.codenavigator.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.codenavigator.common.enums.UserLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationState {
    
    private String sessionId;
    private String userId;
    private ConversationPhase phase;
    private UserLevel userLevel;
    private String currentTopic;
    private Map<String, Object> context;
    private List<String> messageHistory;
    private LocalDateTime lastInteraction;
    private Integer messageCount;
    private String learningGoal;
    private List<String> identifiedSkills;
    private String preferredLearningStyle;
    
    public enum ConversationPhase {
        GREETING,
        GOAL_IDENTIFICATION,
        SKILL_ASSESSMENT,
        PATH_PLANNING,
        TASK_EXECUTION,
        REVIEW_FEEDBACK,
        COMPLETED
    }
    
    public void addMessage(String message) {
        if (messageHistory == null) {
            messageHistory = new java.util.ArrayList<>();
        }
        messageHistory.add(message);
        this.messageCount = messageHistory.size();
        this.lastInteraction = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return lastInteraction != null && 
               lastInteraction.isBefore(LocalDateTime.now().minusHours(2));
    }
}