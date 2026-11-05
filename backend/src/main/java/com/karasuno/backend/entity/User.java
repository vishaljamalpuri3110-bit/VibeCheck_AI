package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Represents an application user with role-based access control.
 * Supported roles: ROLE_CONTENT_CREATOR, ROLE_MANAGER, ROLE_ADMIN
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** BCrypt-encoded password hash */
    @Column(nullable = false)
    private String passwordHash;

    /** Comma-separated roles, e.g. "ROLE_CONTENT_CREATOR,ROLE_MANAGER" */
    @Column(nullable = false, length = 255)
    private String roles;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
