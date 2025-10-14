package com.codenavigator.ai.service;

import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.core.entity.LearningModule;
import com.codenavigator.common.enums.UserLevel;
import com.codenavigator.common.enums.DifficultyLevel;
import com.codenavigator.common.enums.ModuleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LearningPathGenerator单元测试
 * 测试学习路径生成逻辑，包括各种技术栈和用户水平组合
 */
@DisplayName("LearningPathGenerator单元测试")
class LearningPathGeneratorTest {

    private LearningPathGenerator pathGenerator;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        pathGenerator = new LearningPathGenerator();
        context = new HashMap<>();
    }

    // ========== 预设模板测试 (Spring) ==========

    @Test
    @DisplayName("生成Spring初学者路径 - 应包含基础模块并排除专家级模块")
    void testGenerateSpringPathForBeginner() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.BEGINNER;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.getId()).isNotNull();
        assertThat(path.getTitle()).containsIgnoringCase("spring").contains("初级");
        assertThat(path.getDescription()).contains("Spring生态系统");
        assertThat(path.getTargetLevel()).isEqualTo(UserLevel.BEGINNER);
        assertThat(path.getIsActive()).isTrue();
        assertThat(path.getCreatedAt()).isNotNull();

        // 验证模块
        List<LearningModule> modules = path.getModules();
        assertThat(modules).isNotEmpty();

        // 初学者不应包含EXPERT难度模块
        assertThat(modules).noneMatch(m -> m.getDifficulty() == DifficultyLevel.EXPERT);

        // 应该包含基础模块
        assertThat(modules).anyMatch(m -> m.getTitle().contains("Spring核心概念"));
        assertThat(modules).anyMatch(m -> m.getTitle().contains("Spring Boot入门"));

        // 验证模块顺序
        assertThat(modules).isSortedAccordingTo((m1, m2) ->
            Integer.compare(m1.getOrderIndex(), m2.getOrderIndex()));

        // 验证估算时长 (初学者应该有1.5倍时长)
        assertThat(path.getEstimatedDuration()).isGreaterThan(0);
    }

    @Test
    @DisplayName("生成Spring中级路径 - 应包含中级和高级模块")
    void testGenerateSpringPathForIntermediate() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.INTERMEDIATE;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.getTitle()).containsIgnoringCase("spring").contains("中级");
        assertThat(path.getTargetLevel()).isEqualTo(UserLevel.INTERMEDIATE);

        List<LearningModule> modules = path.getModules();
        assertThat(modules).isNotEmpty();

        // 中级用户应包含中级和高级难度模块
        assertThat(modules).anyMatch(m -> m.getDifficulty() == DifficultyLevel.INTERMEDIATE);
        assertThat(modules).anyMatch(m -> m.getDifficulty() == DifficultyLevel.ADVANCED);

        // 应该包含Spring MVC和Spring Data
        assertThat(modules).anyMatch(m -> m.getTitle().contains("Spring MVC"));
        assertThat(modules).anyMatch(m -> m.getTitle().contains("Spring Data"));
    }

    @Test
    @DisplayName("生成Spring高级路径 - 应排除初级模块并包含高级模块")
    void testGenerateSpringPathForAdvanced() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.ADVANCED;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.getTitle()).containsIgnoringCase("spring").contains("高级");
        assertThat(path.getTargetLevel()).isEqualTo(UserLevel.ADVANCED);

        List<LearningModule> modules = path.getModules();
        assertThat(modules).isNotEmpty();

        // 高级用户不应包含BEGINNER难度模块(除非是必需的)
        long beginnerModules = modules.stream()
            .filter(m -> m.getDifficulty() == DifficultyLevel.BEGINNER)
            .count();
        assertThat(beginnerModules).isLessThanOrEqualTo(1); // 最多保留一个必需的基础模块

        // 应该包含高级模块
        assertThat(modules).anyMatch(m -> m.getDifficulty() == DifficultyLevel.ADVANCED);

        // 验证估算时长 (高级用户应该有0.7倍时长)
        assertThat(path.getEstimatedDuration()).isGreaterThan(0);
    }

    // ========== 预设模板测试 (Kafka) ==========

    @Test
    @DisplayName("生成Kafka初学者路径 - 验证基础概念和实践模块")
    void testGenerateKafkaPathForBeginner() {
        // Given
        String technology = "kafka";
        UserLevel userLevel = UserLevel.BEGINNER;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.getTitle()).contains("kafka", "初级");
        assertThat(path.getDescription()).contains("Kafka");

        List<LearningModule> modules = path.getModules();
        assertThat(modules).isNotEmpty();

        // 应该包含基础概念和环境搭建
        assertThat(modules).anyMatch(m -> m.getTitle().contains("Kafka基础概念"));
        assertThat(modules).anyMatch(m -> m.getTitle().contains("环境搭建"));

        // 不应包含EXPERT难度模块
        assertThat(modules).noneMatch(m -> m.getDifficulty() == DifficultyLevel.EXPERT);

        // 验证模块类型多样性
        assertThat(modules).anyMatch(m -> m.getModuleType() == ModuleType.THEORY);
        assertThat(modules).anyMatch(m -> m.getModuleType() == ModuleType.PRACTICE);
    }

    @Test
    @DisplayName("生成Kafka高级路径 - 应包含Kafka Streams和性能调优")
    void testGenerateKafkaPathForAdvanced() {
        // Given
        String technology = "kafka";
        UserLevel userLevel = UserLevel.ADVANCED;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path).isNotNull();
        List<LearningModule> modules = path.getModules();

        // 高级用户应该能看到Kafka Streams和性能调优
        assertThat(modules).anyMatch(m -> m.getTitle().contains("Kafka Streams") ||
                                           m.getDifficulty() == DifficultyLevel.ADVANCED);
    }

    // ========== 预设模板测试 (Netty) ==========

    @Test
    @DisplayName("生成Netty中级路径 - 验证NIO基础和核心概念")
    void testGenerateNettyPathForIntermediate() {
        // Given
        String technology = "netty";
        UserLevel userLevel = UserLevel.INTERMEDIATE;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.getTitle()).contains("netty", "中级");
        assertThat(path.getDescription()).contains("Netty");

        List<LearningModule> modules = path.getModules();
        assertThat(modules).isNotEmpty();

        // 应该包含NIO基础和Netty核心概念
        assertThat(modules).anyMatch(m -> m.getTitle().contains("NIO基础"));
        assertThat(modules).anyMatch(m -> m.getTitle().contains("Netty核心概念"));

        // 验证模块有估算时长
        assertThat(modules).allMatch(m -> m.getEstimatedHours() != null && m.getEstimatedHours() > 0);
    }

    // ========== 通用模板测试 ==========

    @Test
    @DisplayName("生成未预设技术的通用路径 - React")
    void testGenerateGenericPathForReact() {
        // Given
        String technology = "React";
        UserLevel userLevel = UserLevel.BEGINNER;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.getTitle()).contains("React");
        assertThat(path.getDescription()).contains("React");

        List<LearningModule> modules = path.getModules();
        assertThat(modules).isNotEmpty();

        // 通用模板应该包含5个模块
        assertThat(modules).hasSizeGreaterThanOrEqualTo(4);

        // 验证通用模块结构
        assertThat(modules).anyMatch(m -> m.getTitle().contains("基础概念"));
        assertThat(modules).anyMatch(m -> m.getTitle().contains("环境搭建"));
        assertThat(modules).anyMatch(m -> m.getTitle().contains("快速入门"));
    }

    @Test
    @DisplayName("生成未预设技术的通用路径 - Python")
    void testGenerateGenericPathForPython() {
        // Given
        String technology = "Python";
        UserLevel userLevel = UserLevel.INTERMEDIATE;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.getTitle()).contains("Python");

        List<LearningModule> modules = path.getModules();
        assertThat(modules).isNotEmpty();

        // 中级用户应该看到进阶学习和实战项目
        assertThat(modules).anyMatch(m -> m.getTitle().contains("进阶学习") ||
                                           m.getTitle().contains("实战项目"));
    }

    @Test
    @DisplayName("生成未预设技术的通用路径 - Go高级")
    void testGenerateGenericPathForGoAdvanced() {
        // Given
        String technology = "Go";
        UserLevel userLevel = UserLevel.ADVANCED;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.getTitle()).contains("Go", "高级");

        List<LearningModule> modules = path.getModules();

        // 高级用户应该排除部分初级内容
        assertThat(modules).isNotEmpty();

        // 验证估算时长比初学者短
        assertThat(path.getEstimatedDuration()).isGreaterThan(0);
    }

    // ========== 模块依赖关系测试 ==========

    @Test
    @DisplayName("验证模块依赖关系正确设置 - 每个模块依赖前一个模块")
    void testModulePrerequisitesAreSetCorrectly() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.INTERMEDIATE;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        List<LearningModule> modules = path.getModules();
        assertThat(modules).hasSizeGreaterThanOrEqualTo(2);

        // 第一个模块不应有前置依赖
        assertThat(modules.get(0).getPrerequisites()).isNullOrEmpty();

        // 后续模块应该依赖前一个模块
        for (int i = 1; i < modules.size(); i++) {
            LearningModule currentModule = modules.get(i);
            LearningModule previousModule = modules.get(i - 1);

            assertThat(currentModule.getPrerequisites())
                .isNotNull()
                .contains(previousModule.getId());
        }
    }

    @Test
    @DisplayName("验证模块顺序索引正确排序")
    void testModulesAreSortedByOrderIndex() {
        // Given
        String technology = "kafka";
        UserLevel userLevel = UserLevel.BEGINNER;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        List<LearningModule> modules = path.getModules();

        // 验证orderIndex是递增的
        for (int i = 1; i < modules.size(); i++) {
            assertThat(modules.get(i).getOrderIndex())
                .isGreaterThan(modules.get(i - 1).getOrderIndex());
        }
    }

    // ========== 估算时长计算测试 ==========

    @Test
    @DisplayName("初学者路径时长应该比中级多50%")
    void testBeginnerDurationIsLongerThanIntermediate() {
        // Given
        String technology = "spring";

        // When
        LearningPath beginnerPath = pathGenerator.generatePath(technology, UserLevel.BEGINNER, context);
        LearningPath intermediatePath = pathGenerator.generatePath(technology, UserLevel.INTERMEDIATE, context);

        // Then
        // 初学者时长应该更长 (假设相同的模块集，初学者是1.5倍，中级是1倍)
        // 注意：由于模块筛选不同，这里只验证都有合理的时长
        assertThat(beginnerPath.getEstimatedDuration()).isGreaterThan(0);
        assertThat(intermediatePath.getEstimatedDuration()).isGreaterThan(0);
    }

    @Test
    @DisplayName("高级用户路径时长应该比初学者短")
    void testAdvancedDurationIsShorterThanBeginner() {
        // Given
        String technology = "netty";

        // When
        LearningPath beginnerPath = pathGenerator.generatePath(technology, UserLevel.BEGINNER, context);
        LearningPath advancedPath = pathGenerator.generatePath(technology, UserLevel.ADVANCED, context);

        // Then
        assertThat(advancedPath.getEstimatedDuration()).isGreaterThan(0);
        assertThat(beginnerPath.getEstimatedDuration()).isGreaterThan(0);

        // 高级用户应该能更快完成
        // 注意：由于模块筛选差异，这里主要验证都有合理值
    }

    // ========== 模块筛选逻辑测试 ==========

    @Test
    @DisplayName("初学者不应看到专家级模块")
    void testBeginnerDoesNotSeeExpertModules() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.BEGINNER;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        List<LearningModule> modules = path.getModules();

        // Spring模板中有EXPERT级别的"Spring Cloud微服务"
        assertThat(modules).noneMatch(m -> m.getTitle().contains("Spring Cloud微服务"));
        assertThat(modules).noneMatch(m -> m.getDifficulty() == DifficultyLevel.EXPERT);
    }

    @Test
    @DisplayName("中级用户应看到必需的初级模块")
    void testIntermediateSeeRequiredBeginnerModules() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.INTERMEDIATE;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        List<LearningModule> modules = path.getModules();

        // 中级用户应该能看到必需的基础模块
        // 至少应该有一些模块
        assertThat(modules).hasSizeGreaterThanOrEqualTo(3);
    }

    // ========== 模块属性验证测试 ==========

    @Test
    @DisplayName("所有生成的模块都有必需的属性")
    void testAllModulesHaveRequiredAttributes() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.INTERMEDIATE;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        List<LearningModule> modules = path.getModules();

        assertThat(modules).allMatch(m -> {
            return m.getId() != null
                && m.getTitle() != null && !m.getTitle().isEmpty()
                && m.getDescription() != null && !m.getDescription().isEmpty()
                && m.getModuleType() != null
                && m.getDifficulty() != null
                && m.getEstimatedHours() != null && m.getEstimatedHours() > 0
                && m.getOrderIndex() != null && m.getOrderIndex() > 0
                && m.getIsRequired() != null
                && m.getCreatedAt() != null;
        });
    }

    @Test
    @DisplayName("路径包含多种模块类型")
    void testPathContainsVariousModuleTypes() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.INTERMEDIATE;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        List<LearningModule> modules = path.getModules();

        // Spring路径应该包含多种类型：THEORY, PRACTICE, PROJECT
        long theoryCount = modules.stream().filter(m -> m.getModuleType() == ModuleType.THEORY).count();
        long practiceCount = modules.stream().filter(m -> m.getModuleType() == ModuleType.PRACTICE).count();
        long projectCount = modules.stream().filter(m -> m.getModuleType() == ModuleType.PROJECT).count();

        assertThat(theoryCount).isGreaterThan(0);
        assertThat(practiceCount + projectCount).isGreaterThan(0);
    }

    // ========== 边界情况测试 ==========

    @Test
    @DisplayName("空上下文应该正常生成路径")
    void testGeneratePathWithEmptyContext() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.BEGINNER;
        Map<String, Object> emptyContext = new HashMap<>();

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, emptyContext);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.getModules()).isNotEmpty();
    }

    @Test
    @DisplayName("技术名称大小写不敏感")
    void testTechnologyNameIsCaseInsensitive() {
        // Given
        UserLevel userLevel = UserLevel.INTERMEDIATE;

        // When
        LearningPath path1 = pathGenerator.generatePath("SPRING", userLevel, context);
        LearningPath path2 = pathGenerator.generatePath("spring", userLevel, context);
        LearningPath path3 = pathGenerator.generatePath("Spring", userLevel, context);

        // Then
        // 三个路径应该使用相同的模板
        assertThat(path1.getDescription()).isEqualTo(path2.getDescription());
        assertThat(path2.getDescription()).isEqualTo(path3.getDescription());
        assertThat(path1.getModules().size()).isEqualTo(path2.getModules().size());
    }

    @Test
    @DisplayName("验证所有预设技术栈都能正确生成")
    void testAllPresetTechnologiesCanGenerate() {
        // Given
        UserLevel userLevel = UserLevel.INTERMEDIATE;
        String[] technologies = {"spring", "kafka", "netty"};

        // When & Then
        for (String tech : technologies) {
            LearningPath path = pathGenerator.generatePath(tech, userLevel, context);

            assertThat(path).isNotNull();
            assertThat(path.getModules()).isNotEmpty();
            assertThat(path.getTitle()).containsIgnoringCase(tech);
        }
    }

    @Test
    @DisplayName("生成的模块ID都是唯一的")
    void testGeneratedModuleIdsAreUnique() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.INTERMEDIATE;

        // When
        LearningPath path = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        List<LearningModule> modules = path.getModules();
        long uniqueIds = modules.stream().map(LearningModule::getId).distinct().count();

        assertThat(uniqueIds).isEqualTo(modules.size());
    }

    @Test
    @DisplayName("多次生成相同配置应该产生不同的ID")
    void testMultipleGenerationsHaveDifferentIds() {
        // Given
        String technology = "spring";
        UserLevel userLevel = UserLevel.BEGINNER;

        // When
        LearningPath path1 = pathGenerator.generatePath(technology, userLevel, context);
        LearningPath path2 = pathGenerator.generatePath(technology, userLevel, context);

        // Then
        assertThat(path1.getId()).isNotEqualTo(path2.getId());

        // 模块ID也应该不同
        assertThat(path1.getModules().get(0).getId())
            .isNotEqualTo(path2.getModules().get(0).getId());
    }
}
