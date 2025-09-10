package com.codenavigator.core.entity;

import com.codenavigator.common.enums.ProgressStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress")
public class UserProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_module_id")
    private LearningModule currentModule;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status = ProgressStatus.NOT_STARTED;
    
    @Column(name = "completed_modules", nullable = false)
    private Integer completedModules = 0;
    
    @Column(name = "total_modules", nullable = false)
    private Integer totalModules = 0;
    
    @Column(name = "completion_percentage", nullable = false)
    private Double completionPercentage = 0.0;
    
    @Column(name = "module_progress", columnDefinition = "json")
    private String moduleProgress;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 无参构造函数
    public UserProgress() {
    }
    
    // 构造函数
    public UserProgress(User user, LearningPath learningPath) {
        this.user = user;
        this.learningPath = learningPath;
        this.totalModules = learningPath.getModules() != null ? learningPath.getModules().size() : 0;
    }
    
    // 业务方法
    public void startLearning() {
        this.status = ProgressStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
    }
    
    public void updateProgress() {
        this.lastActiveAt = LocalDateTime.now();
        if (this.totalModules > 0) {
            this.completionPercentage = (double) this.completedModules / this.totalModules * 100;
        }
        if (this.completedModules.equals(this.totalModules)) {
            this.status = ProgressStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public LearningPath getLearningPath() {
        return learningPath;
    }
    
    public void setLearningPath(LearningPath learningPath) {
        this.learningPath = learningPath;
    }
    
    public LearningModule getCurrentModule() {
        return currentModule;
    }
    
    public void setCurrentModule(LearningModule currentModule) {
        this.currentModule = currentModule;
    }
    
    public ProgressStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProgressStatus status) {
        this.status = status;
    }
    
    public Integer getCompletedModules() {
        return completedModules;
    }
    
    public void setCompletedModules(Integer completedModules) {
        this.completedModules = completedModules;
    }
    
    public Integer getTotalModules() {
        return totalModules;
    }
    
    public void setTotalModules(Integer totalModules) {
        this.totalModules = totalModules;
    }
    
    public Double getCompletionPercentage() {
        return completionPercentage;
    }
    
    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
    
    public String getModuleProgress() {
        return moduleProgress;
    }
    
    public void setModuleProgress(String moduleProgress) {
        this.moduleProgress = moduleProgress;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }
    
    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}