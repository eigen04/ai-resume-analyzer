package com.example.ai_resume_analyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Objects;

/**
 * Represents a text chunk derived from a resume for RAG retrieval.
 */
@Entity
@Getter
@Setter
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResumeChunk other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}