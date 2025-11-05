package com.popspot.popupplatform.global.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessValidityMs;
    private final long refreshValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds}") long accessValiditySec,
            @Value("${jwt.refresh-token-validity-seconds}") long refreshValiditySec
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessValidityMs = accessValiditySec * 1000;
        this.refreshValidityMs = refreshValiditySec * 1000;
    }

    public String createAccessToken(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessValidityMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshValidityMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public String createSignupToken(String provider, String providerId) {
        return Jwts.builder()
                .claim("purpose", "signup")
                .claim("provider", provider)
                .claim("providerId", providerId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + Duration.ofMinutes(10).toMillis()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseSignupToken(String token) {
        Claims c = parse(token).getBody();
        if (!"signup".equals(c.get("purpose"))) {
            throw new JwtException("Invalid purpose");
        }
        return c;
    }
}