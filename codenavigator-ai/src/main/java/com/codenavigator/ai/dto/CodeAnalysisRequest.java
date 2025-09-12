package com.codenavigator.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeAnalysisRequest {
    
    private String code;
    private String language;
    private String fileName;
    private AnalysisType analysisType;
    private String userId;
    private String moduleId;
    private Map<String, Object> options;
    
    public enum AnalysisType {
        QUALITY_ASSESSMENT,    // 质量评估
        STYLE_CHECK,          // 代码风格检查
        BEST_PRACTICES,       // 最佳实践检查
        PERFORMANCE_REVIEW,   // 性能审查
        SECURITY_SCAN,        // 安全扫描
        COMPREHENSIVE         // 综合分析
    }
    
    // 便捷方法
    public boolean isJavaCode() {
        return "java".equalsIgnoreCase(language) || 
               (fileName != null && fileName.endsWith(".java"));
    }
    
    public boolean shouldAnalyzeStyle() {
        return analysisType == AnalysisType.STYLE_CHECK || 
               analysisType == AnalysisType.COMPREHENSIVE;
    }
    
    public boolean shouldAnalyzeQuality() {
        return analysisType == AnalysisType.QUALITY_ASSESSMENT || 
               analysisType == AnalysisType.COMPREHENSIVE;
    }
    
    public boolean shouldAnalyzeBestPractices() {
        return analysisType == AnalysisType.BEST_PRACTICES || 
               analysisType == AnalysisType.COMPREHENSIVE;
    }
}