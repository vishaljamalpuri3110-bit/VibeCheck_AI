package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Approved Facts & Claims Repository.
 * Stores verified product claims, certifications, statistics, and verifiable sources.
 */
@Entity
@Table(name = "brand_facts", indexes = {
    @Index(name = "idx_fact_topic", columnList = "topic")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandFactClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String verifiedClaim;

    /** Source document, citation, or certificate proof */
    @Column(length = 255)
    private String sourceReference;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = true;
    }
}
