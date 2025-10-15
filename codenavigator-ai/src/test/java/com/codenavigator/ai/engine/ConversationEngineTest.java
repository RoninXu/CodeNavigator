package com.codenavigator.ai.engine;

import com.codenavigator.ai.dto.ConversationRequest;
import com.codenavigator.ai.dto.ConversationResponse;
import com.codenavigator.ai.enums.AiProvider;
import com.codenavigator.ai.model.ConversationState;
import com.codenavigator.ai.service.AiModelService;
import com.codenavigator.ai.service.ConversationStateManager;
import com.codenavigator.ai.service.LearningPathGenerator;
import com.codenavigator.ai.service.NaturalLanguageProcessor;
import com.codenavigator.common.enums.UserLevel;
import com.codenavigator.core.entity.LearningPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ConversationEngine单元测试
 * 测试核心对话引擎的各个对话阶段和响应生成逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationEngine单元测试")
class ConversationEngineTest {

    @Mock
    private AiModelService aiModelService;

    @Mock
    private NaturalLanguageProcessor nlpProcessor;

    @Mock
    private LearningPathGenerator pathGenerator;

    @Mock
    private ConversationStateManager stateManager;

    @InjectMocks
    private ConversationEngine conversationEngine;

    private ConversationRequest testRequest;
    private ConversationState testState;

    @BeforeEach
    void setUp() {
        testRequest = ConversationRequest.builder()
            .userId(1L)
            .message("我想学习Spring")
            .type("learning")
            .build();

        testState = ConversationState.builder()
            .sessionId("test-session-id")
            .userId(1L)
            .phase(ConversationState.ConversationPhase.GREETING)
            .messageCount(0)
            .build();
    }

    // ========== 会话初始化测试 ==========

    @Test
    @DisplayName("处理新会话 - 应创建新的会话状态")
    void testProcessMessage_NewSession_CreatesNewState() {
        // Given
        when(stateManager.getState(anyString())).thenReturn(null);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("greeting");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.TEXT_RESPONSE);
        verify(stateManager).saveState(any(ConversationState.class));
    }

    @Test
    @DisplayName("处理已存在会话 - 应恢复现有会话状态")
    void testProcessMessage_ExistingSession_RestoresState() {
        // Given
        testRequest.setSessionId("existing-session-id");
        when(stateManager.getState("existing-session-id")).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("greeting");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("test-session-id");
        verify(stateManager).getState("existing-session-id");
        verify(stateManager).saveState(testState);
    }

    // ========== GREETING阶段测试 ==========

    @Test
    @DisplayName("问候阶段 - 应返回欢迎消息和建议操作")
    void testProcessMessage_GreetingPhase_ReturnsWelcomeMessage() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.GREETING);
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("greeting");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.TEXT_RESPONSE);
        assertThat(response.getMessage()).contains("CodeNavigator", "学习助手");
        assertThat(response.getConfidence()).isEqualTo(1.0);
        assertThat(response.getSuggestedActions()).isNotEmpty();
        assertThat(response.getSuggestedActions()).hasSize(3);

        // 验证建议操作包含Spring, Kafka, Netty
        assertThat(response.getSuggestedActions())
            .anyMatch(action -> action.getLabel().contains("Spring"))
            .anyMatch(action -> action.getLabel().contains("Kafka"))
            .anyMatch(action -> action.getLabel().contains("Netty"));
    }

    // ========== GOAL_IDENTIFICATION阶段测试 ==========

    @Test
    @DisplayName("目标识别阶段 - 成功提取学习目标")
    void testProcessMessage_GoalIdentification_ExtractsLearningGoal() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.GOAL_IDENTIFICATION);
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("set_learning_goal");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(nlpProcessor.extractLearningGoal("我想学习Spring")).thenReturn("Spring");

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.TEXT_RESPONSE);
        assertThat(response.getMessage()).contains("Spring", "技术水平");
        assertThat(response.getConfidence()).isEqualTo(0.8);
        assertThat(testState.getLearningGoal()).isEqualTo("Spring");
    }

    @Test
    @DisplayName("目标识别阶段 - 无法提取学习目标，请求澄清")
    void testProcessMessage_GoalIdentification_NeedsClarification() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.GOAL_IDENTIFICATION);
        testRequest.setMessage("我想学点东西");
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("set_learning_goal");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(nlpProcessor.extractLearningGoal("我想学点东西")).thenReturn(null);

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.CLARIFICATION_NEEDED);
        assertThat(response.getMessage()).contains("没有完全理解", "更具体");
        assertThat(response.getConfidence()).isEqualTo(0.3);
    }

    // ========== SKILL_ASSESSMENT阶段测试 ==========

    @Test
    @DisplayName("技能评估阶段 - 评估用户技能水平")
    void testProcessMessage_SkillAssessment_AssessesUserLevel() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.SKILL_ASSESSMENT);
        testRequest.setMessage("我是初学者");
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("assess_skill");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(nlpProcessor.assessUserLevel("我是初学者")).thenReturn(UserLevel.BEGINNER);

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.TEXT_RESPONSE);
        assertThat(response.getMessage()).contains("生成", "学习路径");
        assertThat(response.getConfidence()).isEqualTo(0.9);
        assertThat(testState.getUserLevel()).isEqualTo(UserLevel.BEGINNER);
    }

    // ========== PATH_PLANNING阶段测试 ==========

    @Test
    @DisplayName("路径规划阶段 - 成功生成学习路径")
    void testProcessMessage_PathPlanning_GeneratesLearningPath() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.PATH_PLANNING);
        testState.setLearningGoal("Spring");
        testState.setUserLevel(UserLevel.BEGINNER);

        LearningPath mockPath = new LearningPath();
        mockPath.setId(1L);
        mockPath.setEstimatedDuration(8);
        mockPath.setModules(List.of());

        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("generate_path");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(pathGenerator.generatePath(eq("Spring"), eq(UserLevel.BEGINNER), any()))
            .thenReturn(mockPath);

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.LEARNING_PATH_GENERATED);
        assertThat(response.getMessage()).contains("学习路径", "8周");
        assertThat(response.getData()).containsKey("learningPath");
        assertThat(response.getConfidence()).isEqualTo(0.85);
        assertThat(response.getSuggestedActions()).hasSize(2);

        verify(pathGenerator).generatePath("Spring", UserLevel.BEGINNER, testState.getContext());
    }

    @Test
    @DisplayName("路径规划阶段 - 生成路径失败返回错误")
    void testProcessMessage_PathPlanning_GenerationFails() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.PATH_PLANNING);
        testState.setLearningGoal("Spring");
        testState.setUserLevel(UserLevel.BEGINNER);

        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("generate_path");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(pathGenerator.generatePath(anyString(), any(), any()))
            .thenThrow(new RuntimeException("Generation failed"));

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.ERROR_MESSAGE);
        assertThat(response.getMessage()).contains("出现了问题");
        assertThat(response.getConfidence()).isEqualTo(0.0);
    }

    // ========== TASK_EXECUTION阶段测试 ==========

    @Test
    @DisplayName("任务执行阶段 - 处理学习任务")
    void testProcessMessage_TaskExecution_HandlesTask() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.TASK_EXECUTION);
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("execute_task");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.TEXT_RESPONSE);
        assertThat(response.getMessage()).contains("学习任务");
        assertThat(response.getConfidence()).isEqualTo(0.7);
    }

    // ========== REVIEW_FEEDBACK阶段测试 ==========

    @Test
    @DisplayName("回顾反馈阶段 - 处理学习回顾")
    void testProcessMessage_ReviewFeedback_HandlesReview() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.REVIEW_FEEDBACK);
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("review");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.TEXT_RESPONSE);
        assertThat(response.getMessage()).contains("回顾", "学习进展");
        assertThat(response.getConfidence()).isEqualTo(0.7);
    }

    // ========== AI模型响应测试 ==========

    @Test
    @DisplayName("使用默认AI提供商生成响应")
    void testProcessMessage_UsesDefaultAiProvider() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.TASK_EXECUTION);
        testState.setLearningGoal("Spring");
        testState.setUserLevel(UserLevel.INTERMEDIATE);

        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("general_question");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(aiModelService.sendMessage(anyString())).thenReturn("这是AI生成的回复");

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        verify(aiModelService).sendMessage(anyString());
    }

    @Test
    @DisplayName("使用指定的AI提供商生成响应")
    void testProcessMessage_UsesPreferredAiProvider() {
        // Given
        testRequest.setPreferredProvider("deepseek");
        testState.setPhase(ConversationState.ConversationPhase.TASK_EXECUTION);

        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("general_question");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(aiModelService.sendMessage(anyString(), eq(AiProvider.DEEPSEEK)))
            .thenReturn("DeepSeek生成的回复");

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        verify(aiModelService).sendMessage(anyString(), eq(AiProvider.DEEPSEEK));
    }

    @Test
    @DisplayName("指定的AI提供商失败时回退到默认提供商")
    void testProcessMessage_FallbackToDefaultProviderOnFailure() {
        // Given
        testRequest.setPreferredProvider("invalid-provider");
        testState.setPhase(ConversationState.ConversationPhase.TASK_EXECUTION);

        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("general_question");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(aiModelService.sendMessage(anyString())).thenReturn("默认AI生成的回复");

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        verify(aiModelService).sendMessage(anyString());
    }

    // ========== 错误处理测试 ==========

    @Test
    @DisplayName("处理消息时发生异常 - 返回错误响应")
    void testProcessMessage_HandlesException_ReturnsErrorResponse() {
        // Given
        when(stateManager.getState(anyString())).thenThrow(new RuntimeException("Database error"));

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.ERROR_MESSAGE);
        assertThat(response.getMessage()).contains("遇到了一些问题");
        assertThat(response.getConfidence()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("NLP处理失败 - 应优雅降级")
    void testProcessMessage_NlpProcessingFails_GracefulDegradation() {
        // Given
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any()))
            .thenThrow(new RuntimeException("NLP error"));

        // When
        ConversationResponse response = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(ConversationResponse.ResponseType.ERROR_MESSAGE);
    }

    // ========== 会话状态转换测试 ==========

    @Test
    @DisplayName("会话状态正确转换 - GREETING到GOAL_IDENTIFICATION")
    void testConversationStateTransition_GreetingToGoalIdentification() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.GREETING);
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("set_learning_goal");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());

        // When
        conversationEngine.processMessage(testRequest);

        // Then
        assertThat(testState.getPhase()).isEqualTo(ConversationState.ConversationPhase.GOAL_IDENTIFICATION);
    }

    @Test
    @DisplayName("会话状态正确转换 - GOAL_IDENTIFICATION到SKILL_ASSESSMENT")
    void testConversationStateTransition_GoalToSkill() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.GOAL_IDENTIFICATION);
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("set_learning_goal");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(nlpProcessor.extractLearningGoal(anyString())).thenReturn("Spring");

        // When
        conversationEngine.processMessage(testRequest);

        // Then
        assertThat(testState.getPhase()).isEqualTo(ConversationState.ConversationPhase.SKILL_ASSESSMENT);
    }

    @Test
    @DisplayName("会话状态正确转换 - SKILL_ASSESSMENT到PATH_PLANNING")
    void testConversationStateTransition_SkillToPath() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.SKILL_ASSESSMENT);
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("assess_skill");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(nlpProcessor.assessUserLevel(anyString())).thenReturn(UserLevel.BEGINNER);

        // When
        conversationEngine.processMessage(testRequest);

        // Then
        assertThat(testState.getPhase()).isEqualTo(ConversationState.ConversationPhase.PATH_PLANNING);
    }

    @Test
    @DisplayName("会话状态正确转换 - PATH_PLANNING到TASK_EXECUTION")
    void testConversationStateTransition_PathToTask() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.PATH_PLANNING);
        testState.setLearningGoal("Spring");
        testState.setUserLevel(UserLevel.BEGINNER);

        LearningPath mockPath = new LearningPath();
        mockPath.setId(1L);
        mockPath.setEstimatedDuration(8);
        mockPath.setModules(List.of());

        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("generate_path");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());
        when(pathGenerator.generatePath(anyString(), any(), any())).thenReturn(mockPath);

        // When
        conversationEngine.processMessage(testRequest);

        // Then
        assertThat(testState.getPhase()).isEqualTo(ConversationState.ConversationPhase.TASK_EXECUTION);
    }

    // ========== 会话消息管理测试 ==========

    @Test
    @DisplayName("消息应添加到会话状态")
    void testProcessMessage_AddsMessageToState() {
        // Given
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("greeting");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());

        // When
        conversationEngine.processMessage(testRequest);

        // Then
        verify(stateManager).saveState(argThat(state ->
            state.getMessageCount() > 0
        ));
    }

    // ========== 置信度测试 ==========

    @Test
    @DisplayName("不同阶段应返回适当的置信度")
    void testConfidenceLevels_VaryByPhase() {
        // Given
        when(stateManager.getState(anyString())).thenReturn(testState);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("greeting");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());

        // When - GREETING phase (highest confidence)
        testState.setPhase(ConversationState.ConversationPhase.GREETING);
        ConversationResponse greetingResponse = conversationEngine.processMessage(testRequest);

        // When - GOAL_IDENTIFICATION phase with unclear goal
        testState.setPhase(ConversationState.ConversationPhase.GOAL_IDENTIFICATION);
        when(nlpProcessor.extractLearningGoal(anyString())).thenReturn(null);
        ConversationResponse unclearResponse = conversationEngine.processMessage(testRequest);

        // Then
        assertThat(greetingResponse.getConfidence()).isEqualTo(1.0);
        assertThat(unclearResponse.getConfidence()).isLessThan(greetingResponse.getConfidence());
    }

    // ========== 集成场景测试 ==========

    @Test
    @DisplayName("完整对话流程 - 从问候到路径生成")
    void testCompleteConversationFlow_GreetingToPathGeneration() {
        // Given - 新会话
        ConversationRequest req1 = ConversationRequest.builder()
            .userId(1L)
            .message("你好")
            .type("learning")
            .build();

        ConversationState state = ConversationState.builder()
            .sessionId("flow-test-session")
            .userId(1L)
            .phase(ConversationState.ConversationPhase.GREETING)
            .messageCount(0)
            .build();

        when(stateManager.getState(anyString())).thenReturn(state);
        when(nlpProcessor.extractIntent(anyString(), any())).thenReturn("set_learning_goal");
        when(nlpProcessor.extractEntities(anyString())).thenReturn(new HashMap<>());

        // When - Step 1: Greeting
        ConversationResponse response1 = conversationEngine.processMessage(req1);

        // Then - Step 1
        assertThat(response1).isNotNull();
        assertThat(response1.getType()).isEqualTo(ConversationResponse.ResponseType.TEXT_RESPONSE);

        // When - Step 2: Set learning goal
        state.setPhase(ConversationState.ConversationPhase.GOAL_IDENTIFICATION);
        when(nlpProcessor.extractLearningGoal(anyString())).thenReturn("Kafka");
        ConversationRequest req2 = ConversationRequest.builder()
            .userId(1L)
            .sessionId(response1.getSessionId())
            .message("我想学习Kafka")
            .build();

        ConversationResponse response2 = conversationEngine.processMessage(req2);

        // Then - Step 2
        assertThat(response2).isNotNull();
        assertThat(state.getLearningGoal()).isEqualTo("Kafka");
        assertThat(state.getPhase()).isEqualTo(ConversationState.ConversationPhase.SKILL_ASSESSMENT);
    }
}
