package com.codenavigator.ai.service;

import com.codenavigator.ai.model.ConversationState;
import com.codenavigator.common.enums.UserLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NaturalLanguageProcessor单元测试
 * 测试自然语言处理功能，包括意图识别、实体提取和技能水平评估
 */
@DisplayName("NaturalLanguageProcessor单元测试")
class NaturalLanguageProcessorTest {

    private NaturalLanguageProcessor nlpProcessor;
    private ConversationState testState;

    @BeforeEach
    void setUp() {
        nlpProcessor = new NaturalLanguageProcessor();
        testState = ConversationState.builder()
            .sessionId("test-session")
            .userId("1")
            .phase(ConversationState.ConversationPhase.GREETING)
            .messageCount(0)
            .build();
    }

    // ========== 意图识别测试 ==========

    @Test
    @DisplayName("识别学习目标意图 - 包含'学习'关键词")
    void testExtractIntent_LearningGoal_ContainsKeyword() {
        // Given
        String message = "我想学习Spring框架";

        // When
        String intent = nlpProcessor.extractIntent(message, testState);

        // Then
        assertThat(intent).isEqualTo("set_learning_goal");
    }

    @Test
    @DisplayName("识别提问意图 - 包含疑问词")
    void testExtractIntent_Question_ContainsQuestionWord() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.TASK_EXECUTION);
        String message = "什么是依赖注入？";

        // When
        String intent = nlpProcessor.extractIntent(message, testState);

        // Then
        assertThat(intent).isEqualTo("ask_question");
    }

    @Test
    @DisplayName("根据会话阶段识别意图 - GOAL_IDENTIFICATION阶段")
    void testExtractIntent_BasedOnPhase_GoalIdentification() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.GOAL_IDENTIFICATION);
        String message = "Spring Boot";

        // When
        String intent = nlpProcessor.extractIntent(message, testState);

        // Then
        assertThat(intent).isEqualTo("set_learning_goal");
    }

    @Test
    @DisplayName("根据会话阶段识别意图 - SKILL_ASSESSMENT阶段")
    void testExtractIntent_BasedOnPhase_SkillAssessment() {
        // Given
        testState.setPhase(ConversationState.ConversationPhase.SKILL_ASSESSMENT);
        String message = "我是初学者";

        // When
        String intent = nlpProcessor.extractIntent(message, testState);

        // Then
        assertThat(intent).isEqualTo("assess_skill");
    }

    @Test
    @DisplayName("无法识别意图时返回通用问题")
    void testExtractIntent_UnknownIntent_ReturnsGeneral() {
        // Given
        String message = "测试消息";

        // When
        String intent = nlpProcessor.extractIntent(message, testState);

        // Then
        assertThat(intent).isEqualTo("general_question");
    }

    // ========== 实体提取测试 ==========

    @Test
    @DisplayName("提取技术实体 - Spring")
    void testExtractEntities_TechnologyEntity_Spring() {
        // Given
        String message = "我想学习Spring Boot";

        // When
        Map<String, Object> entities = nlpProcessor.extractEntities(message);

        // Then
        assertThat(entities).containsKey("technology");
        assertThat(entities.get("technology")).isEqualTo("Spring");
    }

    @Test
    @DisplayName("提取技术实体 - Kafka")
    void testExtractEntities_TechnologyEntity_Kafka() {
        // Given
        String message = "学习消息队列Kafka";

        // When
        Map<String, Object> entities = nlpProcessor.extractEntities(message);

        // Then
        assertThat(entities).containsKey("technology");
        assertThat(entities.get("technology")).isEqualTo("Kafka");
    }

    @Test
    @DisplayName("提取技能水平实体 - 初学者")
    void testExtractEntities_SkillLevel_Beginner() {
        // Given
        String message = "我是初学者，想学习Java";

        // When
        Map<String, Object> entities = nlpProcessor.extractEntities(message);

        // Then
        assertThat(entities).containsKey("skill_level");
        assertThat(entities.get("skill_level")).isEqualTo(UserLevel.BEGINNER);
    }

    @Test
    @DisplayName("提取时间实体 - 周")
    void testExtractEntities_TimeDuration_Weeks() {
        // Given
        String message = "我计划用8周时间学习";

        // When
        Map<String, Object> entities = nlpProcessor.extractEntities(message);

        // Then
        assertThat(entities).containsKey("time_duration");
        assertThat(entities.get("time_duration").toString()).contains("8");
    }

    @Test
    @DisplayName("提取多个实体")
    void testExtractEntities_MultipleEntities() {
        // Given
        String message = "我是初学者，想用3周学习Spring";

        // When
        Map<String, Object> entities = nlpProcessor.extractEntities(message);

        // Then
        assertThat(entities).hasSize(3);
        assertThat(entities).containsKeys("technology", "skill_level", "time_duration");
    }

    // ========== 学习目标提取测试 ==========

    @Test
    @DisplayName("提取学习目标 - Spring关键词")
    void testExtractLearningGoal_SpringKeyword() {
        // Given
        String message = "我想学习Spring框架";

        // When
        String goal = nlpProcessor.extractLearningGoal(message);

        // Then
        assertThat(goal).isEqualTo("Spring");
    }

    @Test
    @DisplayName("提取学习目标 - Kafka关键词")
    void testExtractLearningGoal_KafkaKeyword() {
        // Given
        String message = "学习消息队列";

        // When
        String goal = nlpProcessor.extractLearningGoal(message);

        // Then
        assertThat(goal).isEqualTo("Kafka");
    }

    @Test
    @DisplayName("提取学习目标 - Netty关键词")
    void testExtractLearningGoal_NettyKeyword() {
        // Given
        String message = "想掌握Netty网络编程";

        // When
        String goal = nlpProcessor.extractLearningGoal(message);

        // Then
        assertThat(goal).isEqualTo("Netty");
    }

    @Test
    @DisplayName("提取学习目标 - 使用正则匹配")
    void testExtractLearningGoal_UsingRegex() {
        // Given
        String message = "想学Redis";

        // When
        String goal = nlpProcessor.extractLearningGoal(message);

        // Then
        assertThat(goal).isNotNull().isEqualTo("Redis");
    }

    @Test
    @DisplayName("无法提取学习目标返回null")
    void testExtractLearningGoal_NoGoal_ReturnsNull() {
        // Given
        String message = "你好";

        // When
        String goal = nlpProcessor.extractLearningGoal(message);

        // Then
        assertThat(goal).isNull();
    }

    // ========== 用户水平评估测试 ==========

    @Test
    @DisplayName("评估用户水平 - 初学者关键词")
    void testAssessUserLevel_BeginnerKeyword() {
        // Given
        String message = "我是新手，刚开始学";

        // When
        UserLevel level = nlpProcessor.assessUserLevel(message);

        // Then
        assertThat(level).isEqualTo(UserLevel.BEGINNER);
    }

    @Test
    @DisplayName("评估用户水平 - 中级关键词")
    void testAssessUserLevel_IntermediateKeyword() {
        // Given
        String message = "我有点经验，了解一些基础";

        // When
        UserLevel level = nlpProcessor.assessUserLevel(message);

        // Then
        assertThat(level).isEqualTo(UserLevel.INTERMEDIATE);
    }

    @Test
    @DisplayName("评估用户水平 - 高级关键词")
    void testAssessUserLevel_AdvancedKeyword() {
        // Given
        String message = "我很熟悉，经验丰富";

        // When
        UserLevel level = nlpProcessor.assessUserLevel(message);

        // Then
        assertThat(level).isEqualTo(UserLevel.ADVANCED);
    }

    @Test
    @DisplayName("评估用户水平 - 基于经验年限（1年）")
    void testAssessUserLevel_BasedOnYears_OneYear() {
        // Given
        String message = "我有1年经验";

        // When
        UserLevel level = nlpProcessor.assessUserLevel(message);

        // Then
        assertThat(level).isEqualTo(UserLevel.BEGINNER);
    }

    @Test
    @DisplayName("评估用户水平 - 基于经验年限（3年）")
    void testAssessUserLevel_BasedOnYears_ThreeYears() {
        // Given
        String message = "我有3年工作经验";

        // When
        UserLevel level = nlpProcessor.assessUserLevel(message);

        // Then
        assertThat(level).isEqualTo(UserLevel.INTERMEDIATE);
    }

    @Test
    @DisplayName("评估用户水平 - 基于经验年限（6年）")
    void testAssessUserLevel_BasedOnYears_SixYears() {
        // Given
        String message = "有6年开发经验";

        // When
        UserLevel level = nlpProcessor.assessUserLevel(message);

        // Then
        assertThat(level).isEqualTo(UserLevel.ADVANCED);
    }

    @Test
    @DisplayName("评估用户水平 - 无明确信息时默认中级")
    void testAssessUserLevel_NoInfo_DefaultsToIntermediate() {
        // Given
        String message = "我想学习";

        // When
        UserLevel level = nlpProcessor.assessUserLevel(message);

        // Then
        assertThat(level).isEqualTo(UserLevel.INTERMEDIATE);
    }

    // ========== 置信度计算测试 ==========

    @Test
    @DisplayName("计算置信度 - 有技术实体时置信度较高")
    void testCalculateConfidence_WithTechnologyEntity_HighConfidence() {
        // Given
        String intent = "set_learning_goal";
        Map<String, Object> entities = Map.of("technology", "Spring");

        // When
        double confidence = nlpProcessor.calculateConfidence(intent, entities);

        // Then
        assertThat(confidence).isGreaterThan(0.7);
    }

    @Test
    @DisplayName("计算置信度 - 有技能水平实体时置信度较高")
    void testCalculateConfidence_WithSkillLevelEntity_HighConfidence() {
        // Given
        String intent = "assess_skill";
        Map<String, Object> entities = Map.of("skill_level", UserLevel.BEGINNER);

        // When
        double confidence = nlpProcessor.calculateConfidence(intent, entities);

        // Then
        assertThat(confidence).isGreaterThan(0.7);
    }

    @Test
    @DisplayName("计算置信度 - 实体越多置信度越高")
    void testCalculateConfidence_MoreEntities_HigherConfidence() {
        // Given
        String intent = "set_learning_goal";
        Map<String, Object> fewEntities = Map.of("technology", "Spring");
        Map<String, Object> moreEntities = Map.of(
            "technology", "Spring",
            "skill_level", UserLevel.BEGINNER,
            "time_duration", "4周"
        );

        // When
        double confidence1 = nlpProcessor.calculateConfidence(intent, fewEntities);
        double confidence2 = nlpProcessor.calculateConfidence(intent, moreEntities);

        // Then
        assertThat(confidence2).isGreaterThan(confidence1);
    }

    @Test
    @DisplayName("计算置信度 - 最大值不超过1.0")
    void testCalculateConfidence_MaxValueIsOne() {
        // Given
        String intent = "set_learning_goal";
        Map<String, Object> manyEntities = Map.of(
            "technology", "Spring",
            "skill_level", UserLevel.BEGINNER,
            "time_duration", "4周",
            "extra1", "value1",
            "extra2", "value2"
        );

        // When
        double confidence = nlpProcessor.calculateConfidence(intent, manyEntities);

        // Then
        assertThat(confidence).isLessThanOrEqualTo(1.0);
    }

    // ========== 综合场景测试 ==========

    @Test
    @DisplayName("完整流程 - 识别学习Spring的初学者")
    void testCompleteFlow_BeginnerLearningSpring() {
        // Given
        String message = "我是初学者，想学习Spring Boot框架，大概需要2个月时间";

        // When
        Map<String, Object> entities = nlpProcessor.extractEntities(message);
        String goal = nlpProcessor.extractLearningGoal(message);
        UserLevel level = nlpProcessor.assessUserLevel(message);
        String intent = nlpProcessor.extractIntent(message, testState);

        // Then
        assertThat(entities).containsKeys("technology", "skill_level", "time_duration");
        assertThat(goal).isEqualTo("Spring");
        assertThat(level).isEqualTo(UserLevel.BEGINNER);
        assertThat(intent).isEqualTo("set_learning_goal");
    }

    @Test
    @DisplayName("大小写不敏感 - Spring/spring/SPRING应识别为相同")
    void testCaseInsensitive_SpringVariants() {
        // Given
        String[] messages = {"学习SPRING", "学习Spring", "学习spring"};

        // When & Then
        for (String message : messages) {
            String goal = nlpProcessor.extractLearningGoal(message);
            assertThat(goal).isEqualTo("Spring");
        }
    }
}
