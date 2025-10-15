package com.codenavigator.ai.service;

import com.codenavigator.common.enums.DifficultyLevel;
import com.codenavigator.common.enums.ModuleType;
import com.codenavigator.common.enums.UserLevel;
import com.codenavigator.core.entity.LearningModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskDifficultyAssessor单元测试
 * 测试任务难度评估逻辑
 */
@DisplayName("TaskDifficultyAssessor单元测试")
class TaskDifficultyAssessorTest {

    private TaskDifficultyAssessor assessor;

    @BeforeEach
    void setUp() {
        assessor = new TaskDifficultyAssessor();
    }

    // ========== 模块难度评估测试 ==========

    @Test
    @DisplayName("评估模块难度 - 简单理论模块")
    void testAssessModuleDifficulty_SimpleTheoryModule() {
        // Given
        LearningModule module = createModule("Java基础入门", "介绍Java基础语法",
                                            ModuleType.THEORY, 2);

        // When
        DifficultyLevel difficulty = assessor.assessModuleDifficulty(module, "Java");

        // Then
        assertThat(difficulty).isEqualTo(DifficultyLevel.BEGINNER);
    }

    @Test
    @DisplayName("评估模块难度 - 复杂实践模块")
    void testAssessModuleDifficulty_ComplexPracticeModule() {
        // Given
        LearningModule module = createModule("Spring Cloud微服务架构",
                                            "深入理解分布式微服务架构设计",
                                            ModuleType.PROJECT, 15);

        // When
        DifficultyLevel difficulty = assessor.assessModuleDifficulty(module, "Spring");

        // Then
        assertThat(difficulty).isIn(DifficultyLevel.ADVANCED, DifficultyLevel.EXPERT);
    }

    @Test
    @DisplayName("评估模块难度 - Kafka进阶模块")
    void testAssessModuleDifficulty_KafkaAdvanced() {
        // Given
        LearningModule module = createModule("Kafka Streams深入",
                                            "深入学习Kafka流处理",
                                            ModuleType.PRACTICE, 10);

        // When
        DifficultyLevel difficulty = assessor.assessModuleDifficulty(module, "Kafka");

        // Then
        assertThat(difficulty).isIn(DifficultyLevel.INTERMEDIATE, DifficultyLevel.ADVANCED);
    }

    @Test
    @DisplayName("评估模块难度 - Netty源码分析")
    void testAssessModuleDifficulty_NettySourceCode() {
        // Given
        LearningModule module = createModule("Netty源码深入分析",
                                            "源码级别深入分析Netty核心机制",
                                            ModuleType.THEORY, 20);

        // When
        DifficultyLevel difficulty = assessor.assessModuleDifficulty(module, "Netty");

        // Then
        assertThat(difficulty).isEqualTo(DifficultyLevel.EXPERT);
    }

    // ========== 用户能力差距评估测试 ==========

    @Test
    @DisplayName("评估能力差距 - 初学者vs简单模块")
    void testAssessCapabilityGap_BeginnerVsSimpleModule() {
        // Given
        LearningModule module = createModule("基础入门", "简单介绍",
                                            ModuleType.THEORY, 2);

        // When
        DifficultyLevel gap = assessor.assessUserCapabilityGap(UserLevel.BEGINNER, module);

        // Then
        assertThat(gap).isEqualTo(DifficultyLevel.BEGINNER);
    }

    @Test
    @DisplayName("评估能力差距 - 初学者vs复杂模块")
    void testAssessCapabilityGap_BeginnerVsComplexModule() {
        // Given
        LearningModule module = createModule("高级架构设计",
                                            "深入分析分布式系统架构",
                                            ModuleType.PROJECT, 20);

        // When
        DifficultyLevel gap = assessor.assessUserCapabilityGap(UserLevel.BEGINNER, module);

        // Then
        assertThat(gap).isIn(DifficultyLevel.ADVANCED, DifficultyLevel.EXPERT);
    }

    @Test
    @DisplayName("评估能力差距 - 高级用户vs简单模块")
    void testAssessCapabilityGap_AdvancedVsSimpleModule() {
        // Given
        LearningModule module = createModule("基础语法", "入门知识",
                                            ModuleType.TUTORIAL, 1);

        // When
        DifficultyLevel gap = assessor.assessUserCapabilityGap(UserLevel.ADVANCED, module);

        // Then
        assertThat(gap).isEqualTo(DifficultyLevel.BEGINNER);
    }

    @Test
    @DisplayName("评估能力差距 - 中级用户vs中级模块")
    void testAssessCapabilityGap_IntermediateVsIntermediateModule() {
        // Given
        LearningModule module = createModule("进阶学习", "深入理解核心概念",
                                            ModuleType.PRACTICE, 8);

        // When
        DifficultyLevel gap = assessor.assessUserCapabilityGap(UserLevel.INTERMEDIATE, module);

        // Then
        assertThat(gap).isIn(DifficultyLevel.BEGINNER, DifficultyLevel.INTERMEDIATE);
    }

    // ========== 完成时间估算测试 ==========

    @Test
    @DisplayName("估算完成时间 - 初学者完成简单模块")
    void testEstimateCompletionTime_BeginnerSimpleModule() {
        // Given
        LearningModule module = createModule("基础入门", "简单知识",
                                            ModuleType.THEORY, 2);

        // When
        int estimatedTime = assessor.estimateCompletionTime(module, UserLevel.BEGINNER);

        // Then
        assertThat(estimatedTime).isGreaterThan(2);  // 初学者需要更多时间
        assertThat(estimatedTime).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("估算完成时间 - 高级用户完成复杂模块")
    void testEstimateCompletionTime_AdvancedComplexModule() {
        // Given
        LearningModule module = createModule("高级特性", "深入学习",
                                            ModuleType.PRACTICE, 10);

        // When
        int estimatedTime = assessor.estimateCompletionTime(module, UserLevel.ADVANCED);

        // Then
        assertThat(estimatedTime).isGreaterThan(0);
        assertThat(estimatedTime).isLessThanOrEqualTo(100);  // 在合理范围内
    }

    @Test
    @DisplayName("估算完成时间 - 中级用户标准时间")
    void testEstimateCompletionTime_IntermediateStandardTime() {
        // Given
        LearningModule module = createModule("标准模块", "正常难度",
                                            ModuleType.PRACTICE, 5);

        // When
        int estimatedTime = assessor.estimateCompletionTime(module, UserLevel.INTERMEDIATE);

        // Then
        assertThat(estimatedTime).isGreaterThan(0);
        assertThat(estimatedTime).isLessThanOrEqualTo(50);
    }

    @Test
    @DisplayName("估算完成时间 - 不同用户水平的时间差异")
    void testEstimateCompletionTime_DifferentUserLevels() {
        // Given
        LearningModule module = createModule("测试模块", "测试内容",
                                            ModuleType.PRACTICE, 10);

        // When
        int beginnerTime = assessor.estimateCompletionTime(module, UserLevel.BEGINNER);
        int intermediateTime = assessor.estimateCompletionTime(module, UserLevel.INTERMEDIATE);
        int advancedTime = assessor.estimateCompletionTime(module, UserLevel.ADVANCED);

        // Then
        assertThat(beginnerTime).isGreaterThan(intermediateTime);
        assertThat(intermediateTime).isGreaterThan(advancedTime);
    }

    // ========== 模块复杂度分析测试 ==========

    @Test
    @DisplayName("分析模块复杂度 - 应返回所有复杂度指标")
    void testAnalyzeModuleComplexity_ReturnsAllMetrics() {
        // Given
        LearningModule module = createModule("Spring MVC", "深入学习MVC框架",
                                            ModuleType.PRACTICE, 8);

        // When
        Map<String, Double> analysis = assessor.analyzeModuleComplexity(module);

        // Then
        assertThat(analysis).containsKeys(
            "moduleTypeComplexity",
            "contentComplexity",
            "timeComplexity",
            "prerequisiteComplexity",
            "overallComplexity"
        );
        assertThat(analysis.get("overallComplexity")).isGreaterThan(0);
    }

    @Test
    @DisplayName("分析模块复杂度 - 实践模块比理论模块复杂")
    void testAnalyzeModuleComplexity_PracticeMoreComplexThanTheory() {
        // Given
        LearningModule theoryModule = createModule("理论知识", "理论学习",
                                                   ModuleType.THEORY, 5);
        LearningModule practiceModule = createModule("实践练习", "动手实践",
                                                     ModuleType.PRACTICE, 5);

        // When
        Map<String, Double> theoryAnalysis = assessor.analyzeModuleComplexity(theoryModule);
        Map<String, Double> practiceAnalysis = assessor.analyzeModuleComplexity(practiceModule);

        // Then
        assertThat(practiceAnalysis.get("moduleTypeComplexity"))
            .isGreaterThan(theoryAnalysis.get("moduleTypeComplexity"));
    }

    @Test
    @DisplayName("分析模块复杂度 - 项目模块最复杂")
    void testAnalyzeModuleComplexity_ProjectMostComplex() {
        // Given
        LearningModule theoryModule = createModule("理论", "学习", ModuleType.THEORY, 5);
        LearningModule practiceModule = createModule("实践", "练习", ModuleType.PRACTICE, 5);
        LearningModule projectModule = createModule("项目", "实战", ModuleType.PROJECT, 5);

        // When
        double theoryComplexity = assessor.analyzeModuleComplexity(theoryModule)
                                         .get("moduleTypeComplexity");
        double practiceComplexity = assessor.analyzeModuleComplexity(practiceModule)
                                           .get("moduleTypeComplexity");
        double projectComplexity = assessor.analyzeModuleComplexity(projectModule)
                                          .get("moduleTypeComplexity");

        // Then
        assertThat(projectComplexity).isGreaterThan(practiceComplexity);
        assertThat(practiceComplexity).isGreaterThan(theoryComplexity);
    }

    @Test
    @DisplayName("分析模块复杂度 - 有前置条件的模块更复杂")
    void testAnalyzeModuleComplexity_WithPrerequisites() {
        // Given
        LearningModule moduleWithoutPrereq = createModule("模块A", "内容",
                                                          ModuleType.THEORY, 5);

        LearningModule moduleWithPrereq = createModule("模块B", "内容",
                                                       ModuleType.THEORY, 5);
        moduleWithPrereq.setPrerequisites(List.of("prereq1", "prereq2"));

        // When
        double complexityWithout = assessor.analyzeModuleComplexity(moduleWithoutPrereq)
                                          .get("prerequisiteComplexity");
        double complexityWith = assessor.analyzeModuleComplexity(moduleWithPrereq)
                                       .get("prerequisiteComplexity");

        // Then
        assertThat(complexityWith).isGreaterThan(complexityWithout);
    }

    // ========== 内容复杂度分析测试 ==========

    @Test
    @DisplayName("内容复杂度 - 包含'基础'关键词应简单")
    void testContentComplexity_BasicKeyword() {
        // Given
        LearningModule module = createModule("Java基础", "基础入门知识",
                                            ModuleType.THEORY, 2);

        // When
        Map<String, Double> analysis = assessor.analyzeModuleComplexity(module);

        // Then
        assertThat(analysis.get("contentComplexity")).isLessThan(2.0);
    }

    @Test
    @DisplayName("内容复杂度 - 包含'高级'关键词应复杂")
    void testContentComplexity_AdvancedKeyword() {
        // Given
        LearningModule module = createModule("高级特性", "高级深入学习",
                                            ModuleType.THEORY, 10);

        // When
        Map<String, Double> analysis = assessor.analyzeModuleComplexity(module);

        // Then
        assertThat(analysis.get("contentComplexity")).isGreaterThan(1.5);
    }

    @Test
    @DisplayName("内容复杂度 - 包含'源码'关键词应最复杂")
    void testContentComplexity_SourceCodeKeyword() {
        // Given
        LearningModule module = createModule("框架源码分析", "深入源码级别分析",
                                            ModuleType.THEORY, 15);

        // When
        Map<String, Double> analysis = assessor.analyzeModuleComplexity(module);

        // Then
        assertThat(analysis.get("contentComplexity")).isGreaterThanOrEqualTo(2.0);
    }

    // ========== 时长复杂度测试 ==========

    @Test
    @DisplayName("时长复杂度 - 短时长模块简单")
    void testTimeComplexity_ShortDuration() {
        // Given
        LearningModule module = createModule("快速入门", "简短内容",
                                            ModuleType.TUTORIAL, 2);

        // When
        Map<String, Double> analysis = assessor.analyzeModuleComplexity(module);

        // Then
        assertThat(analysis.get("timeComplexity")).isLessThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("时长复杂度 - 长时长模块复杂")
    void testTimeComplexity_LongDuration() {
        // Given
        LearningModule module = createModule("深入学习", "详细内容",
                                            ModuleType.PROJECT, 20);

        // When
        Map<String, Double> analysis = assessor.analyzeModuleComplexity(module);

        // Then
        assertThat(analysis.get("timeComplexity")).isGreaterThan(1.0);
    }

    // ========== 边界情况测试 ==========

    @Test
    @DisplayName("空描述 - 应使用默认复杂度")
    void testNullDescription_UsesDefaultComplexity() {
        // Given
        LearningModule module = createModule("测试", null, ModuleType.THEORY, 5);

        // When
        Map<String, Double> analysis = assessor.analyzeModuleComplexity(module);

        // Then
        assertThat(analysis.get("contentComplexity")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("极短时长 - 应返回最小估算时间")
    void testVeryShortDuration_ReturnsMinimumTime() {
        // Given
        LearningModule module = createModule("超短模块", "很短",
                                            ModuleType.QUIZ, 0);
        module.setEstimatedHours(0);

        // When
        int estimatedTime = assessor.estimateCompletionTime(module, UserLevel.INTERMEDIATE);

        // Then
        assertThat(estimatedTime).isGreaterThanOrEqualTo(1);  // 至少1小时
    }

    @Test
    @DisplayName("极长时长 - 应限制在最大估算时间")
    void testVeryLongDuration_CapsAtMaximumTime() {
        // Given
        LearningModule module = createModule("超长模块", "很长很复杂",
                                            ModuleType.PROJECT, 200);
        module.setEstimatedHours(200);

        // When
        int estimatedTime = assessor.estimateCompletionTime(module, UserLevel.BEGINNER);

        // Then
        assertThat(estimatedTime).isLessThanOrEqualTo(100);  // 最多100小时
    }

    // ========== 综合场景测试 ==========

    @Test
    @DisplayName("完整流程 - Spring微服务项目评估")
    void testCompleteFlow_SpringMicroserviceProject() {
        // Given
        LearningModule module = createModule(
            "Spring Cloud微服务架构实战",
            "深入学习微服务架构设计与实现，包括服务注册发现、配置中心、网关等",
            ModuleType.PROJECT,
            25
        );
        module.setPrerequisites(List.of("Spring基础", "Spring Boot", "分布式系统"));

        // When
        DifficultyLevel difficulty = assessor.assessModuleDifficulty(module, "Spring");
        DifficultyLevel beginnerGap = assessor.assessUserCapabilityGap(UserLevel.BEGINNER, module);
        DifficultyLevel advancedGap = assessor.assessUserCapabilityGap(UserLevel.ADVANCED, module);
        int beginnerTime = assessor.estimateCompletionTime(module, UserLevel.BEGINNER);
        int advancedTime = assessor.estimateCompletionTime(module, UserLevel.ADVANCED);
        Map<String, Double> complexity = assessor.analyzeModuleComplexity(module);

        // Then
        assertThat(difficulty).isIn(DifficultyLevel.ADVANCED, DifficultyLevel.EXPERT);
        assertThat(beginnerGap).isIn(DifficultyLevel.ADVANCED, DifficultyLevel.EXPERT);
        assertThat(advancedGap).isIn(DifficultyLevel.BEGINNER, DifficultyLevel.INTERMEDIATE);
        assertThat(beginnerTime).isGreaterThan(advancedTime);
        assertThat(complexity.get("overallComplexity")).isGreaterThan(2.0);
    }

    // ========== 辅助方法 ==========

    private LearningModule createModule(String title, String description,
                                       ModuleType type, int hours) {
        LearningModule module = new LearningModule();
        module.setTitle(title);
        module.setDescription(description);
        module.setModuleType(type);
        module.setEstimatedHours(hours);
        return module;
    }
}
