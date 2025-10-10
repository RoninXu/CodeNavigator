package com.codenavigator.core.repository;

import com.codenavigator.core.entity.User;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository单元测试
 * 使用@DataJpaTest进行JPA Repository层测试
 * 自动配置内存数据库H2
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // 清理数据
        userRepository.deleteAll();

        // 创建测试用户1 - 初学者
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setEmail("testuser1@example.com");
        testUser1.setLevel(UserLevel.BEGINNER);
        testUser1.setCreatedAt(LocalDateTime.now().minusDays(30));
        testUser1.setUpdatedAt(LocalDateTime.now());

        // 创建测试用户2 - 中级
        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setEmail("testuser2@example.com");
        testUser2.setLevel(UserLevel.INTERMEDIATE);
        testUser2.setCreatedAt(LocalDateTime.now().minusDays(5));
        testUser2.setUpdatedAt(LocalDateTime.now());

        // 创建测试用户3 - 高级
        testUser3 = new User();
        testUser3.setUsername("testuser3");
        testUser3.setEmail("testuser3@example.com");
        testUser3.setLevel(UserLevel.ADVANCED);
        testUser3.setCreatedAt(LocalDateTime.now().minusDays(1));
        testUser3.setUpdatedAt(LocalDateTime.now());

        // 持久化测试数据
        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.persist(testUser3);
        entityManager.flush();
    }

    @Test
    void testFindByUsername_Success() {
        // When
        Optional<User> result = userRepository.findByUsername("testuser1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser1");
        assertThat(result.get().getEmail()).isEqualTo("testuser1@example.com");
        assertThat(result.get().getLevel()).isEqualTo(UserLevel.BEGINNER);
    }

    @Test
    void testFindByUsername_NotFound() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByEmail_Success() {
        // When
        Optional<User> result = userRepository.findByEmail("testuser2@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser2");
        assertThat(result.get().getLevel()).isEqualTo(UserLevel.INTERMEDIATE);
    }

    @Test
    void testExistsByUsername_True() {
        // When
        boolean exists = userRepository.existsByUsername("testuser1");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByUsername_False() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testExistsByEmail_True() {
        // When
        boolean exists = userRepository.existsByEmail("testuser1@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByEmail_False() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindByLevel_WithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByLevel(UserLevel.BEGINNER, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser1");
    }

    @Test
    void testFindBasicInfoById_Success() {
        // When
        Optional<User> result = userRepository.findBasicInfoById(testUser1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser1");
    }

    @Test
    void testFindRecentUsers() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);

        // When
        List<User> result = userRepository.findRecentUsers(fromDate);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSizeGreaterThanOrEqualTo(2); // 至少包含testuser2和testuser3
        assertThat(result).extracting(User::getUsername)
                .contains("testuser2", "testuser3");
    }

    @Test
    void testSearchUsers_ByUsername() {
        // Given
        String keyword = "user2";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.searchUsers(keyword, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser2");
    }

    @Test
    void testSearchUsers_ByEmail() {
        // Given
        String keyword = "user3@example";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.searchUsers(keyword, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser3");
    }

    @Test
    void testGetUserLevelDistribution() {
        // When
        List<Object[]> result = userRepository.getUserLevelDistribution();

        // Then
        assertThat(result).hasSize(3);

        // 验证每个等级都有正确的用户数
        for (Object[] row : result) {
            UserLevel level = (UserLevel) row[0];
            Long count = (Long) row[1];

            assertThat(count).isEqualTo(1L);
            assertThat(level).isIn(UserLevel.BEGINNER, UserLevel.INTERMEDIATE, UserLevel.ADVANCED);
        }
    }

    @Test
    void testUpdateUserLevel() {
        // When
        int updatedCount = userRepository.updateUserLevel(UserLevel.BEGINNER, UserLevel.INTERMEDIATE);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedCount).isEqualTo(1);

        // 验证更新是否成功
        Optional<User> updatedUser = userRepository.findByUsername("testuser1");
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().getLevel()).isEqualTo(UserLevel.INTERMEDIATE);
    }

    @Test
    void testSaveAndFindUser() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setLevel(UserLevel.BEGINNER);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        // When
        User savedUser = userRepository.save(newUser);
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("newuser");
    }

    @Test
    void testDeleteUser() {
        // Given
        Long userId = testUser1.getId();

        // When
        userRepository.deleteById(userId);
        Optional<User> deletedUser = userRepository.findById(userId);

        // Then
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void testCountUsers() {
        // When
        long count = userRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testFindAll() {
        // When
        List<User> allUsers = userRepository.findAll();

        // Then
        assertThat(allUsers).hasSize(3);
        assertThat(allUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser1", "testuser2", "testuser3");
    }
}
