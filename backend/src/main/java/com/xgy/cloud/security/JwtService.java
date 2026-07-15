package com.xgy.cloud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String createToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant exp = now.plus(jwtProperties.getExpireDays(), ChronoUnit.DAYS);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(principal.getUserId()))
                .claim("phone", principal.getPhone())
                .claim("orgId", principal.getOrgId())
                .claim("roles", principal.getRoles())
                .claim("currentRole", principal.getCurrentRole())
                .claim("currentStudentId", principal.getCurrentStudentId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey())
                .compact();
    }

    public UserPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String phone = claims.get("phone", String.class);
        Long orgId = toLong(claims.get("orgId"));
        String currentRole = claims.get("currentRole", String.class);
        Long currentStudentId = toLong(claims.get("currentStudentId"));

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);

        UserPrincipal principal = new UserPrincipal(
                userId, phone, orgId, roles != null ? roles : List.of(), currentRole, currentStudentId);
        principal.setTokenId(claims.getId());
        principal.setTokenExpireAt(claims.getExpiration() != null ? claims.getExpiration().toInstant() : null);
        return principal;
    }

    public Instant getExpireAt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Date exp = claims.getExpiration();
        return exp != null ? exp.toInstant() : Instant.now();
    }

    public String getTokenId(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
    }

    private SecretKey secretKey() {
        byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    public boolean rolesContain(Collection<String> roles, String role) {
        return roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase(role));
    }
}
