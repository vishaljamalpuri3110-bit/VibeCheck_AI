
package com.karasuno.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Base64;

@Component
public class JwtTokenProvider {

    private static final Logger log =
            LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs) {

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret is missing");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);

            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                    "JWT secret must contain at least 32 decoded bytes"
                );
            }

            this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "JWT secret must be a valid Base64-encoded key", e
            );
        }

        if (jwtExpirationMs <= 0) {
            throw new IllegalStateException(
                "JWT expiration must be greater than zero"
            );
        }

        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(Authentication authentication) {
        UserDetails principal =
                (UserDetails) authentication.getPrincipal();

        return generateTokenFromUsername(principal.getUsername());
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired");
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported");
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT signature mismatch");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims empty or token missing");
        }

        return false;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
