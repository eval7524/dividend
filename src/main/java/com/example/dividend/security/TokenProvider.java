package com.example.dividend.security;

import com.example.dividend.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    private static final long TOKEN_EXPIRE_TIME = 60 * 60 * 1000; // 1시간
    private static final String KEY_ROLES = "roles";

    private final MemberService memberService;

    @Value("${spring.jwt.secret}")
    private String secretKey;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("spring.jwt.secret 이(가) 설정되지 않았습니다. 최소 48바이트(384비트) 키를 Base64 또는 원문으로 설정하세요.");
        }

        String trimmed = secretKey.trim();
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(trimmed);
        } catch (IllegalArgumentException ex) {
            keyBytes = trimmed.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 48) {
            throw new IllegalStateException(
                "JWT 서명 키가 너무 짧습니다. HS384는 최소 48바이트(384비트) 키가 필요합니다. 현재: " + (keyBytes.length * 8) + " bits"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() + TOKEN_EXPIRE_TIME);

        return Jwts.builder()
            .setSubject(username)
            .claim(KEY_ROLES, roles)
            .setIssuedAt(now)
            .setExpiration(expiredDate)
            .signWith(this.signingKey, SignatureAlgorithm.HS384)
            .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // 유효(만료 전)하면 true 반환
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) return false;
        try {
            Claims claims = parseClaims(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(this.signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public Authentication getAuthentication(String jwt) {
        UserDetails userDetails = this.memberService.loadUserByUsername(this.getUsername(jwt));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
}
