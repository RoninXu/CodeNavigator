package com.codenavigator.core.entity;

import com.codenavigator.common.enums.DifficultyLevel;
import com.codenavigator.common.enums.UserLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "learning_paths")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPath {
    
    @Id
    private String id;
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(nullable = false, length = 50)
    private String framework;
    
    @Column(length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "target_level")
    private UserLevel targetLevel;
    
    @Column(name = "estimated_duration")
    private Integer estimatedDuration;
    
    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<LearningModule> modules;
    
    @Column(columnDefinition = "json")
    private String prerequisites;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "tags")
    private String tags;
    
    @Column(name = "completion_count")
    @Builder.Default
    private Integer completionCount = 0;
    
    @Column(name = "average_rating")
    private Double averageRating;
}