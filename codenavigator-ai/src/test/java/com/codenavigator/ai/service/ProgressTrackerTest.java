package com.codenavigator.ai.service;

import com.codenavigator.ai.service.ProgressTracker.ModuleProgress;
import com.codenavigator.ai.service.ProgressTracker.ProgressSummary;
import com.codenavigator.ai.service.ProgressTracker.StudyStreak;
import com.codenavigator.core.entity.LearningModule;
import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.core.entity.User;
import com.codenavigator.core.entity.UserProgress;
import com.codenavigator.common.enums.DifficultyLevel;
import com.codenavigator.common.enums.ModuleType;
import com.codenavigator.common.enums.ProgressStatus;
import com.codenavigator.common.enums.UserLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * ProgressTracker单元测试
 * 测试学习进度跟踪功能，包括模块完成、进度计算和成就解锁
 */
@DisplayName("ProgressTracker单元测试")
class ProgressTrackerTest {

    private ProgressTracker progressTracker;
    private User testUser;
    private LearningPath testPath;
    private List<LearningModule> testModules;

    @BeforeEach
    void setUp() {
        progressTracker = new ProgressTracker();

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setLevel(UserLevel.INTERMEDIATE);

        // 创建测试模块
        testModules = createTestModules();

        // 创建测试学习路径
        testPath = new LearningPath();
        testPath.setId(UUID.randomUUID().toString());
        testPath.setTitle("Spring Boot学习路径");
        testPath.setDescription("全面学习Spring Boot");
        testPath.setEstimatedDuration(10); // 10天
        testPath.setModules(testModules);
    }

    private List<LearningModule> createTestModules() {
        List<LearningModule> modules = new ArrayList<>();

        LearningModule module1 = LearningModule.builder()
            .id(UUID.randomUUID().toString())
            .title("Spring核心概念")
            .description("学习IoC和DI")
            .moduleType(ModuleType.THEORY)
            .difficulty(DifficultyLevel.BEGINNER)
            .estimatedHours(8)
            .orderIndex(1)
            .isRequired(true)
            .build();

        LearningModule module2 = LearningModule.builder()
            .id(UUID.randomUUID().toString())
            .title("Spring MVC")
            .description("Web层开发")
            .moduleType(ModuleType.PRACTICE)
            .difficulty(DifficultyLevel.INTERMEDIATE)
            .estimatedHours(12)
            .orderIndex(2)
            .isRequired(true)
            .build();

        LearningModule module3 = LearningModule.builder()
            .id(UUID.randomUUID().toString())
            .title("Spring Data")
            .description("数据访问层")
            .moduleType(ModuleType.PRACTICE)
            .difficulty(DifficultyLevel.INTERMEDIATE)
            .estimatedHours(10)
            .orderIndex(3)
            .isRequired(true)
            .build();

        modules.add(module1);
        modules.add(module2);
        modules.add(module3);

        return modules;
    }

    // ========== startLearningPath 测试 ==========

    @Test
    @DisplayName("启动学习路径 - 应初始化进度并设置第一个模块")
    void testStartLearningPath_Success() {
        // When
        UserProgress progress = progressTracker.startLearningPath(testUser, testPath);

        // Then
        assertThat(progress).isNotNull();
        assertThat(progress.getUser()).isEqualTo(testUser);
        assertThat(progress.getLearningPath()).isEqualTo(testPath);
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(progress.getStartedAt()).isNotNull();
        assertThat(progress.getLastActiveAt()).isNotNull();
        assertThat(progress.getCurrentModule()).isEqualTo(testModules.get(0));
        assertThat(progress.getTotalModules()).isEqualTo(3);
        assertThat(progress.getCompletedModules()).isEqualTo(0);
    }

    @Test
    @DisplayName("启动学习路径 - 空模块列表应正常处理")
    void testStartLearningPath_WithEmptyModules() {
        // Given
        LearningPath emptyPath = new LearningPath();
        emptyPath.setId(UUID.randomUUID().toString());
        emptyPath.setTitle("空路径");
        emptyPath.setModules(new ArrayList<>());

        // When
        UserProgress progress = progressTracker.startLearningPath(testUser, emptyPath);

        // Then
        assertThat(progress).isNotNull();
        assertThat(progress.getCurrentModule()).isNull();
        assertThat(progress.getTotalModules()).isEqualTo(0);
    }

    @Test
    @DisplayName("启动学习路径 - null模块列表应正常处理")
    void testStartLearningPath_WithNullModules() {
        // Given
        LearningPath nullModulesPath = new LearningPath();
        nullModulesPath.setId(UUID.randomUUID().toString());
        nullModulesPath.setTitle("无模块路径");
        nullModulesPath.setModules(null);

        // When
        UserProgress progress = progressTracker.startLearningPath(testUser, nullModulesPath);

        // Then
        assertThat(progress).isNotNull();
        assertThat(progress.getCurrentModule()).isNull();
        assertThat(progress.getTotalModules()).isEqualTo(0);
    }

    // ========== UserProgress业务方法测试 ==========

    @Test
    @DisplayName("UserProgress.startLearning - 应设置状态和时间")
    void testUserProgress_StartLearning() {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        // When
        progress.startLearning();

        // Then
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(progress.getStartedAt()).isBetween(before, after);
        assertThat(progress.getLastActiveAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("UserProgress.updateProgress - 应更新完成百分比")
    void testUserProgress_UpdateProgress_PartialCompletion() {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);
        progress.startLearning();
        progress.setCompletedModules(1);
        progress.setTotalModules(3);

        // When
        progress.updateProgress();

        // Then
        assertThat(progress.getCompletionPercentage()).isEqualTo(33.33, within(0.01));
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(progress.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("UserProgress.updateProgress - 完成所有模块应设置为COMPLETED")
    void testUserProgress_UpdateProgress_FullCompletion() {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);
        progress.startLearning();
        progress.setCompletedModules(3);
        progress.setTotalModules(3);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        // When
        progress.updateProgress();

        // Then
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(progress.getCompletionPercentage()).isEqualTo(100.0);
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(progress.getCompletedAt()).isNotNull();
        assertThat(progress.getCompletedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("UserProgress.updateProgress - 零模块应处理正常")
    void testUserProgress_UpdateProgress_ZeroModules() {
        // Given
        LearningPath emptyPath = new LearningPath();
        emptyPath.setId(UUID.randomUUID().toString());
        emptyPath.setModules(new ArrayList<>());

        UserProgress progress = new UserProgress(testUser, emptyPath);
        progress.setTotalModules(0);
        progress.setCompletedModules(0);

        // When
        progress.updateProgress();

        // Then
        assertThat(progress.getCompletionPercentage()).isEqualTo(0.0);
    }

    // ========== ProgressSummary Builder测试 ==========

    @Test
    @DisplayName("ProgressSummary.builder - 应正确构建进度摘要")
    void testProgressSummary_Builder() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<String> achievements = Arrays.asList("半程英雄", "学习达人");

        // When
        ProgressSummary summary = ProgressSummary.builder()
            .userId("user-123")
            .learningPathId("path-456")
            .learningPathTitle("Spring Boot进阶")
            .status(ProgressStatus.IN_PROGRESS)
            .completionPercentage(60.0)
            .completedModules(3)
            .totalModules(5)
            .currentModuleTitle("Spring Security")
            .timeSpent(24L)
            .estimatedTimeRemaining(16L)
            .startedAt(now.minusDays(3))
            .lastActiveAt(now)
            .achievements(achievements)
            .build();

        // Then
        assertThat(summary).isNotNull();
        // 注意：由于ProgressSummary没有公共getters，我们无法验证字段值
        // 这里主要验证builder不会抛出异常
    }

    // ========== ModuleProgress Builder测试 ==========

    @Test
    @DisplayName("ModuleProgress.builder - 应正确构建模块进度")
    void testModuleProgress_Builder() {
        // Given
        LocalDateTime completedAt = LocalDateTime.now();

        // When
        ModuleProgress moduleProgress = ModuleProgress.builder()
            .moduleId("module-123")
            .title("Spring核心概念")
            .status(ProgressStatus.COMPLETED)
            .completedAt(completedAt)
            .timeSpent(8L)
            .difficulty(DifficultyLevel.BEGINNER)
            .estimatedHours(10)
            .build();

        // Then
        assertThat(moduleProgress).isNotNull();
    }

    // ========== StudyStreak Builder测试 ==========

    @Test
    @DisplayName("StudyStreak.builder - 应正确构建学习连击")
    void testStudyStreak_Builder() {
        // Given
        LocalDateTime lastStudy = LocalDateTime.now();

        // When
        StudyStreak streak = StudyStreak.builder()
            .currentStreak(7)
            .maxStreak(15)
            .lastStudyDate(lastStudy)
            .totalStudyDays(30)
            .build();

        // Then
        assertThat(streak).isNotNull();
    }

    // ========== UserProgress构造函数测试 ==========

    @Test
    @DisplayName("UserProgress构造函数 - 应正确初始化totalModules")
    void testUserProgress_Constructor_WithModules() {
        // When
        UserProgress progress = new UserProgress(testUser, testPath);

        // Then
        assertThat(progress.getUser()).isEqualTo(testUser);
        assertThat(progress.getLearningPath()).isEqualTo(testPath);
        assertThat(progress.getTotalModules()).isEqualTo(3);
        assertThat(progress.getCompletedModules()).isEqualTo(0);
        assertThat(progress.getCompletionPercentage()).isEqualTo(0.0);
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
    }

    @Test
    @DisplayName("UserProgress构造函数 - null模块列表应设置totalModules为0")
    void testUserProgress_Constructor_WithNullModules() {
        // Given
        LearningPath pathWithoutModules = new LearningPath();
        pathWithoutModules.setId(UUID.randomUUID().toString());
        pathWithoutModules.setModules(null);

        // When
        UserProgress progress = new UserProgress(testUser, pathWithoutModules);

        // Then
        assertThat(progress.getTotalModules()).isEqualTo(0);
    }

    // ========== 边界情况和异常测试 ==========

    @Test
    @DisplayName("完成百分比计算 - 边界值测试")
    void testCompletionPercentage_BoundaryValues() {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);
        progress.startLearning();

        // Test 0%
        progress.setCompletedModules(0);
        progress.setTotalModules(10);
        progress.updateProgress();
        assertThat(progress.getCompletionPercentage()).isEqualTo(0.0);

        // Test 50%
        progress.setCompletedModules(5);
        progress.updateProgress();
        assertThat(progress.getCompletionPercentage()).isEqualTo(50.0);

        // Test 100%
        progress.setCompletedModules(10);
        progress.updateProgress();
        assertThat(progress.getCompletionPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("进度状态转换 - NOT_STARTED -> IN_PROGRESS")
    void testStatusTransition_NotStartedToInProgress() {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);

        // When
        progress.startLearning();

        // Then
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("进度状态转换 - IN_PROGRESS -> COMPLETED")
    void testStatusTransition_InProgressToCompleted() {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);
        progress.startLearning();
        progress.setCompletedModules(3);
        progress.setTotalModules(3);

        // When
        progress.updateProgress();

        // Then
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
    }

    @Test
    @DisplayName("多个模块的学习路径 - 验证总模块数计算")
    void testLearningPath_WithMultipleModules() {
        // Given - 创建包含5个模块的路径
        List<LearningModule> manyModules = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            manyModules.add(LearningModule.builder()
                .id(UUID.randomUUID().toString())
                .title("Module " + i)
                .orderIndex(i)
                .moduleType(ModuleType.PRACTICE)
                .difficulty(DifficultyLevel.INTERMEDIATE)
                .estimatedHours(8)
                .build());
        }

        LearningPath largePath = new LearningPath();
        largePath.setId(UUID.randomUUID().toString());
        largePath.setTitle("大型学习路径");
        largePath.setModules(manyModules);

        // When
        UserProgress progress = progressTracker.startLearningPath(testUser, largePath);

        // Then
        assertThat(progress.getTotalModules()).isEqualTo(5);
        assertThat(progress.getCurrentModule()).isEqualTo(manyModules.get(0));
    }

    @Test
    @DisplayName("时间戳验证 - startedAt应在lastActiveAt之前或相等")
    void testTimestamps_StartedAtBeforeLastActiveAt() {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);

        // When
        progress.startLearning();

        // Then
        assertThat(progress.getStartedAt()).isNotNull();
        assertThat(progress.getLastActiveAt()).isNotNull();
        assertThat(progress.getStartedAt())
            .isBeforeOrEqualTo(progress.getLastActiveAt());
    }

    @Test
    @DisplayName("时间戳验证 - updateProgress应更新lastActiveAt")
    void testTimestamps_UpdateProgressUpdatesLastActiveAt() throws InterruptedException {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);
        progress.startLearning();
        LocalDateTime firstActiveTime = progress.getLastActiveAt();

        Thread.sleep(10); // 等待一小段时间

        // When
        progress.setCompletedModules(1);
        progress.updateProgress();

        // Then
        assertThat(progress.getLastActiveAt()).isAfter(firstActiveTime);
    }

    @Test
    @DisplayName("完成时间验证 - 未完成时completedAt应为null")
    void testCompletedAt_NullWhenNotCompleted() {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);
        progress.startLearning();
        progress.setCompletedModules(1);
        progress.setTotalModules(3);

        // When
        progress.updateProgress();

        // Then
        assertThat(progress.getCompletedAt()).isNull();
        assertThat(progress.getStatus()).isNotEqualTo(ProgressStatus.COMPLETED);
    }

    @Test
    @DisplayName("完成时间验证 - 完成时completedAt应被设置")
    void testCompletedAt_SetWhenCompleted() {
        // Given
        UserProgress progress = new UserProgress(testUser, testPath);
        progress.startLearning();
        progress.setCompletedModules(3);
        progress.setTotalModules(3);

        // When
        progress.updateProgress();

        // Then
        assertThat(progress.getCompletedAt()).isNotNull();
        assertThat(progress.getCompletedAt()).isAfterOrEqualTo(progress.getStartedAt());
    }

    // ========== Getters和Setters测试 ==========

    @Test
    @DisplayName("UserProgress - Getters和Setters正常工作")
    void testUserProgress_GettersAndSetters() {
        // Given
        UserProgress progress = new UserProgress();
        LocalDateTime now = LocalDateTime.now();

        // When
        progress.setId(100L);
        progress.setUser(testUser);
        progress.setLearningPath(testPath);
        progress.setCurrentModule(testModules.get(0));
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setCompletedModules(2);
        progress.setTotalModules(5);
        progress.setCompletionPercentage(40.0);
        progress.setModuleProgress("{\"module1\":\"completed\"}");
        progress.setStartedAt(now);
        progress.setLastActiveAt(now);
        progress.setCompletedAt(now);

        // Then
        assertThat(progress.getId()).isEqualTo(100L);
        assertThat(progress.getUser()).isEqualTo(testUser);
        assertThat(progress.getLearningPath()).isEqualTo(testPath);
        assertThat(progress.getCurrentModule()).isEqualTo(testModules.get(0));
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(progress.getCompletedModules()).isEqualTo(2);
        assertThat(progress.getTotalModules()).isEqualTo(5);
        assertThat(progress.getCompletionPercentage()).isEqualTo(40.0);
        assertThat(progress.getModuleProgress()).isEqualTo("{\"module1\":\"completed\"}");
        assertThat(progress.getStartedAt()).isEqualTo(now);
        assertThat(progress.getLastActiveAt()).isEqualTo(now);
        assertThat(progress.getCompletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("UserProgress - 默认值验证")
    void testUserProgress_DefaultValues() {
        // When
        UserProgress progress = new UserProgress();

        // Then
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(progress.getCompletedModules()).isEqualTo(0);
        assertThat(progress.getTotalModules()).isEqualTo(0);
        assertThat(progress.getCompletionPercentage()).isEqualTo(0.0);
    }

    // ========== 集成场景测试 ==========

    @Test
    @DisplayName("完整学习流程 - 从开始到完成")
    void testCompletelearningFlow() {
        // 1. 开始学习
        UserProgress progress = progressTracker.startLearningPath(testUser, testPath);
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(progress.getCompletionPercentage()).isEqualTo(0.0);

        // 2. 完成第一个模块
        progress.setCompletedModules(1);
        progress.updateProgress();
        assertThat(progress.getCompletionPercentage()).isEqualTo(33.33, within(0.01));
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);

        // 3. 完成第二个模块
        progress.setCompletedModules(2);
        progress.updateProgress();
        assertThat(progress.getCompletionPercentage()).isEqualTo(66.67, within(0.01));
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);

        // 4. 完成所有模块
        progress.setCompletedModules(3);
        progress.updateProgress();
        assertThat(progress.getCompletionPercentage()).isEqualTo(100.0);
        assertThat(progress.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(progress.getCompletedAt()).isNotNull();
    }
}
