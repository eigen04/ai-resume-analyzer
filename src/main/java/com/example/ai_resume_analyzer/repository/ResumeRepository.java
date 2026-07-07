package com.example.ai_resume_analyzer.repository;

import com.example.ai_resume_analyzer.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    /** Find a resume by its SHA-256 file hash, used for duplicate upload detection. */
    Optional<Resume> findByFileHash(String fileHash);

    /** Paginated list of all resumes ordered by creation time (most recent first). */
    Page<Resume> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Paginated list of resumes owned by a specific user. */
    Page<Resume> findByOwnerOrderByCreatedAtDesc(com.example.ai_resume_analyzer.entity.User owner, Pageable pageable);
}