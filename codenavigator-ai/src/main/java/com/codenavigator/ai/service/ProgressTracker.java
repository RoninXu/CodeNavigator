package com.codenavigator.ai.service;

import com.codenavigator.core.entity.User;
import com.codenavigator.core.entity.LearningPath;
import com.codenavigator.core.entity.LearningModule;
import com.codenavigator.core.entity.UserProgress;
import com.codenavigator.common.enums.ProgressStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressTracker {
    
    // 这里应该注入相应的Repository，暂时省略
    // private final UserProgressRepository userProgressRepository;
    // private final LearningModuleRepository learningModuleRepository;
    
    @Transactional
    public UserProgress startLearningPath(User user, LearningPath learningPath) {
        log.info("Starting learning path {} for user {}", learningPath.getTitle(), user.getId());
        
        UserProgress progress = new UserProgress(user, learningPath);
        progress.startLearning();
        
        // 设置第一个模块为当前模块
        if (learningPath.getModules() != null && !learningPath.getModules().isEmpty()) {
            progress.setCurrentModule(learningPath.getModules().get(0));
        }
        
        // 初始化模块进度
        initializeModuleProgress(progress, learningPath.getModules());
        
        // 保存进度 (这里应该调用repository)
        // return userProgressRepository.save(progress);
        
        log.info("Learning path started successfully for user {}", user.getId());
        return progress;
    }
    
    @Transactional
    public UserProgress completeModule(String userId, String moduleId) {
        log.info("Completing module {} for user {}", moduleId, userId);
        
        // 获取用户进度 (这里应该从repository获取)
        UserProgress progress = getUserProgress(userId, moduleId);
        if (progress == null) {
            throw new IllegalStateException("No progress found for user and module");
        }
        
        // 更新模块完成状态
        updateModuleCompletion(progress, moduleId);
        
        // 移动到下一个模块
        moveToNextModule(progress);
        
        // 更新总体进度
        progress.updateProgress();
        
        // 保存进度
        // userProgressRepository.save(progress);
        
        log.info("Module {} completed for user {}", moduleId, userId);
        return progress;
    }
    
    public ProgressSummary getProgressSummary(String userId, String learningPathId) {
        log.debug("Getting progress summary for user {} and path {}", userId, learningPathId);
        
        UserProgress progress = getUserProgressByPath(userId, learningPathId);
        if (progress == null) {
            return null;
        }
        
        return ProgressSummary.builder()
            .userId(userId)
            .learningPathId(learningPathId)
            .learningPathTitle(progress.getLearningPath().getTitle())
            .status(progress.getStatus())
            .completionPercentage(progress.getCompletionPercentage())
            .completedModules(progress.getCompletedModules())
            .totalModules(progress.getTotalModules())
            .currentModuleTitle(progress.getCurrentModule() != null ? 
                              progress.getCurrentModule().getTitle() : null)
            .timeSpent(calculateTimeSpent(progress))
            .estimatedTimeRemaining(calculateEstimatedTimeRemaining(progress))
            .startedAt(progress.getStartedAt())
            .lastActiveAt(progress.getLastActiveAt())
            .achievements(calculateAchievements(progress))
            .build();
    }
    
    public List<ModuleProgress> getModuleProgressList(String userId, String learningPathId) {
        log.debug("Getting module progress list for user {} and path {}", userId, learningPathId);
        
        UserProgress userProgress = getUserProgressByPath(userId, learningPathId);
        if (userProgress == null) {
            return new ArrayList<>();
        }
        
        return userProgress.getLearningPath().getModules().stream()
            .map(module -> ModuleProgress.builder()
                .moduleId(module.getId())
                .title(module.getTitle())
                .status(getModuleStatus(userProgress, module.getId()))
                .completedAt(getModuleCompletedAt(userProgress, module.getId()))
                .timeSpent(getModuleTimeSpent(userProgress, module.getId()))
                .difficulty(module.getDifficulty())
                .estimatedHours(module.getEstimatedHours())
                .build())
            .collect(Collectors.toList());
    }
    
    public StudyStreak calculateStudyStreak(String userId) {
        log.debug("Calculating study streak for user {}", userId);
        
        // 获取用户最近的学习记录
        List<UserProgress> recentProgress = getUserRecentProgress(userId, 30);
        
        int currentStreak = 0;
        int maxStreak = 0;
        LocalDateTime lastStudyDate = null;
        
        // 按日期倒序排列
        Map<LocalDateTime, List<UserProgress>> progressByDate = recentProgress.stream()
            .collect(Collectors.groupingBy(
                progress -> progress.getLastActiveAt().truncatedTo(ChronoUnit.DAYS)
            ));
        
        List<LocalDateTime> studyDates = progressByDate.keySet().stream()
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
        
        // 计算连续学习天数
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        for (LocalDateTime date : studyDates) {
            if (lastStudyDate == null) {
                lastStudyDate = date;
                currentStreak = 1;
            } else {
                long daysBetween = ChronoUnit.DAYS.between(date, lastStudyDate);
                if (daysBetween == 1) {
                    currentStreak++;
                } else if (daysBetween > 1) {
                    maxStreak = Math.max(maxStreak, currentStreak);
                    currentStreak = 1;
                }
                lastStudyDate = date;
            }
        }
        
        maxStreak = Math.max(maxStreak, currentStreak);
        
        // 如果最后学习日期不是今天或昨天，则当前连击为0
        if (lastStudyDate != null) {
            long daysSinceLastStudy = ChronoUnit.DAYS.between(lastStudyDate, today);
            if (daysSinceLastStudy > 1) {
                currentStreak = 0;
            }
        }
        
        return StudyStreak.builder()
            .currentStreak(currentStreak)
            .maxStreak(maxStreak)
            .lastStudyDate(lastStudyDate)
            .totalStudyDays(studyDates.size())
            .build();
    }
    
    private void initializeModuleProgress(UserProgress progress, List<LearningModule> modules) {
        if (modules == null || modules.isEmpty()) {
            return;
        }
        
        Map<String, Object> moduleProgressMap = new HashMap<>();
        for (LearningModule module : modules) {
            Map<String, Object> moduleStatus = new HashMap<>();
            moduleStatus.put("status", "NOT_STARTED");
            moduleStatus.put("startedAt", null);
            moduleStatus.put("completedAt", null);
            moduleStatus.put("timeSpent", 0);
            moduleProgressMap.put(module.getId(), moduleStatus);
        }
        
        // 将进度信息序列化为JSON字符串存储
        progress.setModuleProgress(serializeModuleProgress(moduleProgressMap));
    }
    
    private void updateModuleCompletion(UserProgress progress, String moduleId) {
        Map<String, Object> moduleProgressMap = deserializeModuleProgress(progress.getModuleProgress());

        if (moduleProgressMap.containsKey(moduleId)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> moduleStatus = (Map<String, Object>) moduleProgressMap.get(moduleId);
            moduleStatus.put("status", "COMPLETED");
            moduleStatus.put("completedAt", LocalDateTime.now());
            
            progress.setModuleProgress(serializeModuleProgress(moduleProgressMap));
            progress.setCompletedModules(progress.getCompletedModules() + 1);
        }
    }
    
    private void moveToNextModule(UserProgress progress) {
        List<LearningModule> modules = progress.getLearningPath().getModules();
        LearningModule currentModule = progress.getCurrentModule();
        
        if (currentModule != null && modules != null) {
            int currentIndex = findModuleIndex(modules, currentModule.getId());
            if (currentIndex >= 0 && currentIndex < modules.size() - 1) {
                progress.setCurrentModule(modules.get(currentIndex + 1));
            } else {
                progress.setCurrentModule(null); // 所有模块已完成
            }
        }
    }
    
    private int findModuleIndex(List<LearningModule> modules, String moduleId) {
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).getId().equals(moduleId)) {
                return i;
            }
        }
        return -1;
    }
    
    private long calculateTimeSpent(UserProgress progress) {
        if (progress.getStartedAt() == null) {
            return 0;
        }
        
        LocalDateTime endTime = progress.getCompletedAt() != null ? 
                                progress.getCompletedAt() : 
                                progress.getLastActiveAt();
        
        return ChronoUnit.HOURS.between(progress.getStartedAt(), endTime);
    }
    
    private long calculateEstimatedTimeRemaining(UserProgress progress) {
        LearningPath path = progress.getLearningPath();
        if (path.getEstimatedDuration() == null) {
            return 0;
        }
        
        long totalEstimatedHours = path.getEstimatedDuration() * 8; // 转换为小时
        long timeSpent = calculateTimeSpent(progress);
        
        return Math.max(0, totalEstimatedHours - timeSpent);
    }
    
    private List<String> calculateAchievements(UserProgress progress) {
        List<String> achievements = new ArrayList<>();
        
        if (progress.getStatus() == ProgressStatus.COMPLETED) {
            achievements.add("路径完成者");
        }
        
        if (progress.getCompletionPercentage() >= 50) {
            achievements.add("半程英雄");
        }
        
        if (progress.getCompletedModules() >= 10) {
            achievements.add("学习达人");
        }
        
        return achievements;
    }
    
    private ProgressStatus getModuleStatus(UserProgress userProgress, String moduleId) {
        Map<String, Object> moduleProgressMap = deserializeModuleProgress(userProgress.getModuleProgress());
        @SuppressWarnings("unchecked")
        Map<String, Object> moduleStatus = (Map<String, Object>) moduleProgressMap.get(moduleId);
        
        if (moduleStatus != null) {
            String status = (String) moduleStatus.get("status");
            return ProgressStatus.valueOf(status);
        }
        
        return ProgressStatus.NOT_STARTED;
    }
    
    private LocalDateTime getModuleCompletedAt(UserProgress userProgress, String moduleId) {
        Map<String, Object> moduleProgressMap = deserializeModuleProgress(userProgress.getModuleProgress());
        @SuppressWarnings("unchecked")
        Map<String, Object> moduleStatus = (Map<String, Object>) moduleProgressMap.get(moduleId);
        
        if (moduleStatus != null) {
            return (LocalDateTime) moduleStatus.get("completedAt");
        }
        
        return null;
    }
    
    private long getModuleTimeSpent(UserProgress userProgress, String moduleId) {
        Map<String, Object> moduleProgressMap = deserializeModuleProgress(userProgress.getModuleProgress());
        @SuppressWarnings("unchecked")
        Map<String, Object> moduleStatus = (Map<String, Object>) moduleProgressMap.get(moduleId);
        
        if (moduleStatus != null) {
            Integer timeSpent = (Integer) moduleStatus.get("timeSpent");
            return timeSpent != null ? timeSpent.longValue() : 0;
        }
        
        return 0;
    }
    
    // 这些方法在实际实现中应该调用相应的Repository
    private UserProgress getUserProgress(String userId, String moduleId) {
        // 实际实现应该从数据库查询
        return null;
    }
    
    private UserProgress getUserProgressByPath(String userId, String learningPathId) {
        // 实际实现应该从数据库查询
        return null;
    }
    
    private List<UserProgress> getUserRecentProgress(String userId, int days) {
        // 实际实现应该从数据库查询最近几天的学习记录
        return new ArrayList<>();
    }
    
    // JSON序列化辅助方法（实际应该使用Jackson等库）
    private String serializeModuleProgress(Map<String, Object> progressMap) {
        // 简化实现，实际应该使用JSON库
        return progressMap.toString();
    }
    
    private Map<String, Object> deserializeModuleProgress(String progressJson) {
        // 简化实现，实际应该使用JSON库解析
        return new HashMap<>();
    }
    
    // 内部数据类
    public static class ProgressSummary {
        private String userId;
        private String learningPathId;
        private String learningPathTitle;
        private ProgressStatus status;
        private Double completionPercentage;
        private Integer completedModules;
        private Integer totalModules;
        private String currentModuleTitle;
        private Long timeSpent;
        private Long estimatedTimeRemaining;
        private LocalDateTime startedAt;
        private LocalDateTime lastActiveAt;
        private List<String> achievements;
        
        public static ProgressSummaryBuilder builder() {
            return new ProgressSummaryBuilder();
        }
        
        // Builder pattern implementation...
        public static class ProgressSummaryBuilder {
            private String userId;
            private String learningPathId;
            private String learningPathTitle;
            private ProgressStatus status;
            private Double completionPercentage;
            private Integer completedModules;
            private Integer totalModules;
            private String currentModuleTitle;
            private Long timeSpent;
            private Long estimatedTimeRemaining;
            private LocalDateTime startedAt;
            private LocalDateTime lastActiveAt;
            private List<String> achievements;
            
            public ProgressSummaryBuilder userId(String userId) {
                this.userId = userId;
                return this;
            }
            
            public ProgressSummaryBuilder learningPathId(String learningPathId) {
                this.learningPathId = learningPathId;
                return this;
            }
            
            public ProgressSummaryBuilder learningPathTitle(String learningPathTitle) {
                this.learningPathTitle = learningPathTitle;
                return this;
            }
            
            public ProgressSummaryBuilder status(ProgressStatus status) {
                this.status = status;
                return this;
            }
            
            public ProgressSummaryBuilder completionPercentage(Double completionPercentage) {
                this.completionPercentage = completionPercentage;
                return this;
            }
            
            public ProgressSummaryBuilder completedModules(Integer completedModules) {
                this.completedModules = completedModules;
                return this;
            }
            
            public ProgressSummaryBuilder totalModules(Integer totalModules) {
                this.totalModules = totalModules;
                return this;
            }
            
            public ProgressSummaryBuilder currentModuleTitle(String currentModuleTitle) {
                this.currentModuleTitle = currentModuleTitle;
                return this;
            }
            
            public ProgressSummaryBuilder timeSpent(Long timeSpent) {
                this.timeSpent = timeSpent;
                return this;
            }
            
            public ProgressSummaryBuilder estimatedTimeRemaining(Long estimatedTimeRemaining) {
                this.estimatedTimeRemaining = estimatedTimeRemaining;
                return this;
            }
            
            public ProgressSummaryBuilder startedAt(LocalDateTime startedAt) {
                this.startedAt = startedAt;
                return this;
            }
            
            public ProgressSummaryBuilder lastActiveAt(LocalDateTime lastActiveAt) {
                this.lastActiveAt = lastActiveAt;
                return this;
            }
            
            public ProgressSummaryBuilder achievements(List<String> achievements) {
                this.achievements = achievements;
                return this;
            }
            
            public ProgressSummary build() {
                ProgressSummary summary = new ProgressSummary();
                summary.userId = this.userId;
                summary.learningPathId = this.learningPathId;
                summary.learningPathTitle = this.learningPathTitle;
                summary.status = this.status;
                summary.completionPercentage = this.completionPercentage;
                summary.completedModules = this.completedModules;
                summary.totalModules = this.totalModules;
                summary.currentModuleTitle = this.currentModuleTitle;
                summary.timeSpent = this.timeSpent;
                summary.estimatedTimeRemaining = this.estimatedTimeRemaining;
                summary.startedAt = this.startedAt;
                summary.lastActiveAt = this.lastActiveAt;
                summary.achievements = this.achievements;
                return summary;
            }
        }
        
        // Getters...
    }
    
    public static class ModuleProgress {
        private String moduleId;
        private String title;
        private ProgressStatus status;
        private LocalDateTime completedAt;
        private Long timeSpent;
        private com.codenavigator.common.enums.DifficultyLevel difficulty;
        private Integer estimatedHours;
        
        public static ModuleProgressBuilder builder() {
            return new ModuleProgressBuilder();
        }
        
        public static class ModuleProgressBuilder {
            private String moduleId;
            private String title;
            private ProgressStatus status;
            private LocalDateTime completedAt;
            private Long timeSpent;
            private com.codenavigator.common.enums.DifficultyLevel difficulty;
            private Integer estimatedHours;
            
            public ModuleProgressBuilder moduleId(String moduleId) {
                this.moduleId = moduleId;
                return this;
            }
            
            public ModuleProgressBuilder title(String title) {
                this.title = title;
                return this;
            }
            
            public ModuleProgressBuilder status(ProgressStatus status) {
                this.status = status;
                return this;
            }
            
            public ModuleProgressBuilder completedAt(LocalDateTime completedAt) {
                this.completedAt = completedAt;
                return this;
            }
            
            public ModuleProgressBuilder timeSpent(Long timeSpent) {
                this.timeSpent = timeSpent;
                return this;
            }
            
            public ModuleProgressBuilder difficulty(com.codenavigator.common.enums.DifficultyLevel difficulty) {
                this.difficulty = difficulty;
                return this;
            }
            
            public ModuleProgressBuilder estimatedHours(Integer estimatedHours) {
                this.estimatedHours = estimatedHours;
                return this;
            }
            
            public ModuleProgress build() {
                ModuleProgress progress = new ModuleProgress();
                progress.moduleId = this.moduleId;
                progress.title = this.title;
                progress.status = this.status;
                progress.completedAt = this.completedAt;
                progress.timeSpent = this.timeSpent;
                progress.difficulty = this.difficulty;
                progress.estimatedHours = this.estimatedHours;
                return progress;
            }
        }
    }
    
    public static class StudyStreak {
        private Integer currentStreak;
        private Integer maxStreak;
        private LocalDateTime lastStudyDate;
        private Integer totalStudyDays;
        
        public static StudyStreakBuilder builder() {
            return new StudyStreakBuilder();
        }
        
        public static class StudyStreakBuilder {
            private Integer currentStreak;
            private Integer maxStreak;
            private LocalDateTime lastStudyDate;
            private Integer totalStudyDays;
            
            public StudyStreakBuilder currentStreak(Integer currentStreak) {
                this.currentStreak = currentStreak;
                return this;
            }
            
            public StudyStreakBuilder maxStreak(Integer maxStreak) {
                this.maxStreak = maxStreak;
                return this;
            }
            
            public StudyStreakBuilder lastStudyDate(LocalDateTime lastStudyDate) {
                this.lastStudyDate = lastStudyDate;
                return this;
            }
            
            public StudyStreakBuilder totalStudyDays(Integer totalStudyDays) {
                this.totalStudyDays = totalStudyDays;
                return this;
            }
            
            public StudyStreak build() {
                StudyStreak streak = new StudyStreak();
                streak.currentStreak = this.currentStreak;
                streak.maxStreak = this.maxStreak;
                streak.lastStudyDate = this.lastStudyDate;
                streak.totalStudyDays = this.totalStudyDays;
                return streak;
            }
        }
    }
}