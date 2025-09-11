package com.codenavigator.web.controller;

import com.codenavigator.ai.service.ProgressTracker;
import com.codenavigator.ai.service.TaskDifficultyAssessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/modules")
public class ModuleController {
    
    private final ProgressTracker progressTracker;
    private final TaskDifficultyAssessor difficultyAssessor;
    
    @GetMapping("/{moduleId}")
    public String viewModule(@PathVariable String moduleId, Model model) {
        log.info("Viewing module: {}", moduleId);
        
        // 这里应该从数据库获取模块信息
        // LearningModule module = moduleRepository.findById(moduleId);
        
        model.addAttribute("moduleId", moduleId);
        model.addAttribute("pageTitle", "模块学习");
        
        return "module/detail";
    }
    
    @GetMapping("/{moduleId}/learn")
    public String learnModule(@PathVariable String moduleId, Model model) {
        log.info("Starting to learn module: {}", moduleId);
        
        model.addAttribute("moduleId", moduleId);
        model.addAttribute("pageTitle", "正在学习");
        
        return "module/learn";
    }
    
    @PostMapping("/{moduleId}/complete")
    @ResponseBody
    public Map<String, Object> completeModule(@PathVariable String moduleId,
                                            @RequestParam(defaultValue = "default-user") String userId,
                                            @RequestBody Map<String, Object> submission) {
        log.info("Completing module {} for user {}", moduleId, userId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 处理模块完成逻辑
            // UserProgress progress = progressTracker.completeModule(userId, moduleId);
            
            response.put("success", true);
            response.put("message", "模块完成！");
            response.put("nextModule", "next-module-id"); // 实际应该从进度中获取
            
        } catch (Exception e) {
            log.error("Error completing module", e);
            response.put("success", false);
            response.put("message", "完成模块失败：" + e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/{moduleId}/submit-task")
    @ResponseBody
    public Map<String, Object> submitTask(@PathVariable String moduleId,
                                        @RequestParam String taskType,
                                        @RequestBody Map<String, Object> taskData) {
        log.info("Submitting task for module: {}, type: {}", moduleId, taskType);
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 这里应该处理不同类型的任务提交
            switch (taskType) {
                case "code_review":
                    response = processCodeReview(moduleId, taskData);
                    break;
                case "quiz":
                    response = processQuiz(moduleId, taskData);
                    break;
                case "project":
                    response = processProject(moduleId, taskData);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的任务类型: " + taskType);
            }
            
        } catch (Exception e) {
            log.error("Error submitting task", e);
            response.put("success", false);
            response.put("message", "任务提交失败：" + e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/{moduleId}/hints")
    @ResponseBody
    public Map<String, Object> getHints(@PathVariable String moduleId) {
        log.info("Getting hints for module: {}", moduleId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 获取模块提示信息
            response.put("success", true);
            response.put("hints", Map.of(
                "general", "仔细阅读文档，理解核心概念",
                "practice", "多动手实践，编写示例代码",
                "troubleshooting", "遇到问题时查看官方文档和社区讨论"
            ));
            
        } catch (Exception e) {
            log.error("Error getting hints", e);
            response.put("success", false);
            response.put("message", "获取提示失败");
        }
        
        return response;
    }
    
    private Map<String, Object> processCodeReview(String moduleId, Map<String, Object> taskData) {
        Map<String, Object> response = new HashMap<>();
        String code = (String) taskData.get("code");
        
        if (code == null || code.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "请提交代码内容");
            return response;
        }
        
        // 这里应该调用AI代码分析服务
        response.put("success", true);
        response.put("feedback", Map.of(
            "score", 85,
            "comments", "代码结构良好，建议添加更多注释",
            "suggestions", java.util.List.of(
                "考虑使用更有意义的变量名",
                "可以添加异常处理机制",
                "建议添加单元测试"
            )
        ));
        
        return response;
    }
    
    private Map<String, Object> processQuiz(String moduleId, Map<String, Object> taskData) {
        Map<String, Object> response = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) taskData.get("answers");
        
        if (answers == null || answers.isEmpty()) {
            response.put("success", false);
            response.put("message", "请完成所有题目");
            return response;
        }
        
        // 这里应该检查答案的正确性
        int correct = 0;
        int total = answers.size();
        
        // 简化的评分逻辑
        for (String answer : answers.values()) {
            if (answer != null && !answer.trim().isEmpty()) {
                correct++; // 实际应该检查正确答案
            }
        }
        
        double score = (double) correct / total * 100;
        
        response.put("success", true);
        response.put("score", score);
        response.put("correct", correct);
        response.put("total", total);
        response.put("passed", score >= 60);
        
        return response;
    }
    
    private Map<String, Object> processProject(String moduleId, Map<String, Object> taskData) {
        Map<String, Object> response = new HashMap<>();
        String projectUrl = (String) taskData.get("projectUrl");
        String description = (String) taskData.get("description");
        
        if (projectUrl == null || projectUrl.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "请提供项目链接");
            return response;
        }
        
        // 这里应该验证项目链接并进行自动评估
        response.put("success", true);
        response.put("message", "项目提交成功，正在评估中...");
        response.put("estimatedReviewTime", "24小时");
        
        return response;
    }
}