package com.example.ai_resume_analyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Objects;

import java.time.LocalDateTime;

/**
 * Represents an uploaded resume and its processing metadata.
 * Tracks the full lifecycle from upload through embedding.
 */
@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String fileName;

    private String contentType;

    /** SHA-256 hash of the original file bytes, used for duplicate detection. */
    @Column(unique = true)
    private String fileHash;

    /** Size of the uploaded file in bytes. */
    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResumeStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resume other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}