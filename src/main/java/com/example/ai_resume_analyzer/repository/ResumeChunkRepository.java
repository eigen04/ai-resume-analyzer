package com.example.ai_resume_analyzer.repository;

import com.example.ai_resume_analyzer.entity.ResumeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeChunkRepository extends JpaRepository<ResumeChunk, Long> {

    List<ResumeChunk> findByResumeIdOrderByChunkIndexAsc(Long resumeId);
    void deleteByResumeId(Long resumeId);
}