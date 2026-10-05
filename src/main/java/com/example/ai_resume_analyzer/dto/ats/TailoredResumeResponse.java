package com.example.ai_resume_analyzer.dto.ats;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TailoredResumeResponse {
    private String rawTex;
    private String pdfUrl;
    private String pdfBase64; // In case the API returns PDF as bytes
    private boolean compiledSuccessfully;
    private String message;
}
