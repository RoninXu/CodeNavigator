package com.codenavigator.ai.service;

import com.codenavigator.ai.dto.CodeAnalysisRequest;
import com.codenavigator.ai.dto.CodeAnalysisResult;
import com.codenavigator.ai.dto.CodeAnalysisResult.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeAnalyzer {
    
    private final JavaCodeAnalyzer javaCodeAnalyzer;
    private final CodeQualityAssessor qualityAssessor;
    private final CodeStyleChecker styleChecker;
    private final BestPracticesChecker bestPracticesChecker;
    
    public CodeAnalysisResult analyzeCode(CodeAnalysisRequest request) {
        log.info("Starting code analysis for user: {}, module: {}", request.getUserId(), request.getModuleId());
        
        try {
            String analysisId = generateAnalysisId();
            
            // 基础验证
            validateRequest(request);
            
            // 执行分析
            List<CodeIssue> issues = new ArrayList<>();
            List<CodeSuggestion> suggestions = new ArrayList<>();
            QualityMetrics metrics = QualityMetrics.builder().build();
            
            // 根据分析类型执行相应的分析
            if (request.shouldAnalyzeQuality()) {
                var qualityResult = qualityAssessor.assessQuality(request.getCode(), request.getLanguage());
                issues.addAll(qualityResult.getIssues());
                suggestions.addAll(qualityResult.getSuggestions());
                updateMetrics(metrics, qualityResult.getMetrics());
            }
            
            if (request.shouldAnalyzeStyle()) {
                var styleResult = styleChecker.checkStyle(request.getCode(), request.getLanguage());
                issues.addAll(styleResult.getIssues());
                suggestions.addAll(styleResult.getSuggestions());
                updateMetrics(metrics, styleResult.getMetrics());
            }
            
            if (request.shouldAnalyzeBestPractices()) {
                var bestPracticesResult = bestPracticesChecker.checkBestPractices(request.getCode(), request.getLanguage());
                issues.addAll(bestPracticesResult.getIssues());
                suggestions.addAll(bestPracticesResult.getSuggestions());
                updateMetrics(metrics, bestPracticesResult.getMetrics());
            }
            
            // 特殊处理Java代码
            if (request.isJavaCode()) {
                var javaResult = javaCodeAnalyzer.analyzeJavaCode(request.getCode());
                issues.addAll(javaResult.getIssues());
                suggestions.addAll(javaResult.getSuggestions());
                updateMetrics(metrics, javaResult.getMetrics());
            }
            
            // 计算总体评分
            int overallScore = calculateOverallScore(metrics, issues);
            QualityLevel qualityLevel = QualityLevel.fromScore(overallScore);
            
            // 对问题和建议进行排序
            issues = prioritizeIssues(issues);
            suggestions = prioritizeSuggestions(suggestions);
            
            // 生成分析摘要
            String summary = generateSummary(overallScore, qualityLevel, issues, suggestions);
            
            // 构建结果
            CodeAnalysisResult result = CodeAnalysisResult.builder()
                .analysisId(analysisId)
                .analysisTime(LocalDateTime.now())
                .userId(request.getUserId())
                .moduleId(request.getModuleId())
                .overallScore(overallScore)
                .qualityLevel(qualityLevel)
                .metrics(metrics)
                .issues(issues)
                .suggestions(suggestions)
                .summary(summary)
                .metadata(buildMetadata(request))
                .build();
            
            log.info("Code analysis completed. Score: {}, Issues: {}, Suggestions: {}", 
                    overallScore, issues.size(), suggestions.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error analyzing code for user: {}", request.getUserId(), e);
            throw new CodeAnalysisException("代码分析失败: " + e.getMessage(), e);
        }
    }
    
    public CodeAnalysisResult quickAnalyze(String code, String language) {
        CodeAnalysisRequest request = CodeAnalysisRequest.builder()
            .code(code)
            .language(language)
            .analysisType(CodeAnalysisRequest.AnalysisType.COMPREHENSIVE)
            .build();
            
        return analyzeCode(request);
    }
    
    private void validateRequest(CodeAnalysisRequest request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("代码内容不能为空");
        }
        
        if (request.getLanguage() == null || request.getLanguage().trim().isEmpty()) {
            request.setLanguage("java"); // 默认Java
        }
        
        if (request.getAnalysisType() == null) {
            request.setAnalysisType(CodeAnalysisRequest.AnalysisType.COMPREHENSIVE);
        }
    }
    
    private String generateAnalysisId() {
        return "CA-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private void updateMetrics(QualityMetrics target, QualityMetrics source) {
        if (source == null) return;
        
        if (source.getCodeStyle() != null) {
            target.setCodeStyle(average(target.getCodeStyle(), source.getCodeStyle()));
        }
        if (source.getReadability() != null) {
            target.setReadability(average(target.getReadability(), source.getReadability()));
        }
        if (source.getMaintainability() != null) {
            target.setMaintainability(average(target.getMaintainability(), source.getMaintainability()));
        }
        if (source.getPerformance() != null) {
            target.setPerformance(average(target.getPerformance(), source.getPerformance()));
        }
        if (source.getSecurity() != null) {
            target.setSecurity(average(target.getSecurity(), source.getSecurity()));
        }
        if (source.getBestPractices() != null) {
            target.setBestPractices(average(target.getBestPractices(), source.getBestPractices()));
        }
    }
    
    private Integer average(Integer existing, Integer newValue) {
        if (existing == null) return newValue;
        if (newValue == null) return existing;
        return (existing + newValue) / 2;
    }
    
    private int calculateOverallScore(QualityMetrics metrics, List<CodeIssue> issues) {
        // 基础评分从指标计算
        double baseScore = 0.0;
        int metricCount = 0;
        
        if (metrics.getCodeStyle() != null) {
            baseScore += metrics.getCodeStyle();
            metricCount++;
        }
        if (metrics.getReadability() != null) {
            baseScore += metrics.getReadability();
            metricCount++;
        }
        if (metrics.getMaintainability() != null) {
            baseScore += metrics.getMaintainability();
            metricCount++;
        }
        if (metrics.getPerformance() != null) {
            baseScore += metrics.getPerformance();
            metricCount++;
        }
        if (metrics.getSecurity() != null) {
            baseScore += metrics.getSecurity();
            metricCount++;
        }
        if (metrics.getBestPractices() != null) {
            baseScore += metrics.getBestPractices();
            metricCount++;
        }
        
        if (metricCount > 0) {
            baseScore = baseScore / metricCount;
        } else {
            baseScore = 75.0; // 默认评分
        }
        
        // 根据问题严重程度扣分
        double penalty = 0.0;
        for (CodeIssue issue : issues) {
            switch (issue.getSeverity()) {
                case CRITICAL:
                    penalty += 15.0;
                    break;
                case HIGH:
                    penalty += 8.0;
                    break;
                case MEDIUM:
                    penalty += 4.0;
                    break;
                case LOW:
                    penalty += 1.0;
                    break;
                default:
                    // INFO level不扣分
                    break;
            }
        }
        
        int finalScore = (int) Math.max(0, Math.min(100, baseScore - penalty));
        return finalScore;
    }
    
    private List<CodeIssue> prioritizeIssues(List<CodeIssue> issues) {
        return issues.stream()
            .sorted((i1, i2) -> {
                // 按严重程度排序，严重的在前
                int severityCompare = Integer.compare(
                    i2.getSeverity().getWeight(), 
                    i1.getSeverity().getWeight()
                );
                if (severityCompare != 0) {
                    return severityCompare;
                }
                
                // 相同严重程度按行号排序
                if (i1.getLineNumber() != null && i2.getLineNumber() != null) {
                    return Integer.compare(i1.getLineNumber(), i2.getLineNumber());
                }
                
                return 0;
            })
            .collect(Collectors.toList());
    }
    
    private List<CodeSuggestion> prioritizeSuggestions(List<CodeSuggestion> suggestions) {
        return suggestions.stream()
            .sorted((s1, s2) -> {
                // 按优先级排序
                int priorityCompare = Integer.compare(
                    s2.getPriority().getWeight(),
                    s1.getPriority().getWeight()
                );
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                
                // 相同优先级按影响程度排序
                if (s1.getEstimatedImpact() != null && s2.getEstimatedImpact() != null) {
                    return Integer.compare(s2.getEstimatedImpact(), s1.getEstimatedImpact());
                }
                
                return 0;
            })
            .collect(Collectors.toList());
    }
    
    private String generateSummary(int overallScore, QualityLevel qualityLevel, 
                                 List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("代码质量评估完成。");
        summary.append("总体评分: ").append(overallScore).append("/100 (").append(qualityLevel.getDescription()).append(")。");
        
        if (issues.isEmpty()) {
            summary.append("未发现明显问题。");
        } else {
            long criticalCount = issues.stream()
                .filter(i -> i.getSeverity() == IssueSeverity.CRITICAL)
                .count();
            long highCount = issues.stream()
                .filter(i -> i.getSeverity() == IssueSeverity.HIGH)
                .count();
                
            if (criticalCount > 0) {
                summary.append("发现").append(criticalCount).append("个严重问题，");
            }
            if (highCount > 0) {
                summary.append("发现").append(highCount).append("个高优先级问题，");
            }
            summary.append("总计").append(issues.size()).append("个问题。");
        }
        
        if (!suggestions.isEmpty()) {
            long highPriorityCount = suggestions.stream()
                .filter(s -> s.getPriority() == Priority.HIGH)
                .count();
            summary.append("提供").append(suggestions.size()).append("条改进建议");
            if (highPriorityCount > 0) {
                summary.append("，其中").append(highPriorityCount).append("条为高优先级");
            }
            summary.append("。");
        }
        
        return summary.toString();
    }
    
    private Map<String, Object> buildMetadata(CodeAnalysisRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("language", request.getLanguage());
        metadata.put("analysisType", request.getAnalysisType().name());
        metadata.put("codeLength", request.getCode().length());
        metadata.put("lineCount", request.getCode().split("\n").length);
        
        if (request.getFileName() != null) {
            metadata.put("fileName", request.getFileName());
        }
        
        return metadata;
    }
    
    // 自定义异常类
    public static class CodeAnalysisException extends RuntimeException {
        public CodeAnalysisException(String message) {
            super(message);
        }
        
        public CodeAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}