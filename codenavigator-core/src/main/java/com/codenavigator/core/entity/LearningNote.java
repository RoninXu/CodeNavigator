package com.codenavigator.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_notes")
public class LearningNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private LearningModule module;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(name = "user_code", columnDefinition = "longtext")
    private String userCode;
    
    @Column(name = "ai_feedback", columnDefinition = "longtext")
    private String aiFeedback;
    
    @Column(name = "source_comparison", columnDefinition = "longtext")
    private String sourceComparison;
    
    @Column(columnDefinition = "longtext")
    private String summary;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteStatus status = NoteStatus.DRAFT;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 笔记状态枚举
    public enum NoteStatus {
        DRAFT("草稿"),
        PUBLISHED("已发布"),
        ARCHIVED("已归档");
        
        private final String displayName;
        
        NoteStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // 无参构造函数
    public LearningNote() {
    }
    
    // 构造函数
    public LearningNote(User user, LearningModule module, String title) {
        this.user = user;
        this.module = module;
        this.title = title;
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
    
    public LearningModule getModule() {
        return module;
    }
    
    public void setModule(LearningModule module) {
        this.module = module;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getUserCode() {
        return userCode;
    }
    
    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }
    
    public String getAiFeedback() {
        return aiFeedback;
    }
    
    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }
    
    public String getSourceComparison() {
        return sourceComparison;
    }
    
    public void setSourceComparison(String sourceComparison) {
        this.sourceComparison = sourceComparison;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public NoteStatus getStatus() {
        return status;
    }
    
    public void setStatus(NoteStatus status) {
        this.status = status;
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