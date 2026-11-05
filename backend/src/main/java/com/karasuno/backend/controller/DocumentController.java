package com.karasuno.backend.controller;

import com.karasuno.backend.dto.DocumentSummaryDto;
import com.karasuno.backend.entity.DocumentChunk;
import com.karasuno.backend.repository.DocumentChunkRepository;
import com.karasuno.backend.service.audit.AuditService;
import com.karasuno.backend.service.pdf.PdfIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST controller for PDF document ingestion and document management.
 * Handles file upload, document listing, chunk retrieval, keyword search, and deletion.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final PdfIngestionService pdfIngestionService;
    private final DocumentChunkRepository documentChunkRepository;
    private final AuditService auditService;

    public DocumentController(PdfIngestionService pdfIngestionService,
                              DocumentChunkRepository documentChunkRepository,
                              AuditService auditService) {
        this.pdfIngestionService = pdfIngestionService;
        this.documentChunkRepository = documentChunkRepository;
        this.auditService = auditService;
    }

    /**
     * Uploads and ingests a PDF file.
     * Accepts multipart/form-data with 'file' and optional 'documentType'.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", defaultValue = "GENERAL") String documentType,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Uploaded file is empty.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("Error: Only PDF files are accepted.");
        }

        try {
            List<DocumentChunk> chunks = pdfIngestionService.ingestPdf(file, documentType);
            auditService.recordAsync(null, userDetails.getUsername(), "PDF_UPLOAD",
                    "DocumentChunk", null,
                    "File: " + filename + " | Chunks: " + chunks.size(), null);

            return ResponseEntity.ok(new UploadResult(filename, documentType, chunks.size()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("PDF processing error: " + e.getMessage());
        }
    }

    /** Lists all ingested documents with their type and total chunk count. */
    @GetMapping
    public ResponseEntity<List<DocumentSummaryDto>> listDocuments() {
        List<String> names = documentChunkRepository.findDistinctDocumentNames();
        List<DocumentSummaryDto> summaries = names.stream().map(name -> {
            long count = documentChunkRepository.countByDocumentName(name);
            List<DocumentChunk> first = documentChunkRepository
                    .findByDocumentNameOrderByPageNumberAscChunkIndexAsc(name);
            String docType = first.isEmpty() ? "GENERAL" : first.get(0).getDocumentType();
            return new DocumentSummaryDto(name, docType, count);
        }).toList();
        return ResponseEntity.ok(summaries);
    }

    /** Returns all chunks for a specific document, ordered by page then chunk index. */
    @GetMapping("/{documentName}/chunks")
    public ResponseEntity<List<DocumentChunk>> getChunks(@PathVariable String documentName) {
        List<DocumentChunk> chunks = documentChunkRepository
                .findByDocumentNameOrderByPageNumberAscChunkIndexAsc(documentName);
        return chunks.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(chunks);
    }

    /** Full-text keyword search across all document chunks. */
    @GetMapping("/search")
    public ResponseEntity<List<DocumentChunk>> searchChunks(@RequestParam String keyword) {
        return ResponseEntity.ok(documentChunkRepository.searchByKeyword(keyword));
    }

    /** Deletes all chunks for a document (supports re-ingestion). Manager/Admin only. */
    @PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{documentName}")
public ResponseEntity<?> deleteDocument(
        @PathVariable String documentName,
        @AuthenticationPrincipal UserDetails userDetails) {

    long count = documentChunkRepository.countByDocumentName(documentName);

    if (count == 0) {
        return ResponseEntity.notFound().build();
    }

    documentChunkRepository.deleteByDocumentName(documentName);

    auditService.recordAsync(
        null,
        userDetails.getUsername(),
        "DOCUMENT_DELETED",
        "DocumentChunk",
        null,
        "Document: " + documentName,
        null
    );

    return ResponseEntity.ok(
        "Deleted '" + documentName + "' (" + count + " chunks removed)."
    );
}

    /** Response record for the upload endpoint. */
    public record UploadResult(String filename, String documentType, int chunksCreated) {}
}
