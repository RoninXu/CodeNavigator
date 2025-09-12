package com.codenavigator.ai.service;

import com.codenavigator.ai.dto.CodeAnalysisResult;
import com.codenavigator.ai.dto.CodeAnalysisResult.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackTemplateService {
    
    // 不同学习水平的模板
    private static final Map<String, FeedbackTemplate> LEVEL_TEMPLATES = new HashMap<>();
    
    // 不同问题类型的解释模板
    private static final Map<String, String> ISSUE_EXPLANATIONS = new HashMap<>();
    
    // 改进建议的详细模板
    private static final Map<String, String> SUGGESTION_TEMPLATES = new HashMap<>();
    
    static {
        initializeLevelTemplates();
        initializeIssueExplanations();
        initializeSuggestionTemplates();
    }
    
    public FeedbackResponse generatePersonalizedFeedback(CodeAnalysisResult analysisResult, String userLevel) {
        log.info("生成个性化反馈，用户水平: {}, 分析ID: {}", userLevel, analysisResult.getAnalysisId());
        
        FeedbackTemplate template = LEVEL_TEMPLATES.getOrDefault(userLevel, LEVEL_TEMPLATES.get("INTERMEDIATE"));
        
        StringBuilder feedback = new StringBuilder();
        
        // 生成总体评价
        feedback.append(generateOverallAssessment(analysisResult, template));
        feedback.append("\n\n");
        
        // 生成问题分析
        if (!analysisResult.getIssues().isEmpty()) {
            feedback.append(generateIssueAnalysis(analysisResult.getIssues(), template));
            feedback.append("\n\n");
        }
        
        // 生成改进建议
        if (!analysisResult.getSuggestions().isEmpty()) {
            feedback.append(generateImprovementSuggestions(analysisResult.getSuggestions(), template));
            feedback.append("\n\n");
        }
        
        // 生成学习建议
        feedback.append(generateLearningRecommendations(analysisResult, template));
        
        // 生成实践练习
        List<String> practiceExercises = generatePracticeExercises(analysisResult, userLevel);
        
        return FeedbackResponse.builder()
            .feedbackText(feedback.toString())
            .practiceExercises(practiceExercises)
            .learningResources(generateLearningResources(analysisResult, userLevel))
            .nextSteps(generateNextSteps(analysisResult, userLevel))
            .estimatedLearningTime(estimateLearningTime(analysisResult, userLevel))
            .build();
    }
    
    private String generateOverallAssessment(CodeAnalysisResult result, FeedbackTemplate template) {
        StringBuilder assessment = new StringBuilder();
        
        assessment.append(String.format(template.getOverallTemplate(), 
            result.getOverallScore(), 
            result.getQualityLevel().getDescription()));
        
        // 根据评分给出不同的鼓励或建议
        if (result.getOverallScore() >= 90) {
            assessment.append(template.getExcellentFeedback());
        } else if (result.getOverallScore() >= 75) {
            assessment.append(template.getGoodFeedback());
        } else if (result.getOverallScore() >= 60) {
            assessment.append(template.getAverageFeedback());
        } else {
            assessment.append(template.getNeedsImprovementFeedback());
        }
        
        return assessment.toString();
    }
    
    private String generateIssueAnalysis(List<CodeIssue> issues, FeedbackTemplate template) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("### 问题分析\n\n");
        
        // 按严重程度分组
        Map<IssueSeverity, List<CodeIssue>> issuesByType = issues.stream()
            .collect(Collectors.groupingBy(CodeIssue::getSeverity));
        
        for (IssueSeverity severity : Arrays.asList(IssueSeverity.CRITICAL, IssueSeverity.HIGH, IssueSeverity.MEDIUM, IssueSeverity.LOW)) {
            List<CodeIssue> severityIssues = issuesByType.get(severity);
            if (severityIssues != null && !severityIssues.isEmpty()) {
                analysis.append(String.format("**%s级问题 (%d个):**\n", 
                    getSeverityDisplayName(severity), severityIssues.size()));
                
                for (CodeIssue issue : severityIssues.subList(0, Math.min(5, severityIssues.size()))) {
                    analysis.append(String.format("- %s (第%d行): %s\n", 
                        issue.getTitle(), 
                        issue.getLineNumber() != null ? issue.getLineNumber() : 0,
                        getIssueExplanation(issue, template.getDetailLevel())));
                }
                
                if (severityIssues.size() > 5) {
                    analysis.append(String.format("  ... 还有%d个类似问题\n", severityIssues.size() - 5));
                }
                analysis.append("\n");
            }
        }
        
        return analysis.toString();
    }
    
    private String generateImprovementSuggestions(List<CodeSuggestion> suggestions, FeedbackTemplate template) {
        StringBuilder suggestionText = new StringBuilder();
        suggestionText.append("### 改进建议\n\n");
        
        // 按优先级排序并分组
        Map<Priority, List<CodeSuggestion>> suggestionsByPriority = suggestions.stream()
            .collect(Collectors.groupingBy(CodeSuggestion::getPriority));
        
        for (Priority priority : Arrays.asList(Priority.HIGH, Priority.MEDIUM, Priority.LOW)) {
            List<CodeSuggestion> prioritySuggestions = suggestionsByPriority.get(priority);
            if (prioritySuggestions != null && !prioritySuggestions.isEmpty()) {
                suggestionText.append(String.format("**%s优先级建议:**\n", 
                    getPriorityDisplayName(priority)));
                
                for (CodeSuggestion suggestion : prioritySuggestions.subList(0, Math.min(3, prioritySuggestions.size()))) {
                    suggestionText.append(String.format("- **%s**: %s\n", 
                        suggestion.getTitle(), 
                        getSuggestionDetails(suggestion, template.getDetailLevel())));
                    
                    if (suggestion.getCodeExample() != null && !suggestion.getCodeExample().isEmpty()) {
                        suggestionText.append("  ```java\n");
                        suggestionText.append("  ").append(suggestion.getCodeExample().replace("\n", "\n  "));
                        suggestionText.append("\n  ```\n");
                    }
                }
                suggestionText.append("\n");
            }
        }
        
        return suggestionText.toString();
    }
    
    private String generateLearningRecommendations(CodeAnalysisResult result, FeedbackTemplate template) {
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("### 学习建议\n\n");
        
        QualityMetrics metrics = result.getMetrics();
        List<String> focusAreas = new ArrayList<>();
        
        if (metrics.getCodeStyle() != null && metrics.getCodeStyle() < 70) {
            focusAreas.add("代码规范和风格");
        }
        if (metrics.getReadability() != null && metrics.getReadability() < 70) {
            focusAreas.add("代码可读性");
        }
        if (metrics.getMaintainability() != null && metrics.getMaintainability() < 70) {
            focusAreas.add("代码可维护性");
        }
        if (metrics.getPerformance() != null && metrics.getPerformance() < 70) {
            focusAreas.add("性能优化");
        }
        if (metrics.getSecurity() != null && metrics.getSecurity() < 70) {
            focusAreas.add("安全编程");
        }
        if (metrics.getBestPractices() != null && metrics.getBestPractices() < 70) {
            focusAreas.add("最佳实践");
        }
        
        if (focusAreas.isEmpty()) {
            recommendations.append("您的代码质量整体良好！建议继续保持并深入学习高级编程技巧。");
        } else {
            recommendations.append("建议重点学习以下方面:\n");
            for (String area : focusAreas) {
                recommendations.append(String.format("- %s: %s\n", area, 
                    getLearningRecommendation(area, template.getDetailLevel())));
            }
        }
        
        return recommendations.toString();
    }
    
    private List<String> generatePracticeExercises(CodeAnalysisResult result, String userLevel) {
        List<String> exercises = new ArrayList<>();
        
        // 根据问题类型生成针对性练习
        Set<String> categories = result.getIssues().stream()
            .map(CodeIssue::getCategory)
            .collect(Collectors.toSet());
        
        if (categories.contains("STYLE")) {
            exercises.add("练习：重构一个违反命名规范的类，使其符合Java命名约定");
        }
        if (categories.contains("PERFORMANCE")) {
            exercises.add("练习：优化一个包含性能问题的方法，比较优化前后的执行时间");
        }
        if (categories.contains("SECURITY")) {
            exercises.add("练习：识别并修复SQL注入漏洞，使用预编译语句");
        }
        if (categories.contains("MAINTAINABILITY")) {
            exercises.add("练习：将一个复杂方法拆分为多个单一职责的小方法");
        }
        
        // 根据用户水平添加适当的练习
        if ("BEGINNER".equals(userLevel)) {
            exercises.add("基础练习：编写一个完整的Java类，包含构造函数、getter/setter和toString方法");
        } else if ("ADVANCED".equals(userLevel)) {
            exercises.add("高级练习：设计并实现一个支持多种排序算法的策略模式");
        }
        
        return exercises;
    }
    
    private List<String> generateLearningResources(CodeAnalysisResult result, String userLevel) {
        List<String> resources = new ArrayList<>();
        
        if (result.getOverallScore() < 70) {
            resources.add("《Effective Java》- Joshua Bloch");
            resources.add("《Clean Code》- Robert C. Martin");
        }
        
        if (result.getMetrics().getSecurity() != null && result.getMetrics().getSecurity() < 70) {
            resources.add("OWASP Java安全编程指南");
            resources.add("《Java安全编程实践》");
        }
        
        if (result.getMetrics().getPerformance() != null && result.getMetrics().getPerformance() < 70) {
            resources.add("《Java性能优化权威指南》");
            resources.add("JProfiler性能分析工具使用教程");
        }
        
        return resources;
    }
    
    private List<String> generateNextSteps(CodeAnalysisResult result, String userLevel) {
        List<String> steps = new ArrayList<>();
        
        // 根据最严重的问题生成下一步行动
        Optional<CodeIssue> criticalIssue = result.getIssues().stream()
            .filter(issue -> issue.getSeverity() == IssueSeverity.CRITICAL)
            .findFirst();
        
        if (criticalIssue.isPresent()) {
            steps.add("立即修复严重级别的问题: " + criticalIssue.get().getTitle());
        }
        
        // 根据建议生成行动步骤
        Optional<CodeSuggestion> highPrioritySuggestion = result.getSuggestions().stream()
            .filter(suggestion -> suggestion.getPriority() == Priority.HIGH)
            .findFirst();
        
        if (highPrioritySuggestion.isPresent()) {
            steps.add("实施高优先级改进: " + highPrioritySuggestion.get().getTitle());
        }
        
        steps.add("定期进行代码审查，建立持续改进习惯");
        
        return steps;
    }
    
    private int estimateLearningTime(CodeAnalysisResult result, String userLevel) {
        int baseTime = 30; // 基础学习时间（分钟）
        
        // 根据问题数量调整
        baseTime += result.getIssues().size() * 5;
        
        // 根据用户水平调整
        switch (userLevel) {
            case "BEGINNER":
                return baseTime * 2;
            case "ADVANCED":
                return (int) (baseTime * 0.7);
            default:
                return baseTime;
        }
    }
    
    // 辅助方法
    private String getSeverityDisplayName(IssueSeverity severity) {
        switch (severity) {
            case CRITICAL: return "严重";
            case HIGH: return "高";
            case MEDIUM: return "中等";
            case LOW: return "低";
            default: return "信息";
        }
    }
    
    private String getPriorityDisplayName(Priority priority) {
        switch (priority) {
            case HIGH: return "高";
            case MEDIUM: return "中等";
            case LOW: return "低";
            default: return "低";
        }
    }
    
    private String getIssueExplanation(CodeIssue issue, String detailLevel) {
        String baseExplanation = ISSUE_EXPLANATIONS.getOrDefault(issue.getType(), issue.getDescription());
        
        if ("DETAILED".equals(detailLevel)) {
            return baseExplanation + " 建议仔细检查此处的实现逻辑。";
        } else if ("SIMPLE".equals(detailLevel)) {
            return issue.getDescription();
        }
        
        return baseExplanation;
    }
    
    private String getSuggestionDetails(CodeSuggestion suggestion, String detailLevel) {
        String baseText = suggestion.getDescription();
        
        if ("DETAILED".equals(detailLevel) && suggestion.getEstimatedImpact() != null) {
            return baseText + String.format(" (预期改进效果: %d%%)", suggestion.getEstimatedImpact());
        }
        
        return baseText;
    }
    
    private String getLearningRecommendation(String area, String detailLevel) {
        Map<String, String> recommendations = new HashMap<>();
        recommendations.put("代码规范和风格", "学习并遵循Java编码规范，使用代码格式化工具");
        recommendations.put("代码可读性", "练习编写自文档化代码，合理命名变量和方法");
        recommendations.put("代码可维护性", "学习SOLID原则，练习代码重构技巧");
        recommendations.put("性能优化", "学习Java性能调优，理解JVM内存模型");
        recommendations.put("安全编程", "学习常见安全漏洞及防范措施");
        recommendations.put("最佳实践", "阅读Java最佳实践相关书籍和文档");
        
        return recommendations.getOrDefault(area, "持续学习和实践");
    }
    
    private static void initializeLevelTemplates() {
        // 初学者模板
        LEVEL_TEMPLATES.put("BEGINNER", FeedbackTemplate.builder()
            .overallTemplate("您的代码得分为 %d/100 (%s)。")
            .excellentFeedback("太棒了！您的代码质量非常高，继续保持这种良好的编程习惯！")
            .goodFeedback("做得很好！您的代码质量不错，继续努力提升。")
            .averageFeedback("您的代码还有改进空间，不要气馁，继续学习和练习！")
            .needsImprovementFeedback("不用担心，每个程序员都是从这里开始的。让我们一起来改进您的代码吧！")
            .detailLevel("SIMPLE")
            .build());
        
        // 中级模板
        LEVEL_TEMPLATES.put("INTERMEDIATE", FeedbackTemplate.builder()
            .overallTemplate("代码评分: %d/100 (%s)")
            .excellentFeedback("优秀的代码质量！您已经掌握了良好的编程实践。")
            .goodFeedback("代码质量良好，在某些方面还可以进一步优化。")
            .averageFeedback("代码基本符合要求，但在一些关键方面需要改进。")
            .needsImprovementFeedback("代码存在一些问题，建议重点关注以下改进点。")
            .detailLevel("MODERATE")
            .build());
        
        // 高级模板
        LEVEL_TEMPLATES.put("ADVANCED", FeedbackTemplate.builder()
            .overallTemplate("Quality Score: %d/100 (%s)")
            .excellentFeedback("Excellent code quality with adherence to best practices.")
            .goodFeedback("Good code quality. Consider the advanced optimizations suggested.")
            .averageFeedback("Code meets basic requirements but lacks some advanced practices.")
            .needsImprovementFeedback("Several areas require attention for production-ready code.")
            .detailLevel("DETAILED")
            .build());
    }
    
    private static void initializeIssueExplanations() {
        ISSUE_EXPLANATIONS.put("NAMING_VIOLATION", "变量或方法命名不符合Java规范，建议使用驼峰命名法");
        ISSUE_EXPLANATIONS.put("MAGIC_NUMBER", "代码中包含魔法数字，应该定义为常量");
        ISSUE_EXPLANATIONS.put("LONG_METHOD", "方法过长，建议拆分为更小的方法");
        ISSUE_EXPLANATIONS.put("EMPTY_CATCH", "空的catch块可能隐藏错误，应该至少记录日志");
        ISSUE_EXPLANATIONS.put("SQL_INJECTION", "存在SQL注入风险，应该使用预编译语句");
        ISSUE_EXPLANATIONS.put("RESOURCE_LEAK", "资源可能没有正确关闭，建议使用try-with-resources");
    }
    
    private static void initializeSuggestionTemplates() {
        SUGGESTION_TEMPLATES.put("EXTRACT_METHOD", "将复杂的代码块提取为独立方法，提高可读性和可维护性");
        SUGGESTION_TEMPLATES.put("USE_CONSTANTS", "将硬编码的值定义为常量，提高代码的可维护性");
        SUGGESTION_TEMPLATES.put("ADD_VALIDATION", "添加输入验证，提高代码的健壮性");
        SUGGESTION_TEMPLATES.put("OPTIMIZE_PERFORMANCE", "优化算法或数据结构使用，提高代码性能");
    }
    
    // 内部类定义
    public static class FeedbackTemplate {
        private String overallTemplate;
        private String excellentFeedback;
        private String goodFeedback;
        private String averageFeedback;
        private String needsImprovementFeedback;
        private String detailLevel;
        
        public static FeedbackTemplateBuilder builder() {
            return new FeedbackTemplateBuilder();
        }
        
        // Getters
        public String getOverallTemplate() { return overallTemplate; }
        public String getExcellentFeedback() { return excellentFeedback; }
        public String getGoodFeedback() { return goodFeedback; }
        public String getAverageFeedback() { return averageFeedback; }
        public String getNeedsImprovementFeedback() { return needsImprovementFeedback; }
        public String getDetailLevel() { return detailLevel; }
        
        public static class FeedbackTemplateBuilder {
            private FeedbackTemplate template = new FeedbackTemplate();
            
            public FeedbackTemplateBuilder overallTemplate(String overallTemplate) {
                template.overallTemplate = overallTemplate;
                return this;
            }
            
            public FeedbackTemplateBuilder excellentFeedback(String excellentFeedback) {
                template.excellentFeedback = excellentFeedback;
                return this;
            }
            
            public FeedbackTemplateBuilder goodFeedback(String goodFeedback) {
                template.goodFeedback = goodFeedback;
                return this;
            }
            
            public FeedbackTemplateBuilder averageFeedback(String averageFeedback) {
                template.averageFeedback = averageFeedback;
                return this;
            }
            
            public FeedbackTemplateBuilder needsImprovementFeedback(String needsImprovementFeedback) {
                template.needsImprovementFeedback = needsImprovementFeedback;
                return this;
            }
            
            public FeedbackTemplateBuilder detailLevel(String detailLevel) {
                template.detailLevel = detailLevel;
                return this;
            }
            
            public FeedbackTemplate build() {
                return template;
            }
        }
    }
    
    public static class FeedbackResponse {
        private String feedbackText;
        private List<String> practiceExercises;
        private List<String> learningResources;
        private List<String> nextSteps;
        private int estimatedLearningTime;
        
        public static FeedbackResponseBuilder builder() {
            return new FeedbackResponseBuilder();
        }
        
        // Getters
        public String getFeedbackText() { return feedbackText; }
        public List<String> getPracticeExercises() { return practiceExercises; }
        public List<String> getLearningResources() { return learningResources; }
        public List<String> getNextSteps() { return nextSteps; }
        public int getEstimatedLearningTime() { return estimatedLearningTime; }
        
        public static class FeedbackResponseBuilder {
            private FeedbackResponse response = new FeedbackResponse();
            
            public FeedbackResponseBuilder feedbackText(String feedbackText) {
                response.feedbackText = feedbackText;
                return this;
            }
            
            public FeedbackResponseBuilder practiceExercises(List<String> practiceExercises) {
                response.practiceExercises = practiceExercises;
                return this;
            }
            
            public FeedbackResponseBuilder learningResources(List<String> learningResources) {
                response.learningResources = learningResources;
                return this;
            }
            
            public FeedbackResponseBuilder nextSteps(List<String> nextSteps) {
                response.nextSteps = nextSteps;
                return this;
            }
            
            public FeedbackResponseBuilder estimatedLearningTime(int estimatedLearningTime) {
                response.estimatedLearningTime = estimatedLearningTime;
                return this;
            }
            
            public FeedbackResponse build() {
                return response;
            }
        }
    }
}