package com.tai.assistant.auth;

import com.tai.assistant.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera y valida los JWT del login. Usa un secreto simétrico (HMAC) configurado
 * por variable de entorno — nunca hardcodeado.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    private SecretKey key() {
        if (signingKey == null) {
            signingKey = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        }
        return signingKey;
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + props.getExpirationMs());
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    /** Devuelve el username del token si es válido, o lanza excepción si está vencido/corrupto/mal firmado. */
    public String validateAndGetUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
