package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.dto.ats.ResumeTailorRequest;
import com.example.ai_resume_analyzer.dto.ats.ResumeTemplateData;
import com.example.ai_resume_analyzer.dto.ats.TailoredResumeResponse;
import com.example.ai_resume_analyzer.entity.Resume;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class ResumeGenerationService {

    private final ChatClient chatClient;
    private final ResumeService resumeService;
    private final RestClient restClient;

    private static final String TAILOR_PROMPT = """
        You are an elite executive resume writer and LaTeX formatting expert.
        I will provide a Candidate's existing resume text and a target Job Description.
        
        Your task is to tailor the candidate's resume to match the Job Description and return a JSON object populated with the exact content that will be injected into a LaTeX template.

        "Smart Padding" Rule:
        You MUST weave missing keywords from the JD into the user's existing experience naturally to optimize ATS matching. 
        However, STRICTLY FORBIDDEN to invent explicit fake metrics or fake jobs (e.g., do not say "5 years of Kubernetes" if they don't have it). You must rephrase, contextualize, and extrapolate honestly.

        LaTeX Escaping Rule:
        The output strings will be injected directly into a LaTeX document. You MUST escape special LaTeX characters in your generated text. 
        Specifically, escape: & (\\&), % (\\%), $ (\\$), # (\\#), _ (\\_), { (\\{), } (\\}).

        Output Requirements:
        - summary_text: 2-3 sentences max.
        - skills_languages, skills_frameworks, skills_db_cloud, skills_tools: Comma-separated list of skills.
        - experience_content: Use \\resumeCompanyHeading, \\resumeRoleHeading, \\resumeItemListStart, \\resumeItem, \\resumeItemListEnd macros.
        - projects_content: Use \\resumeProjectHeading, \\resumeItemListStart, \\resumeItem, \\resumeItemListEnd macros.
        - achievements_content: Use standard \\item.

        Job Description ({role}):
        {jobDescription}

        Original Resume Text:
        {resumeText}
        """;

    public ResumeGenerationService(ChatClient.Builder chatClientBuilder, ResumeService resumeService, RestClient.Builder restClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.resumeService = resumeService;
        this.restClient = restClientBuilder.build();
    }

    public TailoredResumeResponse generateTailoredResume(Long resumeId, ResumeTailorRequest request) {
        Resume resume = resumeService.getById(resumeId);
        String resumeText = resume.getExtractedText();

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("Resume text is empty.");
        }

        log.info("Prompting LLM for LaTeX resume tailoring for resumeId: {}", resumeId);

        // Generate tailored content via LLM
        ResumeTemplateData templateData = chatClient.prompt()
            .user(u -> u.text(TAILOR_PROMPT)
                .param("role", request.getRole())
                .param("jobDescription", request.getJobDescription())
                .param("resumeText", resumeText))
            .call()
            .entity(ResumeTemplateData.class);

        if (templateData == null) {
            throw new RuntimeException("LLM failed to generate resume data");
        }

        // Load the LaTeX template
        String template;
        try {
            ClassPathResource resource = new ClassPathResource("templates/resume-template.tex");
            template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load LaTeX template", e);
            throw new RuntimeException("Internal error: Could not load resume template", e);
        }

        // Inject variables
        String injectedTex = template
            .replace("{{CANDIDATE_NAME}}", escapeLatex(request.getCandidateName()))
            .replace("{{EMAIL}}", escapeLatex(request.getEmail()))
            .replace("{{PHONE}}", escapeLatex(request.getPhone()))
            .replace("{{LINKEDIN_URL}}", request.getLinkedinUrl() != null ? request.getLinkedinUrl() : "")
            .replace("{{GITHUB_URL}}", request.getGithubUrl() != null ? request.getGithubUrl() : "")
            .replace("{{PORTFOLIO_URL}}", request.getPortfolioUrl() != null ? request.getPortfolioUrl() : "")
            
            .replace("{{EDUCATION_UNIVERSITY}}", escapeLatex(request.getEducationUniversity()))
            .replace("{{EDUCATION_LOCATION}}", escapeLatex(request.getEducationLocation()))
            .replace("{{EDUCATION_DEGREE}}", escapeLatex(request.getEducationDegree()))
            .replace("{{EDUCATION_CGPA}}", escapeLatex(request.getEducationCgpa()))
            .replace("{{EDUCATION_DURATION}}", escapeLatex(request.getEducationDuration()))
            
            .replace("{{SUMMARY_TEXT}}", emptyIfNull(templateData.getSummaryText()))
            .replace("{{SKILLS_LANGUAGES}}", emptyIfNull(templateData.getSkillsLanguages()))
            .replace("{{SKILLS_FRAMEWORKS}}", emptyIfNull(templateData.getSkillsFrameworks()))
            .replace("{{SKILLS_DB_CLOUD}}", emptyIfNull(templateData.getSkillsDbCloud()))
            .replace("{{SKILLS_TOOLS}}", emptyIfNull(templateData.getSkillsTools()))
            .replace("{{EXPERIENCE_CONTENT}}", emptyIfNull(templateData.getExperienceContent()))
            .replace("{{PROJECTS_CONTENT}}", emptyIfNull(templateData.getProjectsContent()))
            .replace("{{ACHIEVEMENTS_CONTENT}}", emptyIfNull(templateData.getAchievementsContent()));

        log.info("LaTeX template injection complete. Attempting remote compilation...");

        // Compile to PDF via LaTeX-on-HTTP
        String pdfBase64 = null;
        boolean compiledSuccessfully = false;
        String message = "Resume tailored successfully.";

        try {
            // The API requires {"compiler": "pdflatex", "code": "..."}
            Map<String, String> payload = Map.of(
                "compiler", "pdflatex",
                "code", injectedTex
            );

            ResponseEntity<byte[]> response = restClient.post()
                .uri("https://latex.ytotech.com/build")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toEntity(byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                pdfBase64 = Base64.getEncoder().encodeToString(response.getBody());
                compiledSuccessfully = true;
                message += " PDF generated successfully.";
            } else {
                message += " Compilation API returned non-success status.";
            }
        } catch (Exception e) {
            log.warn("Remote LaTeX compilation failed: {}", e.getMessage());
            message += " PDF compilation failed due to LaTeX syntax errors or API unavailability. Please use the raw LaTeX string in Overleaf.";
        }

        return TailoredResumeResponse.builder()
            .rawTex(injectedTex)
            .pdfBase64(pdfBase64)
            .compiledSuccessfully(compiledSuccessfully)
            .message(message)
            .build();
    }
    
    private String escapeLatex(String input) {
        if (input == null) return "";
        // Basic escaping if not already done by the LLM (for user inputs)
        return input.replace("&", "\\&")
                    .replace("%", "\\%")
                    .replace("$", "\\$")
                    .replace("#", "\\#")
                    .replace("_", "\\_");
    }
    
    private String emptyIfNull(String input) {
        return input == null ? "" : input;
    }
}
