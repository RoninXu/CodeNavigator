package com.codenavigator.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeAnalysisResult {
    
    private String analysisId;
    private LocalDateTime analysisTime;
    private String userId;
    private String moduleId;
    
    // 总体评分 (0-100)
    private Integer overallScore;
    private QualityLevel qualityLevel;
    
    // 各维度评分
    private QualityMetrics metrics;
    
    // 问题和建议
    private List<CodeIssue> issues;
    private List<CodeSuggestion> suggestions;
    
    // 分析摘要
    private String summary;
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityMetrics {
        private Integer codeStyle;        // 代码风格评分
        private Integer readability;      // 可读性评分
        private Integer maintainability;  // 可维护性评分
        private Integer performance;      // 性能评分
        private Integer security;         // 安全性评分
        private Integer bestPractices;    // 最佳实践评分
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeIssue {
        private String id;
        private IssueType type;
        private IssueSeverity severity;
        private String title;
        private String description;
        private Integer lineNumber;
        private Integer columnNumber;
        private String codeSnippet;
        private String rule;              // 违反的规则
        private String category;          // 问题分类
        private List<String> fixSuggestions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeSuggestion {
        private String id;
        private SuggestionType type;
        private Priority priority;
        private String title;
        private String description;
        private String originalCode;
        private String improvedCode;
        private String codeExample;       // 代码示例
        private String explanation;
        private String category;          // 建议分类
        private String difficultyLevel;   // 难度级别
        private Integer lineNumber;       // 关联的行号
        private List<String> benefits;
        private List<String> tags;        // 标签列表
        private Integer estimatedImpact; // 1-100，影响程度
    }
    
    public enum QualityLevel {
        EXCELLENT(90, "优秀"),
        GOOD(75, "良好"),
        AVERAGE(60, "一般"),
        POOR(45, "较差"),
        VERY_POOR(0, "很差");
        
        private final int minScore;
        private final String description;
        
        QualityLevel(int minScore, String description) {
            this.minScore = minScore;
            this.description = description;
        }
        
        public static QualityLevel fromScore(int score) {
            for (QualityLevel level : values()) {
                if (score >= level.minScore) {
                    return level;
                }
            }
            return VERY_POOR;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum IssueType {
        SYNTAX_ERROR,          // 语法错误
        STYLE_VIOLATION,       // 风格违规
        CODE_SMELL,            // 代码异味
        PERFORMANCE_ISSUE,     // 性能问题
        SECURITY_VULNERABILITY, // 安全漏洞
        BEST_PRACTICE_VIOLATION // 最佳实践违规
    }
    
    public enum IssueSeverity {
        CRITICAL(4, "严重"),
        HIGH(3, "高"),
        MEDIUM(2, "中"),
        LOW(1, "低"),
        INFO(0, "信息");
        
        private final int weight;
        private final String description;
        
        IssueSeverity(int weight, String description) {
            this.weight = weight;
            this.description = description;
        }
        
        public int getWeight() {
            return weight;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum SuggestionType {
        REFACTOR,              // 重构建议
        OPTIMIZATION,          // 优化建议
        STYLE_IMPROVEMENT,     // 风格改进
        BEST_PRACTICE,         // 最佳实践
        ALTERNATIVE_APPROACH   // 替代方案
    }
    
    public enum Priority {
        CRITICAL(4, "关键优先级"),
        HIGH(3, "高优先级"),
        MEDIUM(2, "中优先级"),
        LOW(1, "低优先级");
        
        private final int weight;
        private final String description;
        
        Priority(int weight, String description) {
            this.weight = weight;
            this.description = description;
        }
        
        public int getWeight() {
            return weight;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 便捷方法
    public boolean hasIssues() {
        return issues != null && !issues.isEmpty();
    }
    
    public boolean hasCriticalIssues() {
        return issues != null && issues.stream()
            .anyMatch(issue -> issue.getSeverity() == IssueSeverity.CRITICAL);
    }
    
    public long getIssueCount() {
        return issues != null ? issues.size() : 0;
    }
    
    public long getIssueCountBySeverity(IssueSeverity severity) {
        return issues != null ? issues.stream()
            .filter(issue -> issue.getSeverity() == severity)
            .count() : 0;
    }
    
    public boolean isHighQuality() {
        return qualityLevel == QualityLevel.EXCELLENT || qualityLevel == QualityLevel.GOOD;
    }
}