package com.ecommerce.security;

import com.ecommerce.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtService(JwtConfig jwtConfig, RedisTemplate<String, Object> redisTemplate) {
        this.jwtConfig = jwtConfig;
        this.redisTemplate = redisTemplate;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        String token = Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration() * 1000))
                .setIssuer(jwtConfig.getIssuer())
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();

        // 存储到Redis用于刷新
        String userId = userDetails.getUsername();
        redisTemplate.opsForValue().set("jwt:refresh:" + userId, token,
                jwtConfig.getRefreshExpiration(), TimeUnit.SECONDS);

        return token;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token) && !isTokenBlacklisted(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public void blacklistToken(String token) {
        Date expiration = extractExpiration(token);
        long ttl = expiration.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set("jwt:black:" + token, "blacklisted", ttl, TimeUnit.MILLISECONDS);
        }
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:black:" + token));
        } catch (Exception e) {
            return false;
        }
    }

    public String refreshToken(String token) {
        // 验证token是否有效且未过期
        if (isTokenExpired(token)) {
            throw new RuntimeException("Token已过期，无法刷新");
        }

        String username = extractUsername(token);
        String refreshKey = "jwt:refresh:" + username;
        String storedToken = (String) redisTemplate.opsForValue().get(refreshKey);

        if (storedToken == null || !storedToken.equals(token)) {
            throw new RuntimeException("Token无效或已被注销");
        }

        // 生成新token
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration() * 1000))
                .setIssuer(jwtConfig.getIssuer())
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}