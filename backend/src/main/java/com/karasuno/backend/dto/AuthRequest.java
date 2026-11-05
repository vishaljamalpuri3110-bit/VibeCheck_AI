package com.karasuno.backend.dto;

import lombok.Data;

/** Request body for the POST /api/auth/login endpoint. */
@Data
public class AuthRequest {
    private String username;
    private String password;
}
