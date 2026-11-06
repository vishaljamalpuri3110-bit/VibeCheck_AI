package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Glossary & Terminology repository.
 * Stores approved terminology, prohibited words, preferred alternatives, and category tags.
 */
@Entity
@Table(name = "brand_terms", indexes = {
    @Index(name = "idx_term_name", columnList = "term"),
    @Index(name = "idx_term_prohibited", columnList = "prohibited")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String term;

    /** If true, this term is prohibited from brand copy */
    @Column(nullable = false)
    private Boolean prohibited;

    /** Approved preferred alternative if prohibited */
    @Column(length = 200)
    private String approvedAlternative;

    /** Category: e.g. "PRODUCT_NAME", "PROMOTIONAL_HYPE", "LEGAL", "COMPETITOR" */
    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (prohibited == null) prohibited = false;
        if (active == null) active = true;
    }
}
