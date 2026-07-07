package com.example.ai_resume_analyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a text chunk derived from a resume for RAG retrieval.
 */
@Entity
@Data
@Table(
    name = "resume_chunks",
    indexes = @Index(name = "idx_resume_chunks_resume_id", columnList = "resume_id")
)
@NoArgsConstructor
@AllArgsConstructor
public class ResumeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    @Column(nullable = false)
    private Integer chunkIndex;
}