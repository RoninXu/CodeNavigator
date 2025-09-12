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
public class CodeStyleChecker {
    
    // 代码风格规则
    private static final Map<String, StyleRule> STYLE_RULES;
    static {
        Map<String, StyleRule> rules = new HashMap<>();
        
        // 缩进规则
        rules.put("indentation", new StyleRule(
            "缩进不一致",
            "代码缩进应保持一致，建议使用4个空格",
            IssueSeverity.LOW,
            line -> checkIndentation(line)
        ));
        
        // 括号风格
        rules.put("brace-style", new StyleRule(
            "括号风格",
            "左括号应在同一行末尾",
            IssueSeverity.LOW,
            line -> checkBraceStyle(line)
        ));
        
        // 空格规则
        rules.put("spacing", new StyleRule(
            "空格使用",
            "操作符前后应有空格",
            IssueSeverity.LOW,
            line -> checkSpacing(line)
        ));
        
        // 空行规则
        rules.put("blank-lines", new StyleRule(
            "空行使用",
            "方法间应有适当的空行分隔",
            IssueSeverity.LOW,
            line -> false // 需要上下文判断
        ));
        
        STYLE_RULES = Collections.unmodifiableMap(rules);
    }
    
    public StyleCheckResult checkStyle(String code, String language) {
        log.debug("Checking code style for language: {}", language);
        
        List<CodeIssue> issues = new ArrayList<>();
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        String[] lines = code.split("\n");
        
        // 分析每一行的风格
        analyzeLineStyle(lines, issues, suggestions);
        
        // 分析整体风格一致性
        analyzeOverallStyle(code, issues, suggestions);
        
        // 计算风格指标
        QualityMetrics metrics = calculateStyleMetrics(code, issues);
        
        return new StyleCheckResult(issues, suggestions, metrics);
    }
    
    private void analyzeLineStyle(String[] lines, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        Map<Integer, Integer> indentationMap = new HashMap<>();
        int blankLineCount = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            
            // 空行计数
            if (line.trim().isEmpty()) {
                blankLineCount++;
                continue;
            } else {
                // 检查连续空行
                if (blankLineCount > 3) {
                    issues.add(createStyleIssue(
                        "blank-lines",
                        String.format("连续%d个空行过多，建议控制在2-3行内", blankLineCount),
                        lineNumber - blankLineCount,
                        IssueSeverity.LOW
                    ));
                }
                blankLineCount = 0;
            }
            
            // 检查缩进
            checkLineIndentation(line, lineNumber, indentationMap, issues);
            
            // 检查括号风格
            checkLineBraceStyle(line, lineNumber, issues);
            
            // 检查空格使用
            checkLineSpacing(line, lineNumber, issues, suggestions);
            
            // 检查行尾空格
            if (line.endsWith(" ") || line.endsWith("\t")) {
                issues.add(createStyleIssue(
                    "trailing-whitespace",
                    "行尾存在多余的空格或制表符",
                    lineNumber,
                    IssueSeverity.LOW
                ));
            }
            
            // 检查制表符使用
            if (line.contains("\t")) {
                suggestions.add(CodeSuggestion.builder()
                    .id(UUID.randomUUID().toString())
                    .type(SuggestionType.STYLE_IMPROVEMENT)
                    .priority(Priority.LOW)
                    .title("统一使用空格缩进")
                    .description("建议使用空格代替制表符进行缩进")
                    .explanation("使用空格缩进可以在不同编辑器中保持一致的显示效果")
                    .benefits(Arrays.asList("保持跨平台一致性", "避免混合缩进问题"))
                    .estimatedImpact(2)
                    .build());
            }
        }
        
        // 检查缩进一致性
        checkIndentationConsistency(indentationMap, issues);
    }
    
    private void analyzeOverallStyle(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查命名风格一致性
        analyzeNamingStyleConsistency(code, issues, suggestions);
        
        // 检查导入语句风格
        analyzeImportStyle(code, issues, suggestions);
        
        // 检查注释风格
        analyzeCommentStyle(code, issues, suggestions);
        
        // 检查方法声明风格
        analyzeMethodDeclarationStyle(code, issues, suggestions);
    }
    
    private void checkLineIndentation(String line, int lineNumber, Map<Integer, Integer> indentationMap, 
                                    List<CodeIssue> issues) {
        if (line.trim().isEmpty()) return;
        
        int spaces = 0;
        int tabs = 0;
        
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                spaces++;
            } else if (c == '\t') {
                tabs++;
            } else {
                break;
            }
        }
        
        // 记录缩进模式
        if (spaces > 0) {
            indentationMap.merge(spaces, 1, Integer::sum);
        }
        
        // 检查混合缩进
        if (spaces > 0 && tabs > 0) {
            issues.add(createStyleIssue(
                "mixed-indentation",
                "不应混合使用空格和制表符进行缩进",
                lineNumber,
                IssueSeverity.MEDIUM
            ));
        }
    }
    
    private void checkLineBraceStyle(String line, int lineNumber, List<CodeIssue> issues) {
        String trimmed = line.trim();
        
        // 检查左括号是否单独一行（违反K&R风格）
        if (trimmed.equals("{") && !trimmed.startsWith("//")) {
            issues.add(createStyleIssue(
                "brace-style",
                "建议将左括号放在上一行末尾（K&R风格）",
                lineNumber,
                IssueSeverity.LOW
            ));
        }
        
        // 检查右括号后是否有不必要的空行
        if (trimmed.equals("}") && line.length() > trimmed.length()) {
            issues.add(createStyleIssue(
                "brace-style",
                "右括号后不应有多余的空格",
                lineNumber,
                IssueSeverity.LOW
            ));
        }
    }
    
    private void checkLineSpacing(String line, int lineNumber, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查操作符周围的空格
        String[] operators = {"=", "+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">="};
        
        for (String op : operators) {
            if (line.contains(op) && !line.contains("//")) {
                // 简化检查：操作符前后应有空格
                Pattern pattern = Pattern.compile("\\S" + Pattern.quote(op) + "\\S");
                if (pattern.matcher(line).find()) {
                    suggestions.add(CodeSuggestion.builder()
                        .id(UUID.randomUUID().toString())
                        .type(SuggestionType.STYLE_IMPROVEMENT)
                        .priority(Priority.LOW)
                        .title("添加操作符空格")
                        .description(String.format("在操作符 '%s' 前后添加空格", op))
                        .explanation("操作符前后的空格有助于提高代码可读性")
                        .benefits(Arrays.asList("提高代码可读性"))
                        .estimatedImpact(1)
                        .build());
                }
            }
        }
        
        // 检查逗号后的空格
        if (line.contains(",") && !line.contains("//")) {
            Pattern pattern = Pattern.compile(",\\S");
            if (pattern.matcher(line).find()) {
                issues.add(createStyleIssue(
                    "comma-spacing",
                    "逗号后应有一个空格",
                    lineNumber,
                    IssueSeverity.LOW
                ));
            }
        }
        
        // 检查分号前的空格
        if (line.contains(";") && !line.contains("//")) {
            Pattern pattern = Pattern.compile("\\s;");
            if (pattern.matcher(line).find()) {
                issues.add(createStyleIssue(
                    "semicolon-spacing",
                    "分号前不应有空格",
                    lineNumber,
                    IssueSeverity.LOW
                ));
            }
        }
    }
    
    private void checkIndentationConsistency(Map<Integer, Integer> indentationMap, List<CodeIssue> issues) {
        if (indentationMap.size() > 2) { // 允许基础缩进和一级嵌套
            // 找出最常用的缩进
            int mostCommonIndent = indentationMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(4);
                
            issues.add(createStyleIssue(
                "indentation-consistency",
                String.format("缩进不一致，建议统一使用%d个空格", mostCommonIndent),
                1,
                IssueSeverity.MEDIUM
            ));
        }
    }
    
    private void analyzeNamingStyleConsistency(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检查变量命名风格是否一致
        Pattern variablePattern = Pattern.compile("\\b(?:int|String|double|boolean|long)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
        java.util.regex.Matcher matcher = variablePattern.matcher(code);
        
        Set<String> camelCaseVars = new HashSet<>();
        Set<String> underscoreVars = new HashSet<>();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            if (varName.contains("_")) {
                underscoreVars.add(varName);
            } else if (Character.isLowerCase(varName.charAt(0))) {
                camelCaseVars.add(varName);
            }
        }
        
        if (!camelCaseVars.isEmpty() && !underscoreVars.isEmpty()) {
            suggestions.add(CodeSuggestion.builder()
                .id(UUID.randomUUID().toString())
                .type(SuggestionType.STYLE_IMPROVEMENT)
                .priority(Priority.MEDIUM)
                .title("统一变量命名风格")
                .description("建议在整个代码中使用一致的变量命名风格（驼峰命名或下划线）")
                .explanation("一致的命名风格有助于提高代码的专业性和可读性")
                .benefits(Arrays.asList("提高代码一致性", "便于团队协作"))
                .estimatedImpact(5)
                .build());
        }
    }
    
    private void analyzeImportStyle(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        String[] lines = code.split("\n");
        List<String> imports = new ArrayList<>();
        boolean hasWildcardImport = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("import ")) {
                imports.add(line);
                if (line.contains("*")) {
                    hasWildcardImport = true;
                    issues.add(createStyleIssue(
                        "wildcard-import",
                        "避免使用通配符导入，明确指定需要的类",
                        i + 1,
                        IssueSeverity.MEDIUM
                    ));
                }
            }
        }
        
        // 检查导入排序
        if (imports.size() > 1) {
            List<String> sortedImports = new ArrayList<>(imports);
            Collections.sort(sortedImports);
            
            if (!imports.equals(sortedImports)) {
                suggestions.add(CodeSuggestion.builder()
                    .id(UUID.randomUUID().toString())
                    .type(SuggestionType.STYLE_IMPROVEMENT)
                    .priority(Priority.LOW)
                    .title("对导入语句排序")
                    .description("建议按字母顺序排列导入语句")
                    .explanation("有序的导入语句便于查找和维护")
                    .benefits(Arrays.asList("提高代码整洁度", "便于查找依赖"))
                    .estimatedImpact(2)
                    .build());
            }
        }
    }
    
    private void analyzeCommentStyle(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        String[] lines = code.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            
            // 检查单行注释风格
            if (trimmed.startsWith("//")) {
                // 检查注释后是否有空格
                if (trimmed.length() > 2 && trimmed.charAt(2) != ' ') {
                    issues.add(createStyleIssue(
                        "comment-spacing",
                        "单行注释 '//' 后应有一个空格",
                        i + 1,
                        IssueSeverity.LOW
                    ));
                }
            }
            
            // 检查行内注释
            int commentIndex = line.indexOf("//");
            if (commentIndex > 0 && commentIndex < line.length() - 2) {
                // 检查注释前是否有空格
                if (line.charAt(commentIndex - 1) != ' ') {
                    issues.add(createStyleIssue(
                        "inline-comment-spacing",
                        "行内注释前应有至少一个空格",
                        i + 1,
                        IssueSeverity.LOW
                    ));
                }
            }
        }
    }
    
    private void analyzeMethodDeclarationStyle(String code, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        Pattern methodPattern = Pattern.compile("(public|private|protected)?\\s*(static)?\\s*\\w+\\s+\\w+\\s*\\([^)]*\\)");
        java.util.regex.Matcher matcher = methodPattern.matcher(code);
        
        while (matcher.find()) {
            String methodDecl = matcher.group();
            
            // 检查方法声明的格式
            if (!methodDecl.contains(" {") && code.substring(matcher.end()).trim().startsWith("{")) {
                suggestions.add(CodeSuggestion.builder()
                    .id(UUID.randomUUID().toString())
                    .type(SuggestionType.STYLE_IMPROVEMENT)
                    .priority(Priority.LOW)
                    .title("方法声明格式")
                    .description("建议在方法声明和左括号之间添加空格")
                    .explanation("一致的格式有助于提高代码可读性")
                    .benefits(Arrays.asList("提高代码可读性"))
                    .estimatedImpact(1)
                    .build());
            }
        }
    }
    
    private QualityMetrics calculateStyleMetrics(String code, List<CodeIssue> issues) {
        int totalLines = code.split("\n").length;
        long styleIssues = issues.size();
        
        // 代码风格评分
        int codeStyle = Math.max(50, 100 - (int)(styleIssues * 2));
        
        // 一致性评分
        int consistency = Math.max(60, 100 - (int)(styleIssues * 3));
        
        return QualityMetrics.builder()
            .codeStyle(codeStyle)
            .readability(consistency)
            .maintainability(consistency)
            .performance(null) // 风格检查不影响性能评分
            .security(null)    // 风格检查不影响安全评分
            .bestPractices(consistency)
            .build();
    }
    
    private CodeIssue createStyleIssue(String ruleId, String description, int lineNumber, IssueSeverity severity) {
        return CodeIssue.builder()
            .id(UUID.randomUUID().toString())
            .type(IssueType.STYLE_VIOLATION)
            .severity(severity)
            .title("代码风格问题")
            .description(description)
            .lineNumber(lineNumber)
            .rule(ruleId)
            .fixSuggestions(generateStyleFixSuggestions(ruleId))
            .build();
    }
    
    private List<String> generateStyleFixSuggestions(String ruleId) {
        Map<String, List<String>> suggestions = new HashMap<>();
        suggestions.put("indentation", Arrays.asList("使用一致的缩进（建议4个空格）", "配置编辑器自动格式化"));
        suggestions.put("brace-style", Arrays.asList("采用K&R括号风格", "使用代码格式化工具"));
        suggestions.put("spacing", Arrays.asList("在操作符前后添加空格", "使用自动格式化"));
        suggestions.put("comma-spacing", Arrays.asList("在逗号后添加空格"));
        suggestions.put("semicolon-spacing", Arrays.asList("删除分号前的空格"));
        
        return suggestions.getOrDefault(ruleId, Arrays.asList("参考代码风格指南修复"));
    }
    
    // 静态辅助方法
    private static boolean checkIndentation(String line) {
        if (line.trim().isEmpty()) return false;
        
        int spaces = 0;
        int tabs = 0;
        
        for (char c : line.toCharArray()) {
            if (c == ' ') spaces++;
            else if (c == '\t') tabs++;
            else break;
        }
        
        return spaces > 0 && tabs > 0; // 混合缩进
    }
    
    private static boolean checkBraceStyle(String line) {
        return line.trim().equals("{");
    }
    
    private static boolean checkSpacing(String line) {
        return line.matches(".*\\S[=+\\-*/]\\S.*") && !line.contains("//");
    }
    
    // 内部类
    private static class StyleRule {
        final String title;
        final String description;
        final IssueSeverity severity;
        final java.util.function.Predicate<String> predicate;
        
        StyleRule(String title, String description, IssueSeverity severity, 
                 java.util.function.Predicate<String> predicate) {
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.predicate = predicate;
        }
    }
    
    public static class StyleCheckResult {
        private final List<CodeIssue> issues;
        private final List<CodeSuggestion> suggestions;
        private final QualityMetrics metrics;
        
        public StyleCheckResult(List<CodeIssue> issues, List<CodeSuggestion> suggestions, QualityMetrics metrics) {
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