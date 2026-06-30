package com.vendingcom.machine_service.util.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Validación de los tokens JWT emitidos por auth-service.
 * machine-service NO genera tokens, solo los verifica con el mismo secret.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Date expiration = extractClaims(token).getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (Exception exception) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object rolesObject = extractClaims(token).get("roles");

        if (!(rolesObject instanceof List<?> roles)) {
            return List.of();
        }

        return roles.stream().map(Object::toString).toList();
    }
}
