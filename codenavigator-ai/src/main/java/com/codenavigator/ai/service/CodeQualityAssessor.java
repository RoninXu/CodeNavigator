package com.codenavigator.ai.service;

import com.codenavigator.ai.dto.CodeAnalysisResult.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeQualityAssessor {
    
    // 代码质量规则定义
    private static final Map<String, QualityRule> QUALITY_RULES;
    static {
        Map<String, QualityRule> rules = new HashMap<>();
        
        // 可读性规则
        rules.put("long-line", new QualityRule(
            "代码行过长",
            "单行代码不应超过120个字符",
            IssueSeverity.LOW,
            IssueType.STYLE_VIOLATION,
            line -> line.length() > 120
        ));
        
        rules.put("magic-number", new QualityRule(
            "魔法数字",
            "避免在代码中直接使用未解释的数字常量",
            IssueSeverity.MEDIUM,
            IssueType.CODE_SMELL,
            line -> containsMagicNumbers(line)
        ));
        
        rules.put("todo-comment", new QualityRule(
            "TODO注释",
            "存在未完成的TODO注释",
            IssueSeverity.LOW,
            IssueType.CODE_SMELL,
            line -> line.contains("TODO") || line.contains("FIXME")
        ));
        
        rules.put("empty-line", new QualityRule(
            "过多空行",
            "连续空行不应超过2行",
            IssueSeverity.LOW,
            IssueType.STYLE_VIOLATION,
            line -> false // 需要特殊处理
        ));
        
        QUALITY_RULES = Collections.unmodifiableMap(rules);
    }
    
    public QualityAssessmentResult assessQuality(String code, String language) {
        log.debug("Assessing code quality for language: {}", language);
        
        List<CodeIssue> issues = new ArrayList<>();
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        String[] lines = code.split("\n");
        
        // 逐行分析代码质量
        analyzeLines(lines, issues, suggestions);
        
        // 整体结构分析
        analyzeOverallStructure(code, issues, suggestions);
        
        // 计算质量指标
        QualityMetrics metrics = calculateQualityMetrics(code, issues);
        
        return new QualityAssessmentResult(issues, suggestions, metrics);
    }
    
    private void analyzeLines(String[] lines, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        int consecutiveEmptyLines = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            
            // 处理空行计数
            if (line.trim().isEmpty()) {
                consecutiveEmptyLines++;
            } else {
                if (consecutiveEmptyLines > 2) {
                    issues.add(createIssue(
                        "empty-line",
                        QUALITY_RULES.get("empty-line"),
                        lineNumber - consecutiveEmptyLines,
                        String.format("连续%d个空行，建议控制在2个以内", consecutiveEmptyLines)
                    ));
                }
                consecutiveEmptyLines = 0;
            }
            
            // 应用其他规则
            for (Map.Entry<String, QualityRule> entry : QUALITY_RULES.entrySet()) {
                String ruleId = entry.getKey();
                QualityRule rule = entry.getValue();
                
                if (!"empty-line".equals(ruleId) && rule.predicate.test(line)) {
                    issues.add(createIssue(ruleId, rule, lineNumber, rule.description));
                    
                    // 为某些问题提供建议
                    if ("magic-number".equals(ruleId)) {
                        suggestions.add(createMagicNumberSuggestion(line, lineNumber));
                    } else if ("long-line".equals(ruleId)) {
                        suggestions.add(createLongLineSuggestion(line, lineNumber));
                    }
                }
            }
            
            // 检查注释质量
            analyzeComments(line, lineNumber, issues, suggestions);
            
            // 检查变量声明
            analyzeVariableDeclarations(line, lineNumber, issues, suggestions);
        }
    }
    
    private void analyzeOverallStructure(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 分析代码整体结构
        
        // 检查类的大小
        int totalLines = code.split("\n").length;
        if (totalLines > 500) {
            issues.add(CodeIssue.builder()
                .id(UUID.randomUUID().toString())
                .type(IssueType.CODE_SMELL)
                .severity(IssueSeverity.MEDIUM)
                .title("类过大")
                .description(String.format("类有%d行代码，建议控制在500行以内", totalLines))
                .rule("class-size-limit")
                .fixSuggestions(Arrays.asList(
                    "将类拆分为多个较小的类",
                    "提取公共功能到工具类",
                    "使用组合替代继承"
                ))
                .build());
        }
        
        // 检查重复代码
        detectDuplicateCode(code, issues, suggestions);
        
        // 检查代码复杂度模式
        analyzeComplexityPatterns(code, issues, suggestions);
    }
    
    private void analyzeComments(String line, int lineNumber, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        String trimmedLine = line.trim();
        
        // 检查注释质量
        if (trimmedLine.startsWith("//") || trimmedLine.startsWith("/*")) {
            // 检查是否是无意义的注释
            if (isUselessComment(trimmedLine)) {
                issues.add(CodeIssue.builder()
                    .id(UUID.randomUUID().toString())
                    .type(IssueType.STYLE_VIOLATION)
                    .severity(IssueSeverity.LOW)
                    .title("无意义注释")
                    .description("注释应该解释代码的意图，而不是重述代码本身")
                    .lineNumber(lineNumber)
                    .rule("meaningful-comments")
                    .fixSuggestions(Arrays.asList("删除冗余注释", "添加有意义的解释"))
                    .build());
            }
            
            // 检查注释是否过长
            if (trimmedLine.length() > 100) {
                suggestions.add(CodeSuggestion.builder()
                    .id(UUID.randomUUID().toString())
                    .type(SuggestionType.STYLE_IMPROVEMENT)
                    .priority(Priority.LOW)
                    .title("注释过长")
                    .description("考虑将长注释拆分为多行")
                    .explanation("适当的注释长度有助于提高可读性")
                    .benefits(Arrays.asList("提高代码可读性"))
                    .estimatedImpact(2)
                    .build());
            }
        }
    }
    
    private void analyzeVariableDeclarations(String line, int lineNumber, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查变量声明是否合理
        if (line.contains("=") && (line.contains("int ") || line.contains("String ") || line.contains("double "))) {
            // 检查是否一行声明多个变量
            if (line.split(",").length > 1 && line.contains("int ")) {
                suggestions.add(CodeSuggestion.builder()
                    .id(UUID.randomUUID().toString())
                    .type(SuggestionType.STYLE_IMPROVEMENT)
                    .priority(Priority.MEDIUM)
                    .title("分开声明变量")
                    .description("建议每行只声明一个变量")
                    .explanation("单独声明变量可以提高代码的可读性和可维护性")
                    .benefits(Arrays.asList("提高代码可读性", "便于调试", "减少错误"))
                    .estimatedImpact(4)
                    .build());
            }
        }
    }
    
    private void detectDuplicateCode(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 简化的重复代码检测
        String[] lines = code.split("\n");
        Map<String, List<Integer>> lineMap = new HashMap<>();
        
        for (int i = 0; i < lines.length; i++) {
            String normalizedLine = lines[i].trim();
            if (!normalizedLine.isEmpty() && !normalizedLine.startsWith("//")) {
                lineMap.computeIfAbsent(normalizedLine, k -> new ArrayList<>()).add(i + 1);
            }
        }
        
        for (Map.Entry<String, List<Integer>> entry : lineMap.entrySet()) {
            if (entry.getValue().size() > 2) { // 出现3次或以上
                issues.add(CodeIssue.builder()
                    .id(UUID.randomUUID().toString())
                    .type(IssueType.CODE_SMELL)
                    .severity(IssueSeverity.MEDIUM)
                    .title("重复代码")
                    .description(String.format("代码行 '%s' 重复出现 %d 次", 
                               entry.getKey().substring(0, Math.min(50, entry.getKey().length())),
                               entry.getValue().size()))
                    .lineNumber(entry.getValue().get(0))
                    .rule("no-duplicate-code")
                    .fixSuggestions(Arrays.asList(
                        "提取重复代码到方法中",
                        "使用循环替代重复语句",
                        "创建工具方法"
                    ))
                    .build());
                    
                suggestions.add(CodeSuggestion.builder()
                    .id(UUID.randomUUID().toString())
                    .type(SuggestionType.REFACTOR)
                    .priority(Priority.HIGH)
                    .title("消除重复代码")
                    .description("将重复的代码提取到单独的方法中")
                    .explanation("消除重复代码可以提高代码的可维护性并减少bug")
                    .benefits(Arrays.asList("提高可维护性", "减少bug风险", "代码更简洁"))
                    .estimatedImpact(8)
                    .build());
            }
        }
    }
    
    private void analyzeComplexityPatterns(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查嵌套深度
        String[] lines = code.split("\n");
        int maxNestingLevel = 0;
        int currentNestingLevel = 0;
        int deepNestingLine = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // 计算嵌套层级
            if (line.contains("{")) {
                currentNestingLevel++;
                if (currentNestingLevel > maxNestingLevel) {
                    maxNestingLevel = currentNestingLevel;
                    deepNestingLine = i + 1;
                }
            }
            if (line.contains("}")) {
                currentNestingLevel--;
            }
        }
        
        if (maxNestingLevel > 4) {
            issues.add(CodeIssue.builder()
                .id(UUID.randomUUID().toString())
                .type(IssueType.CODE_SMELL)
                .severity(IssueSeverity.MEDIUM)
                .title("嵌套层次过深")
                .description(String.format("代码嵌套深度达到%d层，建议控制在4层以内", maxNestingLevel))
                .lineNumber(deepNestingLine)
                .rule("nesting-depth-limit")
                .fixSuggestions(Arrays.asList(
                    "提取嵌套逻辑到单独方法",
                    "使用早期返回减少嵌套",
                    "重构条件判断逻辑"
                ))
                .build());
                
            suggestions.add(CodeSuggestion.builder()
                .id(UUID.randomUUID().toString())
                .type(SuggestionType.REFACTOR)
                .priority(Priority.HIGH)
                .title("降低嵌套复杂度")
                .description("考虑重构深层嵌套的代码结构")
                .explanation("降低嵌套深度可以显著提高代码的可读性")
                .benefits(Arrays.asList("提高可读性", "降低复杂度", "便于测试"))
                .estimatedImpact(9)
                .build());
        }
    }
    
    private QualityMetrics calculateQualityMetrics(String code, List<CodeIssue> issues) {
        int totalLines = code.split("\n").length;
        
        // 基于问题数量和代码长度计算各项指标
        long styleIssues = issues.stream().filter(i -> i.getType() == IssueType.STYLE_VIOLATION).count();
        long codeSmells = issues.stream().filter(i -> i.getType() == IssueType.CODE_SMELL).count();
        long criticalIssues = issues.stream().filter(i -> i.getSeverity() == IssueSeverity.CRITICAL).count();
        
        // 代码风格评分
        int codeStyle = Math.max(50, 100 - (int)(styleIssues * 3));
        
        // 可读性评分（基于代码长度、注释比例等）
        int commentLines = (int) Arrays.stream(code.split("\n"))
            .filter(line -> line.trim().startsWith("//") || line.trim().startsWith("/*"))
            .count();
        int readability = Math.max(60, 90 - (int)(codeSmells * 5) + Math.min(20, commentLines * 2));
        
        // 可维护性评分
        int maintainability = Math.max(50, 95 - (int)(codeSmells * 8) - (int)(criticalIssues * 15));
        
        // 性能评分（简化计算）
        int performance = 85; // 默认值，需要更深入的性能分析
        
        // 安全性评分
        int security = 90; // 默认值，需要安全扫描
        
        // 最佳实践评分
        long bestPracticeViolations = issues.stream()
            .filter(i -> i.getType() == IssueType.BEST_PRACTICE_VIOLATION)
            .count();
        int bestPractices = Math.max(60, 100 - (int)(bestPracticeViolations * 10));
        
        return QualityMetrics.builder()
            .codeStyle(codeStyle)
            .readability(readability)
            .maintainability(maintainability)
            .performance(performance)
            .security(security)
            .bestPractices(bestPractices)
            .build();
    }
    
    // 辅助方法
    private CodeIssue createIssue(String ruleId, QualityRule rule, int lineNumber, String description) {
        return CodeIssue.builder()
            .id(UUID.randomUUID().toString())
            .type(rule.issueType)
            .severity(rule.severity)
            .title(rule.title)
            .description(description)
            .lineNumber(lineNumber)
            .rule(ruleId)
            .fixSuggestions(generateFixSuggestions(ruleId))
            .build();
    }
    
    private List<String> generateFixSuggestions(String ruleId) {
        Map<String, List<String>> suggestions = new HashMap<>();
        suggestions.put("long-line", Arrays.asList("将长行拆分为多行", "提取复杂表达式到变量"));
        suggestions.put("magic-number", Arrays.asList("定义命名常量", "使用枚举值", "添加解释性注释"));
        suggestions.put("todo-comment", Arrays.asList("完成TODO项", "创建任务跟踪", "添加计划时间"));
        
        return suggestions.getOrDefault(ruleId, Arrays.asList("参考代码规范修复"));
    }
    
    private CodeSuggestion createMagicNumberSuggestion(String line, int lineNumber) {
        return CodeSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(SuggestionType.BEST_PRACTICE)
            .priority(Priority.MEDIUM)
            .title("使用命名常量")
            .description("将魔法数字替换为有意义的命名常量")
            .originalCode(line.trim())
            .explanation("命名常量可以提高代码的可读性和可维护性")
            .benefits(Arrays.asList("提高代码可读性", "便于维护和修改", "避免重复定义"))
            .estimatedImpact(6)
            .build();
    }
    
    private CodeSuggestion createLongLineSuggestion(String line, int lineNumber) {
        return CodeSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .type(SuggestionType.STYLE_IMPROVEMENT)
            .priority(Priority.LOW)
            .title("拆分长行")
            .description("将过长的代码行适当拆分")
            .originalCode(line.trim())
            .explanation("适当的行长度有助于提高代码的可读性")
            .benefits(Arrays.asList("提高可读性", "便于代码审查", "适应不同屏幕尺寸"))
            .estimatedImpact(3)
            .build();
    }
    
    private static boolean containsMagicNumbers(String line) {
        // 简化的魔法数字检测
        Pattern pattern = Pattern.compile("\\b(?!0|1)\\d{2,}\\b");
        return pattern.matcher(line).find() && 
               !line.contains("//") && 
               !line.contains("final") &&
               !line.contains("static final");
    }
    
    private boolean isUselessComment(String comment) {
        // 检查是否是无意义的注释
        String content = comment.replaceAll("//|/\\*|\\*/", "").trim().toLowerCase();
        
        String[] uselessPatterns = {
            "getter", "setter", "constructor", "end of", "start of",
            "initialize", "declaration", "definition"
        };
        
        return Arrays.stream(uselessPatterns).anyMatch(content::contains);
    }
    
    // 内部类
    private static class QualityRule {
        final String title;
        final String description;
        final IssueSeverity severity;
        final IssueType issueType;
        final java.util.function.Predicate<String> predicate;
        
        QualityRule(String title, String description, IssueSeverity severity, 
                   IssueType issueType, java.util.function.Predicate<String> predicate) {
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.issueType = issueType;
            this.predicate = predicate;
        }
    }
    
    public static class QualityAssessmentResult {
        private final List<CodeIssue> issues;
        private final List<CodeSuggestion> suggestions;
        private final QualityMetrics metrics;
        
        public QualityAssessmentResult(List<CodeIssue> issues, List<CodeSuggestion> suggestions, QualityMetrics metrics) {
            this.issues = issues;
            this.suggestions = suggestions;
            this.metrics = metrics;
        }
        
        public List<CodeIssue> getIssues() {
            return issues;
        }
        
        public List<CodeSuggestion> getSuggestions() {
            return suggestions;
        }
        
        public QualityMetrics getMetrics() {
            return metrics;
        }
    }
}