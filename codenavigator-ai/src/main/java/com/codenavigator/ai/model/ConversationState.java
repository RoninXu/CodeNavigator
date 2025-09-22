package com.codenavigator.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.codenavigator.common.enums.UserLevel;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
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

    public List<String> getRecentMessages(int count) {
        if (messageHistory == null || messageHistory.isEmpty()) {
            return List.of();
        }

        int startIndex = Math.max(0, messageHistory.size() - count);
        return messageHistory.subList(startIndex, messageHistory.size());
    }
}