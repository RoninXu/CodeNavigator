package com.codenavigator.ai.service;

import com.codenavigator.ai.dto.CodeAnalysisRequest;
import com.codenavigator.ai.dto.CodeAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * CodeAnalyzer简化单元测试
 * 使用Spring集成测试，重点测试核心分析流程
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("CodeAnalyzer简化单元测试")
class CodeAnalyzerSimpleTest {

    @Autowired
    private CodeAnalyzer codeAnalyzer;

    private String sampleJavaCode;
    private CodeAnalysisRequest testRequest;

    @BeforeEach
    void setUp() {
        sampleJavaCode = """
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            """;

        testRequest = CodeAnalysisRequest.builder()
            .userId("1")
            .moduleId("100")
            .code(sampleJavaCode)
            .language("java")
            .fileName("HelloWorld.java")
            .analysisType(CodeAnalysisRequest.AnalysisType.QUALITY_ASSESSMENT)
            .build();
    }

    // ========== 基础功能测试 ==========

    @Test
    @DisplayName("综合分析 - 应返回完整的分析结果")
    void testAnalyzeCode_Comprehensive_ReturnsCompleteResult() {
        // When
        CodeAnalysisResult result = codeAnalyzer.analyzeCode(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAnalysisId()).isNotNull().startsWith("CA-");
        assertThat(result.getUserId()).isEqualTo("1");
        assertThat(result.getModuleId()).isEqualTo("100");
        assertThat(result.getOverallScore()).isBetween(0, 100);
        assertThat(result.getQualityLevel()).isNotNull();
        assertThat(result.getMetrics()).isNotNull();
        assertThat(result.getSummary()).isNotEmpty();
        assertThat(result.getAnalysisTime()).isNotNull();
    }

    @Test
    @DisplayName("快速分析 - 使用默认配置")
    void testQuickAnalyze_UsesDefaultConfiguration() {
        // When
        CodeAnalysisResult result = codeAnalyzer.quickAnalyze(sampleJavaCode, "java");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOverallScore()).isBetween(0, 100);
    }

    @Test
    @DisplayName("仅质量分析 - 返回质量评分")
    void testAnalyzeCode_QualityOnly() {
        // Given
        testRequest.setAnalysisType(CodeAnalysisRequest.AnalysisType.QUALITY_ASSESSMENT);

        // When
        CodeAnalysisResult result = codeAnalyzer.analyzeCode(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOverallScore()).isGreaterThanOrEqualTo(0);
    }

    // ========== 验证测试 ==========

    @Test
    @DisplayName("空代码 - 应抛出异常")
    void testValidation_EmptyCode_ThrowsException() {
        // Given
        testRequest.setCode("");

        // When & Then
        assertThatThrownBy(() -> codeAnalyzer.analyzeCode(testRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("代码内容不能为空");
    }

    @Test
    @DisplayName("null代码 - 应抛出异常")
    void testValidation_NullCode_ThrowsException() {
        // Given
        testRequest.setCode(null);

        // When & Then
        assertThatThrownBy(() -> codeAnalyzer.analyzeCode(testRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("代码内容不能为空");
    }

    @Test
    @DisplayName("元数据构建 - 应包含代码统计信息")
    void testMetadataBuilding_IncludesCodeStatistics() {
        // When
        CodeAnalysisResult result = codeAnalyzer.analyzeCode(testRequest);

        // Then
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata())
            .containsKeys("language", "analysisType", "codeLength", "lineCount");
        assertThat(result.getMetadata().get("fileName")).isEqualTo("HelloWorld.java");
    }

    @Test
    @DisplayName("简单代码 - 评分应较高")
    void testScoreCalculation_SimpleCode_HighScore() {
        // When
        CodeAnalysisResult result = codeAnalyzer.analyzeCode(testRequest);

        // Then
        assertThat(result.getOverallScore()).isGreaterThanOrEqualTo(70);
        assertThat(result.getQualityLevel()).isIn(
            CodeAnalysisResult.QualityLevel.EXCELLENT,
            CodeAnalysisResult.QualityLevel.GOOD,
            CodeAnalysisResult.QualityLevel.AVERAGE
        );
    }

    @Test
    @DisplayName("复杂代码 - 有多个分析维度")
    void testComplexCodeAnalysis() {
        // Given
        String complexCode = """
            public class Calculator {
                public int add(int a, int b) {
                    return a + b;
                }

                public int subtract(int a, int b) {
                    return a - b;
                }

                public int multiply(int a, int b) {
                    return a * b;
                }
            }
            """;
        testRequest.setCode(complexCode);

        // When
        CodeAnalysisResult result = codeAnalyzer.analyzeCode(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMetrics().getCodeStyle()).isNotNull();
        assertThat(result.getMetrics().getReadability()).isNotNull();
        assertThat(result.getMetrics().getMaintainability()).isNotNull();
    }
}
