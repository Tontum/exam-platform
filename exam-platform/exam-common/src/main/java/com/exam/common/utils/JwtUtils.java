package com.exam.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 — 生成和解析 Token
 * 使用 HMAC-SHA256 签名算法
 */
public class JwtUtils {

    /** 密钥（生产环境应从配置文件读取） */
    private static final String SECRET = "exam-platform-jwt-secret-key-must-be-at-least-256-bits-long";
    
    /** Token 有效期（毫秒）：24 小时 */
    private static final long EXPIRATION = 24 * 60 * 60 * 1000L;

    /** 获取签名密钥 */
    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     * 
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色编码（1=管理员、2=校长、3=老师）
     * @return JWT Token
     */
    public static String generateToken(Long userId, String username, Integer role) {
        return generateToken(userId, username, role, null, null);
    }

    /**
     * 生成 Token（含管理员权限范围）
     * 
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色编码（1=管理员、2=校长、3=老师）
     * @param scope    管理员权限范围（ALL/PROVINCE，非管理员传 null）
     * @param province 管理员所属省份（scope=PROVINCE 时传值）
     * @return JWT Token
     */
    public static String generateToken(Long userId, String username, Integer role, String scope, String province) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        if (scope != null) {
            claims.put("scope", scope);
        }
        if (province != null) {
            claims.put("province", province);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Token
     * 
     * @param token JWT Token
     * @return Claims（包含 userId、username、role）
     * @throws JwtException Token 无效或过期
     */
    public static Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从 Token 中获取角色
     */
    public static Integer getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", Integer.class);
    }

    /**
     * 从 Token 中获取管理员权限范围
     */
    public static String getScope(String token) {
        Claims claims = parseToken(token);
        return claims.get("scope", String.class);
    }

    /**
     * 从 Token 中获取管理员所属省份
     */
    public static String getProvince(String token) {
        Claims claims = parseToken(token);
        return claims.get("province", String.class);
    }

    /**
     * 验证 Token 是否有效
     * 
     * @param token JWT Token
     * @return true=有效，false=无效或过期
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     * 
     * @param token JWT Token
     * @return true=已过期，false=未过期
     */
    public static boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return true;
        }
    }
}
