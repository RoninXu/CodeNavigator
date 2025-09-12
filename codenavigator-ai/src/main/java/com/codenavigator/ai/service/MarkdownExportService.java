package com.codenavigator.ai.service;

import com.codenavigator.ai.dto.CodeAnalysisResult;
import com.codenavigator.ai.dto.CodeAnalysisResult.*;
import com.codenavigator.ai.service.LearningNotesGenerator.LearningNotes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownExportService {
    
    private final LearningNotesGenerator learningNotesGenerator;
    
    // 导出模板配置
    private static final Map<ExportFormat, ExportTemplate> EXPORT_TEMPLATES = new HashMap<>();
    
    static {
        initializeExportTemplates();
    }
    
    public ExportResult exportCodeAnalysisReport(CodeAnalysisResult analysisResult, ExportOptions options) {
        log.info("导出代码分析报告，格式: {}", options.getFormat());
        
        try {
            String content;
            String fileName;
            
            switch (options.getFormat()) {
                case MARKDOWN:
                    content = generateMarkdownReport(analysisResult, options);
                    fileName = generateFileName("code_analysis_report", "md", analysisResult.getAnalysisTime());
                    break;
                case HTML:
                    content = generateHtmlReport(analysisResult, options);
                    fileName = generateFileName("code_analysis_report", "html", analysisResult.getAnalysisTime());
                    break;
                case PDF:
                    content = generatePdfReport(analysisResult, options);
                    fileName = generateFileName("code_analysis_report", "pdf", analysisResult.getAnalysisTime());
                    break;
                default:
                    throw new UnsupportedOperationException("不支持的导出格式: " + options.getFormat());
            }
            
            // 保存文件（如果指定了路径）
            String filePath = null;
            if (options.getOutputPath() != null) {
                filePath = saveToFile(content, options.getOutputPath(), fileName);
            }
            
            return ExportResult.builder()
                .success(true)
                .content(content)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(content.getBytes().length)
                .exportTime(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("导出代码分析报告失败", e);
            return ExportResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .exportTime(LocalDateTime.now())
                .build();
        }
    }
    
    public ExportResult exportLearningNotes(LearningNotes notes, ExportOptions options) {
        log.info("导出学习笔记，格式: {}", options.getFormat());
        
        try {
            String content;
            String fileName;
            
            switch (options.getFormat()) {
                case MARKDOWN:
                    content = learningNotesGenerator.generateMarkdownNotes(notes);
                    fileName = generateFileName("learning_notes", "md", notes.getCreatedTime());
                    break;
                case HTML:
                    content = convertMarkdownToHtml(learningNotesGenerator.generateMarkdownNotes(notes));
                    fileName = generateFileName("learning_notes", "html", notes.getCreatedTime());
                    break;
                default:
                    content = learningNotesGenerator.generateMarkdownNotes(notes);
                    fileName = generateFileName("learning_notes", "md", notes.getCreatedTime());
                    break;
            }
            
            String filePath = null;
            if (options.getOutputPath() != null) {
                filePath = saveToFile(content, options.getOutputPath(), fileName);
            }
            
            return ExportResult.builder()
                .success(true)
                .content(content)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(content.getBytes().length)
                .exportTime(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("导出学习笔记失败", e);
            return ExportResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .exportTime(LocalDateTime.now())
                .build();
        }
    }
    
    public ExportResult exportLearningPath(Object learningPath, ExportOptions options) {
        log.info("导出学习路径，格式: {}", options.getFormat());
        
        try {
            String content = generateLearningPathMarkdown(learningPath);
            String fileName = generateFileName("learning_path", "md", LocalDateTime.now());
            
            if (options.getFormat() == ExportFormat.HTML) {
                content = convertMarkdownToHtml(content);
                fileName = generateFileName("learning_path", "html", LocalDateTime.now());
            }
            
            String filePath = null;
            if (options.getOutputPath() != null) {
                filePath = saveToFile(content, options.getOutputPath(), fileName);
            }
            
            return ExportResult.builder()
                .success(true)
                .content(content)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(content.getBytes().length)
                .exportTime(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("导出学习路径失败", e);
            return ExportResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .exportTime(LocalDateTime.now())
                .build();
        }
    }
    
    public ExportResult exportBatchAnalysisReport(List<CodeAnalysisResult> results, ExportOptions options) {
        log.info("导出批量分析报告，包含 {} 个分析结果", results.size());
        
        try {
            String content = generateBatchAnalysisMarkdown(results);
            String fileName = generateFileName("batch_analysis_report", "md", LocalDateTime.now());
            
            if (options.getFormat() == ExportFormat.HTML) {
                content = convertMarkdownToHtml(content);
                fileName = generateFileName("batch_analysis_report", "html", LocalDateTime.now());
            }
            
            String filePath = null;
            if (options.getOutputPath() != null) {
                filePath = saveToFile(content, options.getOutputPath(), fileName);
            }
            
            return ExportResult.builder()
                .success(true)
                .content(content)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(content.getBytes().length)
                .exportTime(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("导出批量分析报告失败", e);
            return ExportResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .exportTime(LocalDateTime.now())
                .build();
        }
    }
    
    private String generateMarkdownReport(CodeAnalysisResult result, ExportOptions options) {
        StringBuilder markdown = new StringBuilder();
        ExportTemplate template = EXPORT_TEMPLATES.get(options.getFormat());
        
        // 报告头部
        markdown.append("# 代码分析报告\n\n");
        markdown.append(generateReportMetadata(result));
        markdown.append("\n");
        
        // 执行摘要
        markdown.append("## 📊 执行摘要\n\n");
        markdown.append(generateExecutiveSummary(result));
        markdown.append("\n");
        
        // 质量指标
        markdown.append("## 📈 质量指标\n\n");
        markdown.append(generateQualityMetrics(result.getMetrics()));
        markdown.append("\n");
        
        // 问题分析（如果包含详细信息）
        if (options.isIncludeDetailedIssues() && !result.getIssues().isEmpty()) {
            markdown.append("## 🐛 问题分析\n\n");
            markdown.append(generateIssueAnalysis(result.getIssues(), options));
            markdown.append("\n");
        }
        
        // 改进建议
        if (options.isIncludeSuggestions() && !result.getSuggestions().isEmpty()) {
            markdown.append("## 💡 改进建议\n\n");
            markdown.append(generateSuggestionsSection(result.getSuggestions(), options));
            markdown.append("\n");
        }
        
        // 代码统计信息
        if (options.isIncludeStatistics()) {
            markdown.append("## 📋 代码统计\n\n");
            markdown.append(generateCodeStatistics(result));
            markdown.append("\n");
        }
        
        // 报告尾部
        markdown.append(generateReportFooter(result));
        
        return markdown.toString();
    }
    
    private String generateHtmlReport(CodeAnalysisResult result, ExportOptions options) {
        String markdownContent = generateMarkdownReport(result, options);
        return convertMarkdownToHtml(markdownContent);
    }
    
    private String generatePdfReport(CodeAnalysisResult result, ExportOptions options) {
        // PDF生成需要额外的库支持，这里先返回Markdown格式
        // 在实际项目中，可以使用iText、Flying Saucer等库生成PDF
        return generateMarkdownReport(result, options);
    }
    
    private String generateReportMetadata(CodeAnalysisResult result) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("> **分析ID**: ").append(result.getAnalysisId()).append("\n");
        metadata.append("> **分析时间**: ").append(result.getAnalysisTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        metadata.append("> **用户ID**: ").append(result.getUserId() != null ? result.getUserId() : "未知").append("\n");
        metadata.append("> **模块ID**: ").append(result.getModuleId() != null ? result.getModuleId() : "未指定").append("\n");
        metadata.append("> **总体评分**: **").append(result.getOverallScore()).append("/100** (").append(result.getQualityLevel().getDescription()).append(")\n");
        return metadata.toString();
    }
    
    private String generateExecutiveSummary(CodeAnalysisResult result) {
        StringBuilder summary = new StringBuilder();
        
        summary.append(result.getSummary()).append("\n\n");
        
        // 关键指标卡片
        summary.append("### 关键指标\n\n");
        summary.append("| 指标 | 得分 | 状态 |\n");
        summary.append("|------|------|------|\n");
        
        QualityMetrics metrics = result.getMetrics();
        if (metrics.getCodeStyle() != null) {
            summary.append("| 代码规范 | ").append(metrics.getCodeStyle()).append("/100 | ").append(getScoreStatus(metrics.getCodeStyle())).append(" |\n");
        }
        if (metrics.getReadability() != null) {
            summary.append("| 可读性 | ").append(metrics.getReadability()).append("/100 | ").append(getScoreStatus(metrics.getReadability())).append(" |\n");
        }
        if (metrics.getMaintainability() != null) {
            summary.append("| 可维护性 | ").append(metrics.getMaintainability()).append("/100 | ").append(getScoreStatus(metrics.getMaintainability())).append(" |\n");
        }
        if (metrics.getPerformance() != null) {
            summary.append("| 性能 | ").append(metrics.getPerformance()).append("/100 | ").append(getScoreStatus(metrics.getPerformance())).append(" |\n");
        }
        if (metrics.getSecurity() != null) {
            summary.append("| 安全性 | ").append(metrics.getSecurity()).append("/100 | ").append(getScoreStatus(metrics.getSecurity())).append(" |\n");
        }
        if (metrics.getBestPractices() != null) {
            summary.append("| 最佳实践 | ").append(metrics.getBestPractices()).append("/100 | ").append(getScoreStatus(metrics.getBestPractices())).append(" |\n");
        }
        
        return summary.toString();
    }
    
    private String generateQualityMetrics(QualityMetrics metrics) {
        StringBuilder metricsSection = new StringBuilder();
        
        metricsSection.append("本次分析的详细质量指标如下：\n\n");
        
        // 雷达图数据（用于可视化，这里用文字描述）
        metricsSection.append("```\n");
        metricsSection.append("质量雷达图数据:\n");
        if (metrics.getCodeStyle() != null) {
            metricsSection.append("代码规范:    ").append(generateProgressBar(metrics.getCodeStyle())).append(" ").append(metrics.getCodeStyle()).append("/100\n");
        }
        if (metrics.getReadability() != null) {
            metricsSection.append("可读性:      ").append(generateProgressBar(metrics.getReadability())).append(" ").append(metrics.getReadability()).append("/100\n");
        }
        if (metrics.getMaintainability() != null) {
            metricsSection.append("可维护性:    ").append(generateProgressBar(metrics.getMaintainability())).append(" ").append(metrics.getMaintainability()).append("/100\n");
        }
        if (metrics.getPerformance() != null) {
            metricsSection.append("性能:        ").append(generateProgressBar(metrics.getPerformance())).append(" ").append(metrics.getPerformance()).append("/100\n");
        }
        if (metrics.getSecurity() != null) {
            metricsSection.append("安全性:      ").append(generateProgressBar(metrics.getSecurity())).append(" ").append(metrics.getSecurity()).append("/100\n");
        }
        if (metrics.getBestPractices() != null) {
            metricsSection.append("最佳实践:    ").append(generateProgressBar(metrics.getBestPractices())).append(" ").append(metrics.getBestPractices()).append("/100\n");
        }
        metricsSection.append("```\n");
        
        return metricsSection.toString();
    }
    
    private String generateIssueAnalysis(List<CodeIssue> issues, ExportOptions options) {
        StringBuilder issuesSection = new StringBuilder();
        
        // 按严重程度分组统计
        Map<IssueSeverity, List<CodeIssue>> issuesByType = issues.stream()
            .collect(Collectors.groupingBy(CodeIssue::getSeverity));
        
        issuesSection.append("发现 ").append(issues.size()).append(" 个问题，详细分类如下：\n\n");
        
        for (IssueSeverity severity : Arrays.asList(IssueSeverity.CRITICAL, IssueSeverity.HIGH, IssueSeverity.MEDIUM, IssueSeverity.LOW)) {
            List<CodeIssue> severityIssues = issuesByType.get(severity);
            if (severityIssues != null && !severityIssues.isEmpty()) {
                issuesSection.append("### ").append(getSeverityIcon(severity)).append(" ").append(getSeverityDisplayName(severity)).append("级问题 (").append(severityIssues.size()).append("个)\n\n");
                
                int displayCount = options.getMaxIssuesPerSeverity() > 0 ? 
                    Math.min(severityIssues.size(), options.getMaxIssuesPerSeverity()) : 
                    severityIssues.size();
                    
                for (int i = 0; i < displayCount; i++) {
                    CodeIssue issue = severityIssues.get(i);
                    issuesSection.append(String.format("**%d. %s** (第%d行)\n", 
                        i + 1, issue.getTitle(), issue.getLineNumber() != null ? issue.getLineNumber() : 0));
                    issuesSection.append("   ").append(issue.getDescription()).append("\n");
                    
                    if (issue.getCodeSnippet() != null && options.isIncludeCodeSnippets()) {
                        issuesSection.append("   ```java\n");
                        issuesSection.append("   ").append(issue.getCodeSnippet().replace("\n", "\n   ")).append("\n");
                        issuesSection.append("   ```\n");
                    }
                    issuesSection.append("\n");
                }
                
                if (severityIssues.size() > displayCount) {
                    issuesSection.append("   *... 还有 ").append(severityIssues.size() - displayCount).append(" 个类似问题*\n\n");
                }
            }
        }
        
        return issuesSection.toString();
    }
    
    private String generateSuggestionsSection(List<CodeSuggestion> suggestions, ExportOptions options) {
        StringBuilder suggestionsSection = new StringBuilder();
        
        suggestionsSection.append("基于代码分析结果，我们提供以下 ").append(suggestions.size()).append(" 条改进建议：\n\n");
        
        // 按优先级分组
        Map<Priority, List<CodeSuggestion>> suggestionsByPriority = suggestions.stream()
            .collect(Collectors.groupingBy(CodeSuggestion::getPriority));
        
        for (Priority priority : Arrays.asList(Priority.HIGH, Priority.MEDIUM, Priority.LOW)) {
            List<CodeSuggestion> prioritySuggestions = suggestionsByPriority.get(priority);
            if (prioritySuggestions != null && !prioritySuggestions.isEmpty()) {
                suggestionsSection.append("### ").append(getPriorityIcon(priority)).append(" ").append(getPriorityDisplayName(priority)).append("优先级建议\n\n");
                
                int displayCount = options.getMaxSuggestionsPerPriority() > 0 ?
                    Math.min(prioritySuggestions.size(), options.getMaxSuggestionsPerPriority()) :
                    prioritySuggestions.size();
                
                for (int i = 0; i < displayCount; i++) {
                    CodeSuggestion suggestion = prioritySuggestions.get(i);
                    suggestionsSection.append("**").append(i + 1).append(". ").append(suggestion.getTitle()).append("**\n");
                    suggestionsSection.append(suggestion.getDescription()).append("\n");
                    
                    if (suggestion.getEstimatedImpact() != null) {
                        suggestionsSection.append("*预期改进效果: ").append(suggestion.getEstimatedImpact()).append("%*\n");
                    }
                    
                    if (suggestion.getCodeExample() != null && options.isIncludeCodeExamples()) {
                        suggestionsSection.append("```java\n");
                        suggestionsSection.append(suggestion.getCodeExample()).append("\n");
                        suggestionsSection.append("```\n");
                    }
                    suggestionsSection.append("\n");
                }
                
                if (prioritySuggestions.size() > displayCount) {
                    suggestionsSection.append("*... 还有 ").append(prioritySuggestions.size() - displayCount).append(" 条建议*\n\n");
                }
            }
        }
        
        return suggestionsSection.toString();
    }
    
    private String generateCodeStatistics(CodeAnalysisResult result) {
        StringBuilder stats = new StringBuilder();
        
        Map<String, Object> metadata = result.getMetadata();
        if (metadata != null) {
            stats.append("| 统计项 | 数值 |\n");
            stats.append("|--------|------|\n");
            
            if (metadata.get("language") != null) {
                stats.append("| 编程语言 | ").append(metadata.get("language")).append(" |\n");
            }
            if (metadata.get("codeLength") != null) {
                stats.append("| 代码长度 | ").append(metadata.get("codeLength")).append(" 字符 |\n");
            }
            if (metadata.get("lineCount") != null) {
                stats.append("| 代码行数 | ").append(metadata.get("lineCount")).append(" 行 |\n");
            }
            if (metadata.get("fileName") != null) {
                stats.append("| 文件名 | ").append(metadata.get("fileName")).append(" |\n");
            }
            
            stats.append("| 发现问题数 | ").append(result.getIssues().size()).append(" 个 |\n");
            stats.append("| 改进建议数 | ").append(result.getSuggestions().size()).append(" 条 |\n");
        }
        
        return stats.toString();
    }
    
    private String generateReportFooter(CodeAnalysisResult result) {
        StringBuilder footer = new StringBuilder();
        
        footer.append("---\n\n");
        footer.append("### 📝 报告说明\n\n");
        footer.append("- 本报告由 CodeNavigator AI 自动生成\n");
        footer.append("- 评分基于多维度代码质量分析算法\n");
        footer.append("- 建议结合实际项目情况进行参考\n");
        footer.append("- 如有疑问，请联系技术支持团队\n\n");
        
        footer.append("**生成时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        footer.append("**版本**: CodeNavigator v1.0\n");
        
        return footer.toString();
    }
    
    private String generateLearningPathMarkdown(Object learningPath) {
        // TODO: 实现学习路径Markdown生成，当LearningPath类可用时
        StringBuilder markdown = new StringBuilder();
        
        markdown.append("# 学习路径报告\n\n");
        markdown.append("> **生成时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");
        
        markdown.append("## 📝 路径描述\n\n");
        markdown.append("学习路径功能正在开发中，将在后续版本中提供完整的路径导出功能。\n\n");
        
        return markdown.toString();
    }
    
    private String generateBatchAnalysisMarkdown(List<CodeAnalysisResult> results) {
        StringBuilder markdown = new StringBuilder();
        
        markdown.append("# 批量代码分析报告\n\n");
        markdown.append("> **分析时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        markdown.append("> **分析数量**: ").append(results.size()).append(" 个文件/模块\n\n");
        
        // 汇总统计
        markdown.append("## 📊 汇总统计\n\n");
        double avgScore = results.stream().mapToInt(CodeAnalysisResult::getOverallScore).average().orElse(0);
        int totalIssues = results.stream().mapToInt(r -> r.getIssues().size()).sum();
        int totalSuggestions = results.stream().mapToInt(r -> r.getSuggestions().size()).sum();
        
        markdown.append("| 统计项 | 数值 |\n");
        markdown.append("|--------|------|\n");
        markdown.append("| 平均评分 | ").append(String.format("%.1f", avgScore)).append("/100 |\n");
        markdown.append("| 总问题数 | ").append(totalIssues).append(" 个 |\n");
        markdown.append("| 总建议数 | ").append(totalSuggestions).append(" 条 |\n\n");
        
        // 各项目详情
        markdown.append("## 📋 各项目详情\n\n");
        for (int i = 0; i < results.size(); i++) {
            CodeAnalysisResult result = results.get(i);
            markdown.append("### ").append(i + 1).append(". 分析ID: ").append(result.getAnalysisId()).append("\n\n");
            markdown.append("**评分**: ").append(result.getOverallScore()).append("/100 (").append(result.getQualityLevel().getDescription()).append(")\n");
            markdown.append("**问题数**: ").append(result.getIssues().size()).append(" 个\n");
            markdown.append("**建议数**: ").append(result.getSuggestions().size()).append(" 条\n");
            markdown.append("**分析时间**: ").append(result.getAnalysisTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");
        }
        
        return markdown.toString();
    }
    
    private String convertMarkdownToHtml(String markdown) {
        // 简单的Markdown到HTML转换
        // 在实际项目中，建议使用专门的Markdown解析库如flexmark-java
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>CodeNavigator 报告</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }\n");
        html.append("h1, h2, h3 { color: #333; }\n");
        html.append("code { background-color: #f4f4f4; padding: 2px 4px; border-radius: 3px; }\n");
        html.append("pre { background-color: #f4f4f4; padding: 10px; border-radius: 5px; overflow-x: auto; }\n");
        html.append("table { border-collapse: collapse; width: 100%; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append("blockquote { border-left: 4px solid #ddd; margin: 0; padding-left: 20px; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        
        // 简单的Markdown转换逻辑
        String htmlContent = markdown
            .replaceAll("# (.*)", "<h1>$1</h1>")
            .replaceAll("## (.*)", "<h2>$1</h2>")
            .replaceAll("### (.*)", "<h3>$1</h3>")
            .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>")
            .replaceAll("\\*(.*?)\\*", "<em>$1</em>")
            .replaceAll("`(.*?)`", "<code>$1</code>")
            .replaceAll("^> (.*)", "<blockquote>$1</blockquote>")
            .replaceAll("^- (.*)", "<li>$1</li>")
            .replaceAll("\n\n", "</p><p>")
            .replaceAll("```java\n(.*?)\n```", "<pre><code>$1</code></pre>");
        
        html.append("<p>").append(htmlContent).append("</p>");
        html.append("\n</body>\n</html>");
        
        return html.toString();
    }
    
    private String saveToFile(String content, String outputPath, String fileName) throws IOException {
        Path filePath = Paths.get(outputPath, fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content.getBytes("UTF-8"));
        return filePath.toString();
    }
    
    // 辅助方法
    private String generateFileName(String prefix, String extension, LocalDateTime dateTime) {
        String timestamp = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.%s", prefix, timestamp, extension);
    }
    
    private String getScoreStatus(Integer score) {
        if (score >= 90) return "🟢 优秀";
        if (score >= 75) return "🟡 良好";
        if (score >= 60) return "🟠 一般";
        return "🔴 需改进";
    }
    
    private String getSeverityIcon(IssueSeverity severity) {
        switch (severity) {
            case CRITICAL: return "🔴";
            case HIGH: return "🟠";
            case MEDIUM: return "🟡";
            case LOW: return "🟢";
            default: return "ℹ️";
        }
    }
    
    private String getSeverityDisplayName(IssueSeverity severity) {
        switch (severity) {
            case CRITICAL: return "严重";
            case HIGH: return "高";
            case MEDIUM: return "中等";
            case LOW: return "低";
            default: return "信息";
        }
    }
    
    private String getPriorityIcon(Priority priority) {
        switch (priority) {
            case HIGH: return "🔥";
            case MEDIUM: return "⚡";
            case LOW: return "💡";
            default: return "💡";
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
    
    private String generateProgressBar(Integer score) {
        int bars = score / 10;
        StringBuilder progressBar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < bars) {
                progressBar.append("█");
            } else {
                progressBar.append("░");
            }
        }
        return progressBar.toString();
    }
    
    private static void initializeExportTemplates() {
        EXPORT_TEMPLATES.put(ExportFormat.MARKDOWN, ExportTemplate.builder()
            .headerTemplate("# {title}\n\n")
            .sectionTemplate("## {sectionTitle}\n\n{content}\n\n")
            .footerTemplate("---\n*Generated by CodeNavigator AI*\n")
            .build());
            
        EXPORT_TEMPLATES.put(ExportFormat.HTML, ExportTemplate.builder()
            .headerTemplate("<!DOCTYPE html><html><head><title>{title}</title></head><body>")
            .sectionTemplate("<section><h2>{sectionTitle}</h2><div>{content}</div></section>")
            .footerTemplate("<footer><p><em>Generated by CodeNavigator AI</em></p></footer></body></html>")
            .build());
    }
    
    // 枚举和内部类定义
    public enum ExportFormat {
        MARKDOWN, HTML, PDF
    }
    
    public static class ExportOptions {
        private ExportFormat format = ExportFormat.MARKDOWN;
        private String outputPath;
        private boolean includeDetailedIssues = true;
        private boolean includeSuggestions = true;
        private boolean includeStatistics = true;
        private boolean includeCodeSnippets = false;
        private boolean includeCodeExamples = true;
        private int maxIssuesPerSeverity = 10;
        private int maxSuggestionsPerPriority = 5;
        
        public static ExportOptionsBuilder builder() {
            return new ExportOptionsBuilder();
        }
        
        // Getters
        public ExportFormat getFormat() { return format; }
        public String getOutputPath() { return outputPath; }
        public boolean isIncludeDetailedIssues() { return includeDetailedIssues; }
        public boolean isIncludeSuggestions() { return includeSuggestions; }
        public boolean isIncludeStatistics() { return includeStatistics; }
        public boolean isIncludeCodeSnippets() { return includeCodeSnippets; }
        public boolean isIncludeCodeExamples() { return includeCodeExamples; }
        public int getMaxIssuesPerSeverity() { return maxIssuesPerSeverity; }
        public int getMaxSuggestionsPerPriority() { return maxSuggestionsPerPriority; }
        
        public static class ExportOptionsBuilder {
            private ExportOptions options = new ExportOptions();
            
            public ExportOptionsBuilder format(ExportFormat format) { options.format = format; return this; }
            public ExportOptionsBuilder outputPath(String outputPath) { options.outputPath = outputPath; return this; }
            public ExportOptionsBuilder includeDetailedIssues(boolean include) { options.includeDetailedIssues = include; return this; }
            public ExportOptionsBuilder includeSuggestions(boolean include) { options.includeSuggestions = include; return this; }
            public ExportOptionsBuilder includeStatistics(boolean include) { options.includeStatistics = include; return this; }
            public ExportOptionsBuilder includeCodeSnippets(boolean include) { options.includeCodeSnippets = include; return this; }
            public ExportOptionsBuilder includeCodeExamples(boolean include) { options.includeCodeExamples = include; return this; }
            public ExportOptionsBuilder maxIssuesPerSeverity(int max) { options.maxIssuesPerSeverity = max; return this; }
            public ExportOptionsBuilder maxSuggestionsPerPriority(int max) { options.maxSuggestionsPerPriority = max; return this; }
            
            public ExportOptions build() { return options; }
        }
    }
    
    public static class ExportResult {
        private boolean success;
        private String content;
        private String fileName;
        private String filePath;
        private long fileSize;
        private LocalDateTime exportTime;
        private String errorMessage;
        
        public static ExportResultBuilder builder() {
            return new ExportResultBuilder();
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getContent() { return content; }
        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
        public LocalDateTime getExportTime() { return exportTime; }
        public String getErrorMessage() { return errorMessage; }
        
        public static class ExportResultBuilder {
            private ExportResult result = new ExportResult();
            
            public ExportResultBuilder success(boolean success) { result.success = success; return this; }
            public ExportResultBuilder content(String content) { result.content = content; return this; }
            public ExportResultBuilder fileName(String fileName) { result.fileName = fileName; return this; }
            public ExportResultBuilder filePath(String filePath) { result.filePath = filePath; return this; }
            public ExportResultBuilder fileSize(long fileSize) { result.fileSize = fileSize; return this; }
            public ExportResultBuilder exportTime(LocalDateTime exportTime) { result.exportTime = exportTime; return this; }
            public ExportResultBuilder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            
            public ExportResult build() { return result; }
        }
    }
    
    private static class ExportTemplate {
        private String headerTemplate;
        private String sectionTemplate;
        private String footerTemplate;
        
        public static ExportTemplateBuilder builder() {
            return new ExportTemplateBuilder();
        }
        
        public String getHeaderTemplate() { return headerTemplate; }
        public String getSectionTemplate() { return sectionTemplate; }
        public String getFooterTemplate() { return footerTemplate; }
        
        public static class ExportTemplateBuilder {
            private ExportTemplate template = new ExportTemplate();
            
            public ExportTemplateBuilder headerTemplate(String headerTemplate) { 
                template.headerTemplate = headerTemplate; return this; }
            public ExportTemplateBuilder sectionTemplate(String sectionTemplate) { 
                template.sectionTemplate = sectionTemplate; return this; }
            public ExportTemplateBuilder footerTemplate(String footerTemplate) { 
                template.footerTemplate = footerTemplate; return this; }
            
            public ExportTemplate build() { return template; }
        }
    }
}