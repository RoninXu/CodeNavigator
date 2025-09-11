package com.codenavigator.web.controller;

import com.codenavigator.ai.service.LearningPathGenerator;
import com.codenavigator.ai.service.ProgressTracker;
import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.common.enums.UserLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/learning-paths")
public class LearningPathController {
    
    private final LearningPathGenerator pathGenerator;
    private final ProgressTracker progressTracker;
    
    @GetMapping
    public String listPaths(Model model) {
        model.addAttribute("pageTitle", "学习路径");
        return "learning-path/list";
    }
    
    @GetMapping("/{pathId}")
    public String viewPath(@PathVariable String pathId, Model model) {
        log.info("Viewing learning path: {}", pathId);
        
        // 这里应该从数据库获取学习路径
        // LearningPath path = learningPathRepository.findById(pathId);
        
        // 为演示目的，生成一个示例路径
        LearningPath path = pathGenerator.generatePath("Spring", UserLevel.INTERMEDIATE, new HashMap<>());
        
        model.addAttribute("learningPath", path);
        model.addAttribute("pageTitle", path.getTitle());
        
        return "learning-path/detail";
    }
    
    @PostMapping("/{pathId}/start")
    @ResponseBody
    public Map<String, Object> startLearningPath(@PathVariable String pathId) {
        log.info("Starting learning path: {}", pathId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 这里应该创建用户进度记录
            response.put("success", true);
            response.put("message", "学习路径已开始！");
            
        } catch (Exception e) {
            log.error("Error starting learning path", e);
            response.put("success", false);
            response.put("message", "启动学习路径失败，请稍后再试。");
        }
        
        return response;
    }
    
    @GetMapping("/generate")
    public String generatePathForm(Model model) {
        model.addAttribute("pageTitle", "生成学习路径");
        return "learning-path/generate";
    }
    
    @PostMapping("/generate")
    @ResponseBody
    public Map<String, Object> generatePath(@RequestBody Map<String, Object> request) {
        log.info("Generating learning path with request: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        try {
            String technology = (String) request.get("technology");
            String levelStr = (String) request.get("level");
            UserLevel userLevel = UserLevel.valueOf(levelStr.toUpperCase());
            
            LearningPath path = pathGenerator.generatePath(technology, userLevel, request);
            
            response.put("success", true);
            response.put("pathId", path.getId());
            response.put("path", path);
            
        } catch (Exception e) {
            log.error("Error generating learning path", e);
            response.put("success", false);
            response.put("message", "生成学习路径失败：" + e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/{pathId}/progress")
    @ResponseBody
    public Map<String, Object> getProgress(@PathVariable String pathId, 
                                         @RequestParam(defaultValue = "default-user") String userId) {
        log.info("Getting progress for path: {} and user: {}", pathId, userId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            ProgressTracker.ProgressSummary summary = progressTracker.getProgressSummary(userId, pathId);
            List<ProgressTracker.ModuleProgress> moduleProgress = progressTracker.getModuleProgressList(userId, pathId);
            
            response.put("success", true);
            response.put("summary", summary);
            response.put("modules", moduleProgress);
            
        } catch (Exception e) {
            log.error("Error getting progress", e);
            response.put("success", false);
            response.put("message", "获取进度信息失败");
        }
        
        return response;
    }
}