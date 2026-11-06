package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Brand guidelines definition supporting both PDF ingestion and the interactive Brand Guidelines Builder.
 * Tracks versioning, tone of voice, approved color palettes, and audience.
 */
@Entity
@Table(name = "brand_guidelines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandGuideline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String brandName;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String missionStatement;

    @Column(columnDefinition = "TEXT")
    private String targetAudience;

    /** Primary tone descriptors, e.g. "Authoritative, Clear, Empathetic, Modern" */
    @Column(columnDefinition = "TEXT")
    private String toneOfVoice;

    /** Comma-separated or JSON list of valid hex brand colors (e.g. "#A9664D,#F7F5F1,#292824") */
    @Column(columnDefinition = "TEXT")
    private String primaryColors;

    /** Comma-separated competitor names for competitor reference detection */
    @Column(columnDefinition = "TEXT")
    private String competitorWatchlist;

    /** Full descriptive guideline text (from direct builder or summary) */
    @Column(columnDefinition = "LONGTEXT")
    private String guidelineContent;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) active = true;
        if (version == null || version.isBlank()) version = "1.0";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
