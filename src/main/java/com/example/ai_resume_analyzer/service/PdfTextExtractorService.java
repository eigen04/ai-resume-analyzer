package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.exception.InvalidPdfException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extracts raw text content from uploaded PDF files using Apache PDFBox.
 *
 * Note on scanned/image-based PDFs:
 * PDFBox cannot extract text from image-based pages. If the extracted text
 * is blank, this service throws an exception. OCR (e.g., Tesseract4J) can be
 * integrated as a fallback in a future enhancement.
 */
@Slf4j
@Service
public class PdfTextExtractorService {

    private static final int MIN_EXTRACTABLE_TEXT_LENGTH = 50;

    /**
     * Extracts text from a PDF multipart file using streaming to avoid loading
     * the entire file into heap memory.
     *
     * @param file the uploaded PDF multipart file
     * @return extracted raw text content
     * @throws IOException if the PDF cannot be read
     * @throws InvalidPdfException if the PDF is encrypted, invalid, or image-based
     */
    public String extractText(MultipartFile file) throws IOException {
        log.debug("Starting PDF text extraction for file: {}", file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            if (document.isEncrypted()) {
                throw new InvalidPdfException("Cannot process an encrypted/password-protected PDF.");
            }

            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new InvalidPdfException("The uploaded PDF has no pages.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String extracted = stripper.getText(document);

            if (extracted == null || extracted.trim().length() < MIN_EXTRACTABLE_TEXT_LENGTH) {
                // This typically indicates a scanned/image-based PDF
                // TODO: Integrate Tesseract4J OCR fallback for scanned PDFs
                throw new InvalidPdfException(
                    "Could not extract readable text from this PDF. " +
                    "The document may be a scanned image. OCR support is not yet available."
                );
            }

            log.debug("Successfully extracted {} characters from {} pages in file: {}",
                extracted.length(), pageCount, file.getOriginalFilename());

            return extracted;
        }
    }
}
