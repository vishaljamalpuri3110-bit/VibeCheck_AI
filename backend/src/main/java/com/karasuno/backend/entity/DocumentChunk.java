package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Stores a single text chunk extracted from a PDF document.
 * Each chunk corresponds to a logical paragraph or ~500-character segment
 * from a specific page of the source reference document.
 */
@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_doc_name", columnList = "documentName"),
    @Index(name = "idx_doc_page", columnList = "pageNumber")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Original sanitized PDF filename */
    @Column(nullable = false, length = 255)
    private String documentName;

    /** Category label: BRAND_HANDBOOK, STYLE_GUIDE, LEGAL_GUIDELINES, GENERAL */
    @Column(length = 100)
    private String documentType;

    /** 1-based page number within the PDF */
    @Column(nullable = false)
    private Integer pageNumber;

    /** Sequential chunk index within the page (0-based) */
    @Column(nullable = false)
    private Integer chunkIndex;

    /** Extracted text content of this chunk */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Character count of content */
    @Column(nullable = false)
    private Integer contentLength;

    /** MD5 hash of content for deduplication */
    @Column(length = 32)
    private String contentHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime ingestedAt;

    @PrePersist
    protected void onCreate() {
        ingestedAt = LocalDateTime.now();
        if (content != null) {
            contentLength = content.length();
        }
    }
}
