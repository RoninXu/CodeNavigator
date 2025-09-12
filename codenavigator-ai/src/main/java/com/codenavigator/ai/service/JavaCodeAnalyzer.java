package com.codenavigator.ai.service;

import com.codenavigator.ai.dto.CodeAnalysisResult;
import com.codenavigator.ai.dto.CodeAnalysisResult.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JavaCodeAnalyzer {
    
    private final JavaParser javaParser = new JavaParser();
    
    public AnalysisResult analyzeJavaCode(String code) {
        log.debug("Analyzing Java code with length: {}", code.length());
        
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(code);
            
            if (!parseResult.isSuccessful()) {
                return handleParseErrors(parseResult);
            }
            
            CompilationUnit cu = parseResult.getResult().orElseThrow();
            
            // 执行各种分析
            List<CodeIssue> issues = new ArrayList<>();
            List<CodeSuggestion> suggestions = new ArrayList<>();
            
            // 分析代码复杂度
            analyzeComplexity(cu, issues, suggestions);
            
            // 分析命名规范
            analyzeNaming(cu, issues, suggestions);
            
            // 分析方法设计
            analyzeMethods(cu, issues, suggestions);
            
            // 分析异常处理
            analyzeExceptionHandling(cu, issues, suggestions);
            
            // 分析设计模式应用
            analyzeDesignPatterns(cu, issues, suggestions);
            
            // 计算质量指标
            QualityMetrics metrics = calculateMetrics(cu, issues);
            
            return new AnalysisResult(issues, suggestions, metrics);
            
        } catch (Exception e) {
            log.error("Error analyzing Java code", e);
            return new AnalysisResult(
                Collections.singletonList(createParseErrorIssue(e)),
                Collections.emptyList(),
                createDefaultMetrics()
            );
        }
    }
    
    private void analyzeComplexity(CompilationUnit cu, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration method, Void arg) {
                super.visit(method, arg);
                
                // 计算圈复杂度
                int complexity = calculateCyclomaticComplexity(method);
                if (complexity > 10) {
                    issues.add(CodeIssue.builder()
                        .id(UUID.randomUUID().toString())
                        .type(IssueType.CODE_SMELL)
                        .severity(complexity > 15 ? IssueSeverity.HIGH : IssueSeverity.MEDIUM)
                        .title("方法复杂度过高")
                        .description(String.format("方法 '%s' 的圈复杂度为 %d，建议重构以降低复杂度", 
                                   method.getName(), complexity))
                        .lineNumber(method.getBegin().map(pos -> pos.line).orElse(null))
                        .rule("complexity-limit")
                        .fixSuggestions(Arrays.asList(
                            "将复杂逻辑提取到单独的方法中",
                            "使用策略模式替换复杂的条件判断",
                            "考虑使用多态替换条件语句"
                        ))
                        .build());
                        
                    suggestions.add(CodeSuggestion.builder()
                        .id(UUID.randomUUID().toString())
                        .type(SuggestionType.REFACTOR)
                        .priority(complexity > 15 ? Priority.HIGH : Priority.MEDIUM)
                        .title("重构复杂方法")
                        .description("建议将复杂的方法拆分为更小、更专注的方法")
                        .explanation("降低方法复杂度可以提高代码的可读性和可维护性")
                        .benefits(Arrays.asList("提高代码可读性", "降低测试难度", "减少bug概率"))
                        .estimatedImpact(complexity > 15 ? 9 : 6)
                        .build());
                }
                
                // 检查方法长度
                int lineCount = method.getEnd().map(pos -> pos.line).orElse(0) - 
                               method.getBegin().map(pos -> pos.line).orElse(0);
                if (lineCount > 50) {
                    issues.add(CodeIssue.builder()
                        .id(UUID.randomUUID().toString())
                        .type(IssueType.CODE_SMELL)
                        .severity(IssueSeverity.MEDIUM)
                        .title("方法过长")
                        .description(String.format("方法 '%s' 有 %d 行，建议控制在 50 行以内", 
                                   method.getName(), lineCount))
                        .lineNumber(method.getBegin().map(pos -> pos.line).orElse(null))
                        .rule("method-length-limit")
                        .fixSuggestions(Arrays.asList("将方法拆分为多个较小的方法"))
                        .build());
                }
            }
        }, null);
    }
    
    private void analyzeNaming(CompilationUnit cu, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration clazz, Void arg) {
                super.visit(clazz, arg);
                
                String className = clazz.getName().asString();
                
                // 检查类名是否符合驼峰命名
                if (!isValidClassName(className)) {
                    issues.add(CodeIssue.builder()
                        .id(UUID.randomUUID().toString())
                        .type(IssueType.STYLE_VIOLATION)
                        .severity(IssueSeverity.LOW)
                        .title("类名不符合命名规范")
                        .description(String.format("类名 '%s' 应使用大驼峰命名法", className))
                        .lineNumber(clazz.getBegin().map(pos -> pos.line).orElse(null))
                        .rule("class-naming-convention")
                        .fixSuggestions(Arrays.asList("使用大驼峰命名法重命名类"))
                        .build());
                }
                
                // 检查类名是否有意义
                if (isGenericName(className)) {
                    suggestions.add(CodeSuggestion.builder()
                        .id(UUID.randomUUID().toString())
                        .type(SuggestionType.STYLE_IMPROVEMENT)
                        .priority(Priority.MEDIUM)
                        .title("使用更有意义的类名")
                        .description(String.format("类名 '%s' 过于通用，建议使用更具体的名称", className))
                        .explanation("有意义的类名能够更好地表达类的职责和用途")
                        .benefits(Arrays.asList("提高代码可读性", "便于理解类的用途"))
                        .estimatedImpact(5)
                        .build());
                }
            }
            
            @Override
            public void visit(MethodDeclaration method, Void arg) {
                super.visit(method, arg);
                
                String methodName = method.getName().asString();
                
                // 检查方法名命名规范
                if (!isValidMethodName(methodName)) {
                    issues.add(CodeIssue.builder()
                        .id(UUID.randomUUID().toString())
                        .type(IssueType.STYLE_VIOLATION)
                        .severity(IssueSeverity.LOW)
                        .title("方法名不符合命名规范")
                        .description(String.format("方法名 '%s' 应使用小驼峰命名法", methodName))
                        .lineNumber(method.getBegin().map(pos -> pos.line).orElse(null))
                        .rule("method-naming-convention")
                        .fixSuggestions(Arrays.asList("使用小驼峰命名法重命名方法"))
                        .build());
                }
            }
            
            @Override
            public void visit(VariableDeclarator variable, Void arg) {
                super.visit(variable, arg);
                
                String varName = variable.getName().asString();
                
                // 检查变量名命名规范
                if (!isValidVariableName(varName)) {
                    issues.add(CodeIssue.builder()
                        .id(UUID.randomUUID().toString())
                        .type(IssueType.STYLE_VIOLATION)
                        .severity(IssueSeverity.LOW)
                        .title("变量名不符合命名规范")
                        .description(String.format("变量名 '%s' 应使用小驼峰命名法", varName))
                        .lineNumber(variable.getBegin().map(pos -> pos.line).orElse(null))
                        .rule("variable-naming-convention")
                        .fixSuggestions(Arrays.asList("使用小驼峰命名法重命名变量"))
                        .build());
                }
            }
        }, null);
    }
    
    private void analyzeMethods(CompilationUnit cu, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration method, Void arg) {
                super.visit(method, arg);
                
                // 检查参数数量
                int paramCount = method.getParameters().size();
                if (paramCount > 5) {
                    issues.add(CodeIssue.builder()
                        .id(UUID.randomUUID().toString())
                        .type(IssueType.CODE_SMELL)
                        .severity(IssueSeverity.MEDIUM)
                        .title("方法参数过多")
                        .description(String.format("方法 '%s' 有 %d 个参数，建议控制在 5 个以内", 
                                   method.getName(), paramCount))
                        .lineNumber(method.getBegin().map(pos -> pos.line).orElse(null))
                        .rule("parameter-limit")
                        .fixSuggestions(Arrays.asList(
                            "将相关参数封装为对象",
                            "使用Builder模式简化参数传递"
                        ))
                        .build());
                        
                    suggestions.add(CodeSuggestion.builder()
                        .id(UUID.randomUUID().toString())
                        .type(SuggestionType.REFACTOR)
                        .priority(Priority.MEDIUM)
                        .title("重构方法参数")
                        .description("考虑使用参数对象模式减少参数数量")
                        .explanation("过多的参数会降低方法的可读性和可维护性")
                        .benefits(Arrays.asList("提高方法可读性", "减少参数传递错误"))
                        .estimatedImpact(6)
                        .build());
                }
                
                // 检查是否有返回值但缺少文档注释
                if (method.getType().toString().equals("void") && !hasJavadocComment(method)) {
                    suggestions.add(CodeSuggestion.builder()
                        .id(UUID.randomUUID().toString())
                        .type(SuggestionType.BEST_PRACTICE)
                        .priority(Priority.LOW)
                        .title("添加方法文档注释")
                        .description("建议为公共方法添加Javadoc注释")
                        .explanation("良好的文档注释有助于其他开发者理解方法的用途")
                        .benefits(Arrays.asList("提高代码可维护性", "便于API文档生成"))
                        .estimatedImpact(3)
                        .build());
                }
            }
        }, null);
    }
    
    private void analyzeExceptionHandling(CompilationUnit cu, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(TryStmt tryStmt, Void arg) {
                super.visit(tryStmt, arg);
                
                // 检查空catch块
                for (CatchClause catchClause : tryStmt.getCatchClauses()) {
                    if (catchClause.getBody().getStatements().isEmpty()) {
                        issues.add(CodeIssue.builder()
                            .id(UUID.randomUUID().toString())
                            .type(IssueType.BEST_PRACTICE_VIOLATION)
                            .severity(IssueSeverity.HIGH)
                            .title("空的catch块")
                            .description("catch块不应为空，至少应记录异常信息")
                            .lineNumber(catchClause.getBegin().map(pos -> pos.line).orElse(null))
                            .rule("no-empty-catch")
                            .fixSuggestions(Arrays.asList(
                                "记录异常日志",
                                "抛出自定义异常",
                                "提供适当的错误处理逻辑"
                            ))
                            .build());
                    }
                }
            }
            
            @Override
            public void visit(ThrowStmt throwStmt, Void arg) {
                super.visit(throwStmt, arg);
                
                // 检查是否抛出通用异常
                if (throwStmt.getExpression() instanceof ObjectCreationExpr) {
                    ObjectCreationExpr objCreation = (ObjectCreationExpr) throwStmt.getExpression();
                    String exceptionType = objCreation.getType().asString();
                    
                    if ("Exception".equals(exceptionType) || "RuntimeException".equals(exceptionType)) {
                        suggestions.add(CodeSuggestion.builder()
                            .id(UUID.randomUUID().toString())
                            .type(SuggestionType.BEST_PRACTICE)
                            .priority(Priority.MEDIUM)
                            .title("使用具体的异常类型")
                            .description(String.format("建议使用更具体的异常类型而不是 %s", exceptionType))
                            .explanation("具体的异常类型能够更好地表达错误的性质")
                            .benefits(Arrays.asList("提高异常处理的精确性", "便于调试和维护"))
                            .estimatedImpact(4)
                            .build());
                    }
                }
            }
        }, null);
    }
    
    private void analyzeDesignPatterns(CompilationUnit cu, List<CodeIssue> issues, List<CodeSuggestion> suggestions) {
        // 检测可能的设计模式应用机会
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration clazz, Void arg) {
                super.visit(clazz, arg);
                
                // 检测可能适合单例模式的类
                if (shouldConsiderSingleton(clazz)) {
                    suggestions.add(CodeSuggestion.builder()
                        .id(UUID.randomUUID().toString())
                        .type(SuggestionType.ALTERNATIVE_APPROACH)
                        .priority(Priority.LOW)
                        .title("考虑使用单例模式")
                        .description("该类可能适合实现为单例模式")
                        .explanation("如果类只需要一个实例，单例模式能够节省内存并确保实例唯一性")
                        .benefits(Arrays.asList("节省内存", "确保实例唯一性", "提供全局访问点"))
                        .estimatedImpact(5)
                        .build());
                }
                
                // 检测可能适合工厂模式的情况
                if (shouldConsiderFactory(clazz)) {
                    suggestions.add(CodeSuggestion.builder()
                        .id(UUID.randomUUID().toString())
                        .type(SuggestionType.ALTERNATIVE_APPROACH)
                        .priority(Priority.MEDIUM)
                        .title("考虑使用工厂模式")
                        .description("该类的创建逻辑复杂，建议使用工厂模式")
                        .explanation("工厂模式可以隐藏复杂的创建逻辑，提高代码的可维护性")
                        .benefits(Arrays.asList("隐藏创建细节", "便于扩展", "降低耦合度"))
                        .estimatedImpact(7)
                        .build());
                }
            }
        }, null);
    }
    
    private QualityMetrics calculateMetrics(CompilationUnit cu, List<CodeIssue> issues) {
        // 基于分析结果计算质量指标
        int styleScore = calculateStyleScore(issues);
        int readabilityScore = calculateReadabilityScore(cu, issues);
        int maintainabilityScore = calculateMaintainabilityScore(cu, issues);
        int performanceScore = 85; // 默认值，实际应基于性能分析
        int securityScore = calculateSecurityScore(issues);
        int bestPracticesScore = calculateBestPracticesScore(issues);
        
        return QualityMetrics.builder()
            .codeStyle(styleScore)
            .readability(readabilityScore)
            .maintainability(maintainabilityScore)
            .performance(performanceScore)
            .security(securityScore)
            .bestPractices(bestPracticesScore)
            .build();
    }
    
    // 辅助方法
    private int calculateCyclomaticComplexity(MethodDeclaration method) {
        // 简化的圈复杂度计算
        ComplexityVisitor visitor = new ComplexityVisitor();
        method.accept(visitor, null);
        return visitor.getComplexity();
    }
    
    private boolean isValidClassName(String name) {
        return name.matches("^[A-Z][a-zA-Z0-9]*$");
    }
    
    private boolean isValidMethodName(String name) {
        return name.matches("^[a-z][a-zA-Z0-9]*$");
    }
    
    private boolean isValidVariableName(String name) {
        return name.matches("^[a-z][a-zA-Z0-9]*$|^[A-Z][A-Z0-9_]*$"); // 小驼峰或常量
    }
    
    private boolean isGenericName(String name) {
        String[] genericNames = {"Test", "Data", "Info", "Manager", "Handler", "Util", "Helper"};
        return Arrays.stream(genericNames).anyMatch(name::contains);
    }
    
    private boolean hasJavadocComment(MethodDeclaration method) {
        return method.getJavadocComment().isPresent();
    }
    
    private boolean shouldConsiderSingleton(ClassOrInterfaceDeclaration clazz) {
        // 简化判断逻辑
        return clazz.getName().asString().toLowerCase().contains("manager") ||
               clazz.getName().asString().toLowerCase().contains("config");
    }
    
    private boolean shouldConsiderFactory(ClassOrInterfaceDeclaration clazz) {
        // 检查是否有复杂的构造逻辑
        return clazz.getConstructors().stream()
            .anyMatch(c -> c.getBody().getStatements().size() > 5);
    }
    
    private int calculateStyleScore(List<CodeIssue> issues) {
        long styleViolations = issues.stream()
            .filter(issue -> issue.getType() == IssueType.STYLE_VIOLATION)
            .count();
        return Math.max(0, 100 - (int)(styleViolations * 5));
    }
    
    private int calculateReadabilityScore(CompilationUnit cu, List<CodeIssue> issues) {
        // 基于方法复杂度、命名质量等计算
        long complexityIssues = issues.stream()
            .filter(issue -> issue.getDescription().contains("复杂度"))
            .count();
        return Math.max(60, 100 - (int)(complexityIssues * 10));
    }
    
    private int calculateMaintainabilityScore(CompilationUnit cu, List<CodeIssue> issues) {
        long maintainabilityIssues = issues.stream()
            .filter(issue -> issue.getType() == IssueType.CODE_SMELL)
            .count();
        return Math.max(50, 100 - (int)(maintainabilityIssues * 8));
    }
    
    private int calculateSecurityScore(List<CodeIssue> issues) {
        long securityIssues = issues.stream()
            .filter(issue -> issue.getType() == IssueType.SECURITY_VULNERABILITY)
            .count();
        return Math.max(70, 100 - (int)(securityIssues * 20));
    }
    
    private int calculateBestPracticesScore(List<CodeIssue> issues) {
        long bestPracticeViolations = issues.stream()
            .filter(issue -> issue.getType() == IssueType.BEST_PRACTICE_VIOLATION)
            .count();
        return Math.max(60, 100 - (int)(bestPracticeViolations * 15));
    }
    
    private AnalysisResult handleParseErrors(ParseResult<CompilationUnit> parseResult) {
        List<CodeIssue> issues = new ArrayList<>();
        for (var problem : parseResult.getProblems()) {
            CodeIssue issue = CodeIssue.builder()
                .id(UUID.randomUUID().toString())
                .type(IssueType.SYNTAX_ERROR)
                .severity(IssueSeverity.CRITICAL)
                .title("语法错误")
                .description(problem.getMessage())
                .lineNumber(1) // 默认行号，JavaParser API变化导致无法获取准确行号
                .category("SYNTAX")
                .rule("syntax-check")
                .build();
            issues.add(issue);
        }
            
        return new AnalysisResult(issues, Collections.emptyList(), createDefaultMetrics());
    }
    
    private CodeIssue createParseErrorIssue(Exception e) {
        return CodeIssue.builder()
            .id(UUID.randomUUID().toString())
            .type(IssueType.SYNTAX_ERROR)
            .severity(IssueSeverity.CRITICAL)
            .title("代码解析失败")
            .description("无法解析Java代码: " + e.getMessage())
            .rule("syntax-check")
            .build();
    }
    
    private QualityMetrics createDefaultMetrics() {
        return QualityMetrics.builder()
            .codeStyle(50)
            .readability(50)
            .maintainability(50)
            .performance(50)
            .security(50)
            .bestPractices(50)
            .build();
    }
    
    // 内部类
    public static class AnalysisResult {
        private final List<CodeIssue> issues;
        private final List<CodeSuggestion> suggestions;
        private final QualityMetrics metrics;
        
        public AnalysisResult(List<CodeIssue> issues, List<CodeSuggestion> suggestions, QualityMetrics metrics) {
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
    
    // 复杂度计算访问者
    private static class ComplexityVisitor extends VoidVisitorAdapter<Void> {
        private int complexity = 1; // 基础复杂度为1
        
        @Override
        public void visit(IfStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }
        
        @Override
        public void visit(WhileStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }
        
        @Override
        public void visit(ForStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }
        
        @Override
        public void visit(SwitchStmt n, Void arg) {
            complexity += n.getEntries().size();
            super.visit(n, arg);
        }
        
        @Override
        public void visit(ConditionalExpr n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }
        
        public int getComplexity() {
            return complexity;
        }
    }
}