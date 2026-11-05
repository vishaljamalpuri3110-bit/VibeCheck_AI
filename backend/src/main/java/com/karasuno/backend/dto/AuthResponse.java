package com.karasuno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Response body after successful authentication — carries the JWT and user metadata. */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String roles;
    private long expiresInMs;
}
