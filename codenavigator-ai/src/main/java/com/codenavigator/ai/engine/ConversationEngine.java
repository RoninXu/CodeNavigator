package com.codenavigator.ai.engine;

import com.codenavigator.ai.dto.ConversationRequest;
import com.codenavigator.ai.dto.ConversationResponse;
import com.codenavigator.ai.enums.AiProvider;
import com.codenavigator.ai.model.ConversationState;
import com.codenavigator.ai.service.AiModelService;
import com.codenavigator.ai.service.NaturalLanguageProcessor;
import com.codenavigator.ai.service.LearningPathGenerator;
import com.codenavigator.ai.service.ConversationStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEngine {
    
    private final AiModelService aiModelService;
    private final NaturalLanguageProcessor nlpProcessor;
    private final LearningPathGenerator pathGenerator;
    private final ConversationStateManager stateManager;
    
    public ConversationResponse processMessage(ConversationRequest request) {
        log.info("Processing conversation message for user: {}", request.getUserId());
        
        try {
            // 获取或创建会话状态
            ConversationState state = getOrCreateState(request);
            
            // 更新会话状态
            state.addMessage(request.getMessage());
            
            // 自然语言理解
            var intent = nlpProcessor.extractIntent(request.getMessage(), state);
            var entities = nlpProcessor.extractEntities(request.getMessage());
            
            // 根据会话阶段和意图生成响应
            ConversationResponse response = generateResponse(request, state, intent, entities);
            
            // 更新会话状态
            updateConversationState(state, intent, entities);
            stateManager.saveState(state);
            
            response.setSessionId(state.getSessionId());
            return response;
            
        } catch (Exception e) {
            log.error("Error processing conversation message", e);
            return ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.ERROR_MESSAGE)
                .message("抱歉，我遇到了一些问题，请稍后再试。")
                .sessionId(request.getSessionId())
                .confidence(0.0)
                .build();
        }
    }
    
    private ConversationState getOrCreateState(ConversationRequest request) {
        ConversationState state = null;
        
        if (request.getSessionId() != null) {
            state = stateManager.getState(request.getSessionId());
        }
        
        if (state == null) {
            state = ConversationState.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(request.getUserId())
                .phase(ConversationState.ConversationPhase.GREETING)
                .messageCount(0)
                .build();
        }
        
        return state;
    }
    
    private ConversationResponse generateResponse(
            ConversationRequest request, 
            ConversationState state, 
            String intent, 
            Map<String, Object> entities) {
        
        switch (state.getPhase()) {
            case GREETING:
                return handleGreeting(request, state);
            case GOAL_IDENTIFICATION:
                return handleGoalIdentification(request, state, intent, entities);
            case SKILL_ASSESSMENT:
                return handleSkillAssessment(request, state, intent, entities);
            case PATH_PLANNING:
                return handlePathPlanning(request, state, intent, entities);
            case TASK_EXECUTION:
                return handleTaskExecution(request, state, intent, entities);
            case REVIEW_FEEDBACK:
                return handleReviewFeedback(request, state, intent, entities);
            default:
                return generateDefaultResponse(request, state);
        }
    }
    
    private ConversationResponse handleGreeting(ConversationRequest request, ConversationState state) {
        String message = "你好！我是CodeNavigator的学习助手。我可以帮助你制定个性化的技术学习路径。" +
                        "请告诉我你想学习什么技术，或者你目前遇到的学习困难？";
        
        return ConversationResponse.builder()
            .type(ConversationResponse.ResponseType.TEXT_RESPONSE)
            .message(message)
            .confidence(1.0)
            .suggestedActions(List.of(
                ConversationResponse.SuggestedAction.builder()
                    .label("学习Spring框架")
                    .action("set_learning_goal")
                    .parameters(Map.of("technology", "Spring"))
                    .build(),
                ConversationResponse.SuggestedAction.builder()
                    .label("学习Kafka")
                    .action("set_learning_goal")
                    .parameters(Map.of("technology", "Kafka"))
                    .build(),
                ConversationResponse.SuggestedAction.builder()
                    .label("学习Netty")
                    .action("set_learning_goal")
                    .parameters(Map.of("technology", "Netty"))
                    .build()
            ))
            .build();
    }
    
    private ConversationResponse handleGoalIdentification(
            ConversationRequest request, 
            ConversationState state, 
            String intent, 
            Map<String, Object> entities) {
        
        // 解析学习目标
        String learningGoal = nlpProcessor.extractLearningGoal(request.getMessage());
        if (learningGoal != null) {
            state.setLearningGoal(learningGoal);
            
            String message = String.format("很好！我理解你想学习%s。为了为你制定最合适的学习路径，" +
                    "能告诉我你目前的技术水平吗？比如是初学者、有一定经验还是已经比较熟练？", learningGoal);
            
            return ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.TEXT_RESPONSE)
                .message(message)
                .confidence(0.8)
                .build();
        }
        
        return ConversationResponse.builder()
            .type(ConversationResponse.ResponseType.CLARIFICATION_NEEDED)
            .message("我没有完全理解你的学习目标，能否更具体地描述一下你想学习的技术？")
            .confidence(0.3)
            .build();
    }
    
    private ConversationResponse handleSkillAssessment(
            ConversationRequest request, 
            ConversationState state, 
            String intent, 
            Map<String, Object> entities) {
        
        // 评估用户技能水平
        var userLevel = nlpProcessor.assessUserLevel(request.getMessage());
        state.setUserLevel(userLevel);
        
        String message = "好的，基于你的技能水平，我来为你生成一个定制的学习路径。请稍等...";
        
        return ConversationResponse.builder()
            .type(ConversationResponse.ResponseType.TEXT_RESPONSE)
            .message(message)
            .confidence(0.9)
            .build();
    }
    
    private ConversationResponse handlePathPlanning(
            ConversationRequest request, 
            ConversationState state, 
            String intent, 
            Map<String, Object> entities) {
        
        try {
            // 生成学习路径
            var learningPath = pathGenerator.generatePath(
                state.getLearningGoal(), 
                state.getUserLevel(),
                state.getContext()
            );
            
            String message = "我已经为你生成了一个学习路径！这个路径包含了" + 
                    learningPath.getModules().size() + "个学习模块，预计需要" +
                    learningPath.getEstimatedDuration() + "周完成。你可以查看详细内容并开始学习。";
            
            return ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.LEARNING_PATH_GENERATED)
                .message(message)
                .data(Map.of("learningPath", learningPath))
                .confidence(0.85)
                .suggestedActions(List.of(
                    ConversationResponse.SuggestedAction.builder()
                        .label("开始学习")
                        .action("start_learning")
                        .parameters(Map.of("pathId", learningPath.getId()))
                        .build(),
                    ConversationResponse.SuggestedAction.builder()
                        .label("自定义路径")
                        .action("customize_path")
                        .parameters(Map.of("pathId", learningPath.getId()))
                        .build()
                ))
                .build();
                
        } catch (Exception e) {
            log.error("Error generating learning path", e);
            return ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.ERROR_MESSAGE)
                .message("抱歉，生成学习路径时出现了问题，请稍后再试。")
                .confidence(0.0)
                .build();
        }
    }
    
    private ConversationResponse handleTaskExecution(
            ConversationRequest request, 
            ConversationState state, 
            String intent, 
            Map<String, Object> entities) {
        
        // 处理任务执行阶段的对话
        return ConversationResponse.builder()
            .type(ConversationResponse.ResponseType.TEXT_RESPONSE)
            .message("我正在帮你处理当前的学习任务...")
            .confidence(0.7)
            .build();
    }
    
    private ConversationResponse handleReviewFeedback(
            ConversationRequest request, 
            ConversationState state, 
            String intent, 
            Map<String, Object> entities) {
        
        // 处理回顾反馈阶段的对话
        return ConversationResponse.builder()
            .type(ConversationResponse.ResponseType.TEXT_RESPONSE)
            .message("让我们回顾一下你的学习进展...")
            .confidence(0.7)
            .build();
    }
    
    private ConversationResponse generateDefaultResponse(ConversationRequest request, ConversationState state) {
        try {
            // 使用AI模型生成智能回复
            String aiResponse = generateAiResponse(request, state);
            return ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.TEXT_RESPONSE)
                .message(aiResponse)
                .confidence(0.8)
                .build();
        } catch (Exception e) {
            log.error("Error generating AI response", e);
            return ConversationResponse.builder()
                .type(ConversationResponse.ResponseType.TEXT_RESPONSE)
                .message("我理解你的需求，让我为你提供帮助...")
                .confidence(0.5)
                .build();
        }
    }

    private String generateAiResponse(ConversationRequest request, ConversationState state) {
        // 构建上下文提示
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是CodeNavigator的AI学习助手，专门帮助用户制定技术学习路径和解答编程问题。\n\n");

        // 添加会话上下文
        if (state.getLearningGoal() != null) {
            promptBuilder.append("用户学习目标: ").append(state.getLearningGoal()).append("\n");
        }
        if (state.getUserLevel() != null) {
            promptBuilder.append("用户技能水平: ").append(state.getUserLevel()).append("\n");
        }
        promptBuilder.append("会话阶段: ").append(state.getPhase()).append("\n");

        // 添加历史消息（最近几条）
        List<String> recentMessages = state.getRecentMessages(3);
        if (!recentMessages.isEmpty()) {
            promptBuilder.append("\n最近对话:\n");
            for (int i = 0; i < recentMessages.size(); i++) {
                promptBuilder.append(i % 2 == 0 ? "用户: " : "助手: ").append(recentMessages.get(i)).append("\n");
            }
        }

        promptBuilder.append("\n当前用户消息: ").append(request.getMessage());
        promptBuilder.append("\n\n请基于以上信息给出专业、有帮助的回复。回复应该简洁明了，并提供具体的学习建议或解答。");

        // 支持指定AI提供商
        if (request.getPreferredProvider() != null) {
            try {
                AiProvider provider = AiProvider.fromCode(request.getPreferredProvider());
                return aiModelService.sendMessage(promptBuilder.toString(), provider);
            } catch (Exception e) {
                log.warn("Failed to use preferred provider {}, falling back to default", request.getPreferredProvider());
            }
        }

        return aiModelService.sendMessage(promptBuilder.toString());
    }
    
    private void updateConversationState(ConversationState state, String intent, Map<String, Object> entities) {
        // 根据意图和实体更新会话状态
        switch (state.getPhase()) {
            case GREETING:
                if ("set_learning_goal".equals(intent)) {
                    state.setPhase(ConversationState.ConversationPhase.GOAL_IDENTIFICATION);
                }
                break;
            case GOAL_IDENTIFICATION:
                if (state.getLearningGoal() != null) {
                    state.setPhase(ConversationState.ConversationPhase.SKILL_ASSESSMENT);
                }
                break;
            case SKILL_ASSESSMENT:
                if (state.getUserLevel() != null) {
                    state.setPhase(ConversationState.ConversationPhase.PATH_PLANNING);
                }
                break;
            case PATH_PLANNING:
                state.setPhase(ConversationState.ConversationPhase.TASK_EXECUTION);
                break;
        }
    }
}