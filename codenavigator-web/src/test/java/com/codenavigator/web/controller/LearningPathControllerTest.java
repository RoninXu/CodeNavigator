package com.codenavigator.web.controller;

import com.codenavigator.ai.service.LearningPathGenerator;
import com.codenavigator.ai.service.ProgressTracker;
import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.common.enums.DifficultyLevel;
import com.codenavigator.common.enums.UserLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LearningPathController单元测试
 * 测试学习路径控制器的各种功能
 */
@WebMvcTest(LearningPathController.class)
class LearningPathControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LearningPathGenerator pathGenerator;

    @MockBean
    private ProgressTracker progressTracker;

    private LearningPath testPath;
    private ProgressTracker.ProgressSummary progressSummary;
    private List<ProgressTracker.ModuleProgress> moduleProgressList;

    @BeforeEach
    void setUp() {
        // 准备测试学习路径
        testPath = new LearningPath();
        testPath.setId("spring-boot-path");
        testPath.setTitle("Spring Boot 学习路径");
        testPath.setDescription("从入门到精通Spring Boot");
        testPath.setFramework("Spring Boot");
        testPath.setDifficulty(DifficultyLevel.INTERMEDIATE);
        testPath.setTargetLevel(UserLevel.INTERMEDIATE);
        testPath.setEstimatedDuration(40);
        testPath.setIsActive(true);
        testPath.setCreatedAt(LocalDateTime.now());
        testPath.setUpdatedAt(LocalDateTime.now());

        // 准备进度摘要
        progressSummary = new ProgressTracker.ProgressSummary();
        progressSummary.setCompleted(3);
        progressSummary.setTotal(10);
        progressSummary.setPercentage(30.0);

        // 准备模块进度列表
        moduleProgressList = new ArrayList<>();
        ProgressTracker.ModuleProgress module1 = new ProgressTracker.ModuleProgress();
        module1.setModuleId("module-1");
        module1.setModuleName("Spring Boot基础");
        module1.setCompleted(true);
        moduleProgressList.add(module1);

        ProgressTracker.ModuleProgress module2 = new ProgressTracker.ModuleProgress();
        module2.setModuleId("module-2");
        module2.setModuleName("Spring Boot高级");
        module2.setCompleted(false);
        moduleProgressList.add(module2);
    }

    @Test
    void testListPaths_ReturnsCorrectView() throws Exception {
        // When & Then
        mockMvc.perform(get("/learning-paths"))
                .andExpect(status().isOk())
                .andExpect(view().name("learning-path/list"))
                .andExpect(model().attributeExists("pageTitle"))
                .andExpect(model().attribute("pageTitle", "学习路径"));
    }

    @Test
    void testViewPath_ReturnsCorrectView() throws Exception {
        // Given
        String pathId = "spring-boot-path";
        when(pathGenerator.generatePath(anyString(), any(UserLevel.class), any(Map.class)))
                .thenReturn(testPath);

        // When & Then
        mockMvc.perform(get("/learning-paths/{pathId}", pathId))
                .andExpect(status().isOk())
                .andExpect(view().name("learning-path/detail"))
                .andExpect(model().attributeExists("learningPath"))
                .andExpect(model().attributeExists("pageTitle"))
                .andExpect(model().attribute("pageTitle", testPath.getTitle()));

        verify(pathGenerator, times(1)).generatePath(anyString(), any(UserLevel.class), any(Map.class));
    }

    @Test
    void testStartLearningPath_Success() throws Exception {
        // Given
        String pathId = "spring-boot-path";

        // When & Then
        mockMvc.perform(post("/learning-paths/{pathId}/start", pathId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("学习路径已开始")));
    }

    @Test
    void testGeneratePathForm_ReturnsCorrectView() throws Exception {
        // When & Then
        mockMvc.perform(get("/learning-paths/generate"))
                .andExpect(status().isOk())
                .andExpect(view().name("learning-path/generate"))
                .andExpect(model().attributeExists("pageTitle"))
                .andExpect(model().attribute("pageTitle", "生成学习路径"));
    }

    @Test
    void testGeneratePath_Success() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("technology", "Spring Boot");
        request.put("level", "INTERMEDIATE");
        request.put("goals", List.of("web开发", "微服务"));

        when(pathGenerator.generatePath(anyString(), any(UserLevel.class), any(Map.class)))
                .thenReturn(testPath);

        // When & Then
        mockMvc.perform(post("/learning-paths/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pathId").value("spring-boot-path"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path.title").value("Spring Boot 学习路径"));

        verify(pathGenerator, times(1)).generatePath(
                eq("Spring Boot"),
                eq(UserLevel.INTERMEDIATE),
                any(Map.class)
        );
    }

    @Test
    void testGeneratePath_BeginnerLevel() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("technology", "Java");
        request.put("level", "BEGINNER");

        LearningPath beginnerPath = new LearningPath();
        beginnerPath.setId("java-beginner-path");
        beginnerPath.setTitle("Java 入门");
        beginnerPath.setTargetLevel(UserLevel.BEGINNER);

        when(pathGenerator.generatePath(anyString(), any(UserLevel.class), any(Map.class)))
                .thenReturn(beginnerPath);

        // When & Then
        mockMvc.perform(post("/learning-paths/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pathId").value("java-beginner-path"))
                .andExpect(jsonPath("$.path.title").value("Java 入门"));

        verify(pathGenerator, times(1)).generatePath(
                eq("Java"),
                eq(UserLevel.BEGINNER),
                any(Map.class)
        );
    }

    @Test
    void testGeneratePath_AdvancedLevel() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("technology", "Kubernetes");
        request.put("level", "ADVANCED");

        LearningPath advancedPath = new LearningPath();
        advancedPath.setId("k8s-advanced-path");
        advancedPath.setTitle("Kubernetes 高级");
        advancedPath.setTargetLevel(UserLevel.ADVANCED);

        when(pathGenerator.generatePath(anyString(), any(UserLevel.class), any(Map.class)))
                .thenReturn(advancedPath);

        // When & Then
        mockMvc.perform(post("/learning-paths/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pathId").value("k8s-advanced-path"));

        verify(pathGenerator, times(1)).generatePath(
                eq("Kubernetes"),
                eq(UserLevel.ADVANCED),
                any(Map.class)
        );
    }

    @Test
    void testGeneratePath_ThrowsException_ReturnsError() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("technology", "Invalid");
        request.put("level", "INTERMEDIATE");

        when(pathGenerator.generatePath(anyString(), any(UserLevel.class), any(Map.class)))
                .thenThrow(new RuntimeException("Path generation failed"));

        // When & Then
        mockMvc.perform(post("/learning-paths/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("生成学习路径失败")));

        verify(pathGenerator, times(1)).generatePath(anyString(), any(UserLevel.class), any(Map.class));
    }

    @Test
    void testGeneratePath_InvalidLevel_ReturnsError() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("technology", "Spring Boot");
        request.put("level", "INVALID_LEVEL");

        // When & Then
        mockMvc.perform(post("/learning-paths/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(pathGenerator, never()).generatePath(anyString(), any(UserLevel.class), any(Map.class));
    }

    @Test
    void testGetProgress_Success() throws Exception {
        // Given
        String pathId = "spring-boot-path";
        String userId = "user-123";

        when(progressTracker.getProgressSummary(anyString(), anyString()))
                .thenReturn(progressSummary);
        when(progressTracker.getModuleProgressList(anyString(), anyString()))
                .thenReturn(moduleProgressList);

        // When & Then
        mockMvc.perform(get("/learning-paths/{pathId}/progress", pathId)
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.summary.completed").value(3))
                .andExpect(jsonPath("$.summary.total").value(10))
                .andExpect(jsonPath("$.summary.percentage").value(30.0))
                .andExpect(jsonPath("$.modules").isArray())
                .andExpect(jsonPath("$.modules", hasSize(2)))
                .andExpect(jsonPath("$.modules[0].moduleName").value("Spring Boot基础"))
                .andExpect(jsonPath("$.modules[0].completed").value(true))
                .andExpect(jsonPath("$.modules[1].moduleName").value("Spring Boot高级"))
                .andExpect(jsonPath("$.modules[1].completed").value(false));

        verify(progressTracker, times(1)).getProgressSummary(userId, pathId);
        verify(progressTracker, times(1)).getModuleProgressList(userId, pathId);
    }

    @Test
    void testGetProgress_WithDefaultUserId() throws Exception {
        // Given
        String pathId = "spring-boot-path";

        when(progressTracker.getProgressSummary(anyString(), anyString()))
                .thenReturn(progressSummary);
        when(progressTracker.getModuleProgressList(anyString(), anyString()))
                .thenReturn(moduleProgressList);

        // When & Then - 不提供userId参数，应该使用默认值
        mockMvc.perform(get("/learning-paths/{pathId}/progress", pathId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.summary").exists());

        verify(progressTracker, times(1)).getProgressSummary("default-user", pathId);
        verify(progressTracker, times(1)).getModuleProgressList("default-user", pathId);
    }

    @Test
    void testGetProgress_ThrowsException_ReturnsError() throws Exception {
        // Given
        String pathId = "spring-boot-path";
        String userId = "user-123";

        when(progressTracker.getProgressSummary(anyString(), anyString()))
                .thenThrow(new RuntimeException("Progress tracking failed"));

        // When & Then
        mockMvc.perform(get("/learning-paths/{pathId}/progress", pathId)
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("获取进度信息失败")));

        verify(progressTracker, times(1)).getProgressSummary(userId, pathId);
    }

    @Test
    void testGetProgress_ZeroProgress() throws Exception {
        // Given
        String pathId = "spring-boot-path";
        String userId = "new-user";

        ProgressTracker.ProgressSummary zeroProgress = new ProgressTracker.ProgressSummary();
        zeroProgress.setCompleted(0);
        zeroProgress.setTotal(10);
        zeroProgress.setPercentage(0.0);

        when(progressTracker.getProgressSummary(anyString(), anyString()))
                .thenReturn(zeroProgress);
        when(progressTracker.getModuleProgressList(anyString(), anyString()))
                .thenReturn(new ArrayList<>());

        // When & Then
        mockMvc.perform(get("/learning-paths/{pathId}/progress", pathId)
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.summary.completed").value(0))
                .andExpect(jsonPath("$.summary.percentage").value(0.0))
                .andExpect(jsonPath("$.modules").isEmpty());

        verify(progressTracker, times(1)).getProgressSummary(userId, pathId);
    }

    @Test
    void testGetProgress_CompleteProgress() throws Exception {
        // Given
        String pathId = "spring-boot-path";
        String userId = "advanced-user";

        ProgressTracker.ProgressSummary completeProgress = new ProgressTracker.ProgressSummary();
        completeProgress.setCompleted(10);
        completeProgress.setTotal(10);
        completeProgress.setPercentage(100.0);

        when(progressTracker.getProgressSummary(anyString(), anyString()))
                .thenReturn(completeProgress);
        when(progressTracker.getModuleProgressList(anyString(), anyString()))
                .thenReturn(moduleProgressList);

        // When & Then
        mockMvc.perform(get("/learning-paths/{pathId}/progress", pathId)
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.summary.completed").value(10))
                .andExpect(jsonPath("$.summary.total").value(10))
                .andExpect(jsonPath("$.summary.percentage").value(100.0));

        verify(progressTracker, times(1)).getProgressSummary(userId, pathId);
    }
}
