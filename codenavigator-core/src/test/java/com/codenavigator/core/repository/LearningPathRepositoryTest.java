package com.codenavigator.core.repository;

import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.common.enums.DifficultyLevel;
import com.codenavigator.common.enums.UserLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LearningPathRepository单元测试
 * 测试学习路径Repository的各种查询方法
 */
@DataJpaTest
@ActiveProfiles("test")
class LearningPathRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LearningPathRepository learningPathRepository;

    private LearningPath beginnerPath;
    private LearningPath intermediatePath;
    private LearningPath advancedPath;

    @BeforeEach
    void setUp() {
        // 清理数据
        learningPathRepository.deleteAll();

        // 创建初学者路径
        beginnerPath = new LearningPath();
        beginnerPath.setId(UUID.randomUUID().toString());
        beginnerPath.setTitle("Spring Boot Basics");
        beginnerPath.setDescription("Learn Spring Boot fundamentals");
        beginnerPath.setFramework("Spring Boot");
        beginnerPath.setDifficulty(DifficultyLevel.BEGINNER);
        beginnerPath.setTargetLevel(UserLevel.BEGINNER);
        beginnerPath.setTags("java,spring,beginner");
        beginnerPath.setIsActive(true);
        beginnerPath.setCompletionCount(100);
        beginnerPath.setAverageRating(4.5);
        beginnerPath.setCreatedAt(LocalDateTime.now().minusDays(30));
        beginnerPath.setUpdatedAt(LocalDateTime.now());

        // 创建中级路径
        intermediatePath = new LearningPath();
        intermediatePath.setId(UUID.randomUUID().toString());
        intermediatePath.setTitle("Spring Boot Advanced");
        intermediatePath.setDescription("Advanced Spring Boot features");
        intermediatePath.setFramework("Spring Boot");
        intermediatePath.setDifficulty(DifficultyLevel.INTERMEDIATE);
        intermediatePath.setTargetLevel(UserLevel.INTERMEDIATE);
        intermediatePath.setTags("java,spring,advanced");
        intermediatePath.setIsActive(true);
        intermediatePath.setCompletionCount(50);
        intermediatePath.setAverageRating(4.7);
        intermediatePath.setCreatedAt(LocalDateTime.now().minusDays(15));
        intermediatePath.setUpdatedAt(LocalDateTime.now());

        // 创建高级路径
        advancedPath = new LearningPath();
        advancedPath.setId(UUID.randomUUID().toString());
        advancedPath.setTitle("Microservices with Spring Cloud");
        advancedPath.setDescription("Build microservices architecture");
        advancedPath.setFramework("Spring Cloud");
        advancedPath.setDifficulty(DifficultyLevel.ADVANCED);
        advancedPath.setTargetLevel(UserLevel.ADVANCED);
        advancedPath.setTags("java,microservices,cloud");
        advancedPath.setIsActive(true);
        advancedPath.setCompletionCount(30);
        advancedPath.setAverageRating(4.8);
        advancedPath.setCreatedAt(LocalDateTime.now().minusDays(7));
        advancedPath.setUpdatedAt(LocalDateTime.now());

        // 持久化测试数据
        entityManager.persist(beginnerPath);
        entityManager.persist(intermediatePath);
        entityManager.persist(advancedPath);
        entityManager.flush();
    }

    @Test
    void testFindByIsActiveTrueOrderByCompletionCountDesc() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<LearningPath> result = learningPathRepository.findByIsActiveTrueOrderByCompletionCountDesc(pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getCompletionCount()).isEqualTo(100);
        assertThat(result.getContent().get(1).getCompletionCount()).isEqualTo(50);
        assertThat(result.getContent().get(2).getCompletionCount()).isEqualTo(30);
    }

    @Test
    void testFindByFrameworkAndIsActiveTrueOrderByAverageRatingDesc() {
        // Given
        String framework = "Spring Boot";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<LearningPath> result = learningPathRepository
                .findByFrameworkAndIsActiveTrueOrderByAverageRatingDesc(framework, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getAverageRating()).isEqualTo(4.7);
        assertThat(result.getContent().get(1).getAverageRating()).isEqualTo(4.5);
    }

    @Test
    void testFindByDifficultyAndIsActiveTrueOrderByCreatedAtDesc() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<LearningPath> result = learningPathRepository
                .findByDifficultyAndIsActiveTrueOrderByCreatedAtDesc(DifficultyLevel.BEGINNER, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring Boot Basics");
    }

    @Test
    void testFindByTargetLevelAndIsActiveTrueOrderByCompletionCountDesc() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<LearningPath> result = learningPathRepository
                .findByTargetLevelAndIsActiveTrueOrderByCompletionCountDesc(UserLevel.INTERMEDIATE, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring Boot Advanced");
    }

    @Test
    void testSearchPaths_ByTitle() {
        // Given
        String keyword = "Spring Boot";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<LearningPath> result = learningPathRepository.searchPaths(keyword, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(LearningPath::getTitle)
                .contains("Spring Boot Basics", "Spring Boot Advanced");
    }

    @Test
    void testSearchPaths_ByDescription() {
        // Given
        String keyword = "microservices";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<LearningPath> result = learningPathRepository.searchPaths(keyword, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Microservices with Spring Cloud");
    }

    @Test
    void testSearchPaths_ByTags() {
        // Given
        String keyword = "beginner";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<LearningPath> result = learningPathRepository.searchPaths(keyword, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring Boot Basics");
    }

    @Test
    void testFindPopularPaths() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        List<LearningPath> result = learningPathRepository.findPopularPaths(pageable);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCompletionCount()).isGreaterThanOrEqualTo(result.get(1).getCompletionCount());
    }

    @Test
    void testFindRecommendedPaths() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<LearningPath> result = learningPathRepository
                .findRecommendedPaths(UserLevel.INTERMEDIATE, pageable);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getAverageRating()).isGreaterThanOrEqualTo(4.0);
    }

    @Test
    void testFindRecentPaths() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(10);

        // When
        List<LearningPath> result = learningPathRepository.findRecentPaths(fromDate);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSizeGreaterThanOrEqualTo(1);
        assertThat(result).extracting(LearningPath::getTitle)
                .contains("Microservices with Spring Cloud");
    }

    @Test
    void testFindByTag() {
        // Given
        String tag = "spring";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<LearningPath> result = learningPathRepository.findByTag(tag, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void testIncrementCompletionCount() {
        // Given
        String pathId = beginnerPath.getId();
        int originalCount = beginnerPath.getCompletionCount();

        // When
        int updatedCount = learningPathRepository.incrementCompletionCount(pathId);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedCount).isEqualTo(1);

        Optional<LearningPath> updatedPath = learningPathRepository.findById(pathId);
        assertThat(updatedPath).isPresent();
        assertThat(updatedPath.get().getCompletionCount()).isEqualTo(originalCount + 1);
    }

    @Test
    void testUpdateAverageRating() {
        // Given
        String pathId = beginnerPath.getId();
        Double newRating = 4.9;

        // When
        int updatedCount = learningPathRepository.updateAverageRating(pathId, newRating);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedCount).isEqualTo(1);

        Optional<LearningPath> updatedPath = learningPathRepository.findById(pathId);
        assertThat(updatedPath).isPresent();
        assertThat(updatedPath.get().getAverageRating()).isEqualTo(newRating);
    }

    @Test
    void testGetFrameworkDistribution() {
        // When
        List<Object[]> result = learningPathRepository.getFrameworkDistribution();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.size()).isGreaterThanOrEqualTo(2);

        // Spring Boot应该有2个路径
        Object[] springBootRow = result.stream()
                .filter(row -> "Spring Boot".equals(row[0]))
                .findFirst()
                .orElse(null);

        assertThat(springBootRow).isNotNull();
        assertThat(springBootRow[1]).isEqualTo(2L);
    }

    @Test
    void testFindHighRatedPaths() {
        // Given
        Double minRating = 4.6;
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<LearningPath> result = learningPathRepository.findHighRatedPaths(minRating, pageable);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(path -> path.getAverageRating() >= minRating);
        assertThat(result.get(0).getAverageRating()).isGreaterThanOrEqualTo(result.get(1).getAverageRating());
    }

    @Test
    void testSaveAndFindLearningPath() {
        // Given
        LearningPath newPath = new LearningPath();
        newPath.setId(UUID.randomUUID().toString());
        newPath.setTitle("New Path");
        newPath.setDescription("New Description");
        newPath.setFramework("React");
        newPath.setDifficulty(DifficultyLevel.BEGINNER);
        newPath.setTargetLevel(UserLevel.BEGINNER);
        newPath.setTags("javascript,react");
        newPath.setIsActive(true);
        newPath.setCompletionCount(0);
        newPath.setAverageRating(0.0);
        newPath.setCreatedAt(LocalDateTime.now());
        newPath.setUpdatedAt(LocalDateTime.now());

        // When
        LearningPath savedPath = learningPathRepository.save(newPath);
        Optional<LearningPath> foundPath = learningPathRepository.findById(savedPath.getId());

        // Then
        assertThat(savedPath.getId()).isNotNull();
        assertThat(foundPath).isPresent();
        assertThat(foundPath.get().getTitle()).isEqualTo("New Path");
    }

    @Test
    void testDeleteLearningPath() {
        // Given
        String pathId = beginnerPath.getId();

        // When
        learningPathRepository.deleteById(pathId);
        Optional<LearningPath> deletedPath = learningPathRepository.findById(pathId);

        // Then
        assertThat(deletedPath).isEmpty();
    }

    @Test
    void testCountLearningPaths() {
        // When
        long count = learningPathRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testFindAll() {
        // When
        List<LearningPath> allPaths = learningPathRepository.findAll();

        // Then
        assertThat(allPaths).hasSize(3);
        assertThat(allPaths).extracting(LearningPath::getTitle)
                .contains("Spring Boot Basics", "Spring Boot Advanced", "Microservices with Spring Cloud");
    }

    @Test
    void testFindRelatedPaths() {
        // Given
        String excludeId = beginnerPath.getId();
        String framework = "Spring Boot";
        String tag = "spring";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<LearningPath> result = learningPathRepository
                .findRelatedPaths(excludeId, framework, tag, pageable);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).noneMatch(path -> path.getId().equals(excludeId));
    }
}
