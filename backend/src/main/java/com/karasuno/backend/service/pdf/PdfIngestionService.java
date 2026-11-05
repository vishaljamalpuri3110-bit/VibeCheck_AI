package com.karasuno.backend.service.pdf;
import org.apache.pdfbox.Loader;
import com.karasuno.backend.entity.DocumentChunk;
import com.karasuno.backend.repository.DocumentChunkRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Module 1 — PDF Ingestion and Chunking Service.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Load the PDF via Apache PDFBox 3.0.</li>
 *   <li>Extract raw text page-by-page using PDFTextStripper.</li>
 *   <li>Split each page into logical chunks (double-newline paragraph breaks first,
 *       then sentence-boundary fallback, then hard 500-char splits).</li>
 *   <li>Compute MD5 hash per chunk for deduplication.</li>
 *   <li>Batch-persist all DocumentChunk entities to MySQL via Spring Data JPA.</li>
 * </ol>
 */
@Service
public class PdfIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PdfIngestionService.class);
    private static final int MAX_CHUNK_SIZE = 500;

    private final DocumentChunkRepository documentChunkRepository;

    public PdfIngestionService(DocumentChunkRepository documentChunkRepository) {
        this.documentChunkRepository = documentChunkRepository;
    }

    /**
     * Ingests a PDF multipart upload: extracts, chunks, and persists to DB.
     *
     * @param file         the uploaded PDF file
     * @param documentType category label (e.g. "BRAND_HANDBOOK")
     * @return list of all persisted DocumentChunk entities
     */
    @Transactional
    public List<DocumentChunk> ingestPdf(MultipartFile file, String documentType) throws IOException {
        String documentName = sanitizeFilename(file.getOriginalFilename());
        log.info("Starting PDF ingestion: '{}', size={}B, type='{}'",
                documentName, file.getSize(), documentType);

        // Support re-ingestion: delete existing chunks for this document
        long existing = documentChunkRepository.countByDocumentName(documentName);
        if (existing > 0) {
            log.info("Re-ingesting '{}': removing {} existing chunks", documentName, existing);
            documentChunkRepository.deleteByDocumentName(documentName);
        }

        List<DocumentChunk> allChunks = new ArrayList<>();

        try (PDDocument pdfDoc = Loader.loadPDF(file.getInputStream().readAllBytes()))  {
            int totalPages = pdfDoc.getNumberOfPages();
            log.info("'{}' has {} page(s)", documentName, totalPages);

            PDFTextStripper stripper = new PDFTextStripper();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(pdfDoc);

                if (pageText == null || pageText.isBlank()) {
                    log.debug("Page {} of '{}' is blank — skipping", page, documentName);
                    continue;
                }

                List<String> chunks = chunkText(pageText.trim());
                for (int i = 0; i < chunks.size(); i++) {
                    String content = chunks.get(i);
                    if (content.isBlank()) continue;

                    allChunks.add(DocumentChunk.builder()
                            .documentName(documentName)
                            .documentType(documentType != null ? documentType : "GENERAL")
                            .pageNumber(page)
                            .chunkIndex(i)
                            .content(content)
                            .contentLength(content.length())
                            .contentHash(md5(content))
                            .build());
                }
            }
        }

        List<DocumentChunk> saved = documentChunkRepository.saveAll(allChunks);
        log.info("Ingestion complete: '{}' → {} chunks saved", documentName, saved.size());
        return saved;
    }

    /**
     * Splits page text into logical chunks.
     * Strategy: paragraph breaks first → sentence boundaries → hard char limit.
     */
    private List<String> chunkText(String text) {
        List<String> result = new ArrayList<>();
        // Split on two or more consecutive newlines (paragraph break)
        String[] paragraphs = text.split("\\n{2,}");

        for (String para : paragraphs) {
            String trimmed = para.replaceAll("\\s+", " ").trim();
            if (trimmed.isBlank()) continue;

            if (trimmed.length() <= MAX_CHUNK_SIZE) {
                result.add(trimmed);
            } else {
                result.addAll(splitBySentence(trimmed));
            }
        }
        return result;
    }

    /** Splits long text at sentence-ending punctuation, then hard-splits if needed. */
    private List<String> splitBySentence(String text) {
        List<String> result = new ArrayList<>();
        // Split on ". ", "! ", "? " followed by an uppercase letter
        String[] sentences = text.split("(?<=[.!?])\\s+(?=[A-Z])");
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            String s = sentence.trim();
            if (current.length() + s.length() + 1 <= MAX_CHUNK_SIZE) {
                if (!current.isEmpty()) current.append(" ");
                current.append(s);
            } else {
                if (!current.isEmpty()) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                }
                // Single sentence exceeds limit: hard-split
                if (s.length() > MAX_CHUNK_SIZE) {
                    for (int start = 0; start < s.length(); start += MAX_CHUNK_SIZE) {
                        result.add(s.substring(start, Math.min(start + MAX_CHUNK_SIZE, s.length())).trim());
                    }
                } else {
                    current.append(s);
                }
            }
        }
        if (!current.isEmpty()) result.add(current.toString().trim());
        return result;
    }

    /** MD5 hash for chunk content (deduplication). */
    private String md5(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return ""; // MD5 is always available in the JVM
        }
    }

    /** Strips path traversal characters and normalises the filename for safe DB storage. */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown.pdf";
        // Keep only safe characters
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
