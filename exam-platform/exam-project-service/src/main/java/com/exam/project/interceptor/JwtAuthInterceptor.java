package com.exam.project.interceptor;

import com.exam.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器 — 校验请求中的 Token
 * 
 * 拦截规则：
 * 1. 登录接口（/api/auth/login）直接放行
 * 2. 其他接口必须携带有效的 JWT Token
 * 3. Token 从 Authorization 请求头获取，格式：Bearer {token}
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求直接放行（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 获取 Authorization 请求头
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.warn("请求缺少有效的 Authorization 头: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或 Token 已过期\",\"data\":null}");
            return false;
        }

        // 提取 Token
        String token = authHeader.substring(7);

        // 验证 Token
        if (!JwtUtils.validateToken(token)) {
            log.warn("Token 无效或已过期: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\",\"data\":null}");
            return false;
        }

        // 将用户信息存入请求属性，供后续使用
        Long userId = JwtUtils.getUserId(token);
        String username = JwtUtils.getUsername(token);
        Integer role = JwtUtils.getRole(token);
        String scope = JwtUtils.getScope(token);
        String province = JwtUtils.getProvince(token);

        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("role", role);
        request.setAttribute("scope", scope);
        request.setAttribute("province", province);

        // 设置到请求头，方便 Controller 通过 @RequestHeader 获取
        // 需要包装 request 来添加 header
        request.setAttribute("X-User-Id", String.valueOf(userId));
        request.setAttribute("X-User-Role", String.valueOf(role));

        log.debug("JWT 认证通过: userId={}, username={}, role={}, scope={}, province={}", userId, username, role, scope, province);
        return true;
    }
}
