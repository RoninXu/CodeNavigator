package com.codenavigator.ai.service;

import com.codenavigator.core.entity.LearningModule;
import com.codenavigator.common.enums.DifficultyLevel;
import com.codenavigator.common.enums.ModuleType;
import com.codenavigator.common.enums.UserLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDifficultyAssessor {
    
    // 模块类型基础难度权重
    private static final Map<ModuleType, Double> MODULE_TYPE_WEIGHTS;
    static {
        Map<ModuleType, Double> weights = new HashMap<>();
        weights.put(ModuleType.THEORY, 1.0);
        weights.put(ModuleType.PRACTICE, 1.5);
        weights.put(ModuleType.PROJECT, 2.0);
        weights.put(ModuleType.TUTORIAL, 1.2);
        weights.put(ModuleType.QUIZ, 0.8);
        MODULE_TYPE_WEIGHTS = Collections.unmodifiableMap(weights);
    }
    
    // 技术复杂度评分
    private static final Map<String, Double> TECHNOLOGY_COMPLEXITY;
    static {
        Map<String, Double> complexity = new HashMap<>();
        complexity.put("Spring", 1.5);
        complexity.put("Kafka", 2.0);
        complexity.put("Netty", 2.5);
        complexity.put("MySQL", 1.3);
        complexity.put("Redis", 1.4);
        complexity.put("Docker", 1.6);
        complexity.put("Kubernetes", 2.8);
        complexity.put("Java", 1.0);
        TECHNOLOGY_COMPLEXITY = Collections.unmodifiableMap(complexity);
    }
    
    // 概念复杂度关键词
    private static final Map<String, Double> CONCEPT_COMPLEXITY;
    static {
        Map<String, Double> concepts = new HashMap<>();
        concepts.put("基础", 0.5);
        concepts.put("入门", 0.5);
        concepts.put("简介", 0.5);
        concepts.put("进阶", 1.5);
        concepts.put("高级", 2.0);
        concepts.put("深入", 2.2);
        concepts.put("架构", 2.0);
        concepts.put("设计模式", 2.3);
        concepts.put("源码", 2.5);
        concepts.put("性能", 2.0);
        concepts.put("调优", 2.3);
        concepts.put("监控", 1.8);
        concepts.put("分布式", 2.8);
        concepts.put("微服务", 2.5);
        concepts.put("集群", 2.2);
        CONCEPT_COMPLEXITY = Collections.unmodifiableMap(concepts);
    }
    
    public DifficultyLevel assessModuleDifficulty(LearningModule module, String technology) {
        log.debug("Assessing difficulty for module: {}", module.getTitle());
        
        double baseScore = calculateBaseScore(module, technology);
        double adjustedScore = adjustScoreByContent(baseScore, module);
        
        DifficultyLevel difficulty = scoreToDifficulty(adjustedScore);
        
        log.debug("Module '{}' assessed as {} (score: {})", 
                 module.getTitle(), difficulty, adjustedScore);
        
        return difficulty;
    }
    
    public DifficultyLevel assessUserCapabilityGap(UserLevel userLevel, LearningModule module) {
        log.debug("Assessing capability gap for user level: {} and module: {}", 
                 userLevel, module.getTitle());
        
        double moduleComplexity = calculateModuleComplexity(module);
        double userCapability = getUserCapabilityScore(userLevel);
        
        double gap = moduleComplexity - userCapability;
        
        if (gap <= 0.5) return DifficultyLevel.BEGINNER;
        if (gap <= 1.0) return DifficultyLevel.INTERMEDIATE;
        if (gap <= 1.5) return DifficultyLevel.ADVANCED;
        return DifficultyLevel.EXPERT;
    }
    
    public int estimateCompletionTime(LearningModule module, UserLevel userLevel) {
        log.debug("Estimating completion time for module: {} at user level: {}", 
                 module.getTitle(), userLevel);
        
        int baseTime = module.getEstimatedHours();
        double difficulty = calculateModuleComplexity(module);
        double userMultiplier = getUserTimeMultiplier(userLevel);
        
        int estimatedTime = (int) (baseTime * difficulty * userMultiplier);
        
        // 确保时间在合理范围内
        estimatedTime = Math.max(1, Math.min(estimatedTime, 100));
        
        log.debug("Estimated completion time: {} hours", estimatedTime);
        return estimatedTime;
    }
    
    public Map<String, Double> analyzeModuleComplexity(LearningModule module) {
        Map<String, Double> analysis = new HashMap<>();
        
        // 模块类型复杂度
        analysis.put("moduleTypeComplexity", 
                    MODULE_TYPE_WEIGHTS.getOrDefault(module.getModuleType(), 1.0));
        
        // 内容复杂度
        double contentComplexity = analyzeContentComplexity(module.getDescription());
        analysis.put("contentComplexity", contentComplexity);
        
        // 时长复杂度
        double timeComplexity = normalizeTimeComplexity(module.getEstimatedHours());
        analysis.put("timeComplexity", timeComplexity);
        
        // 前置条件复杂度
        double prerequisiteComplexity = calculatePrerequisiteComplexity(module);
        analysis.put("prerequisiteComplexity", prerequisiteComplexity);
        
        // 综合复杂度
        double overallComplexity = (
            analysis.get("moduleTypeComplexity") * 0.3 +
            analysis.get("contentComplexity") * 0.4 +
            analysis.get("timeComplexity") * 0.2 +
            analysis.get("prerequisiteComplexity") * 0.1
        );
        analysis.put("overallComplexity", overallComplexity);
        
        return analysis;
    }
    
    private double calculateBaseScore(LearningModule module, String technology) {
        double score = 1.0;
        
        // 基于模块类型的基础分数
        score *= MODULE_TYPE_WEIGHTS.getOrDefault(module.getModuleType(), 1.0);
        
        // 基于技术的复杂度
        score *= TECHNOLOGY_COMPLEXITY.getOrDefault(technology, 1.0);
        
        // 基于预估时长
        if (module.getEstimatedHours() != null) {
            score *= Math.log(module.getEstimatedHours() + 1) / Math.log(10) + 0.5;
        }
        
        return score;
    }
    
    private double adjustScoreByContent(double baseScore, LearningModule module) {
        double multiplier = 1.0;
        
        String content = (module.getTitle() + " " + 
                         (module.getDescription() != null ? module.getDescription() : "")).toLowerCase();
        
        // 检查概念复杂度关键词
        for (Map.Entry<String, Double> entry : CONCEPT_COMPLEXITY.entrySet()) {
            if (content.contains(entry.getKey())) {
                multiplier *= entry.getValue();
                break; // 只应用第一个匹配的复杂度调整
            }
        }
        
        // 基于前置条件数量调整
        if (module.getPrerequisites() != null) {
            multiplier += module.getPrerequisites().size() * 0.1;
        }
        
        return baseScore * multiplier;
    }
    
    private double calculateModuleComplexity(LearningModule module) {
        double complexity = 1.0;
        
        // 模块类型基础复杂度
        complexity *= MODULE_TYPE_WEIGHTS.getOrDefault(module.getModuleType(), 1.0);
        
        // 内容复杂度分析
        if (module.getDescription() != null) {
            complexity *= analyzeContentComplexity(module.getDescription());
        }
        
        // 时长影响
        if (module.getEstimatedHours() != null) {
            complexity *= Math.log(module.getEstimatedHours() + 1) / 5.0 + 0.5;
        }
        
        return complexity;
    }
    
    private double analyzeContentComplexity(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 1.0;
        }
        
        String lowerContent = content.toLowerCase();
        double complexity = 1.0;
        
        // 检查技术复杂度关键词
        for (Map.Entry<String, Double> entry : CONCEPT_COMPLEXITY.entrySet()) {
            if (lowerContent.contains(entry.getKey())) {
                complexity = Math.max(complexity, entry.getValue());
            }
        }
        
        // 基于内容长度的复杂度调整
        int contentLength = content.length();
        if (contentLength > 500) complexity += 0.3;
        if (contentLength > 1000) complexity += 0.2;
        
        return complexity;
    }
    
    private double normalizeTimeComplexity(Integer estimatedHours) {
        if (estimatedHours == null) return 1.0;
        
        // 将时长转换为复杂度分数 (1-3小时=简单, 4-8小时=中等, 9+小时=困难)
        if (estimatedHours <= 3) return 0.8;
        if (estimatedHours <= 8) return 1.0;
        if (estimatedHours <= 16) return 1.3;
        return 1.6;
    }
    
    private double calculatePrerequisiteComplexity(LearningModule module) {
        if (module.getPrerequisites() == null || module.getPrerequisites().isEmpty()) {
            return 0.8; // 无前置条件相对简单
        }
        
        int prerequisiteCount = module.getPrerequisites().size();
        
        // 前置条件越多，复杂度越高
        return 1.0 + (prerequisiteCount - 1) * 0.15;
    }
    
    private double getUserCapabilityScore(UserLevel userLevel) {
        switch (userLevel) {
            case BEGINNER: return 1.0;
            case INTERMEDIATE: return 2.0;
            case ADVANCED: return 3.0;
            default: return 2.0;
        }
    }
    
    private double getUserTimeMultiplier(UserLevel userLevel) {
        switch (userLevel) {
            case BEGINNER: return 1.5;     // 初学者需要更多时间
            case INTERMEDIATE: return 1.0; // 标准时间
            case ADVANCED: return 0.7;     // 高级用户更快
            default: return 1.0;
        }
    }
    
    private DifficultyLevel scoreToDifficulty(double score) {
        if (score <= 1.0) return DifficultyLevel.BEGINNER;
        if (score <= 2.0) return DifficultyLevel.INTERMEDIATE;
        if (score <= 3.0) return DifficultyLevel.ADVANCED;
        return DifficultyLevel.EXPERT;
    }
}