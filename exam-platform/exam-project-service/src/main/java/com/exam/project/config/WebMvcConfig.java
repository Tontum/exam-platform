package com.exam.project.config;

import com.exam.project.interceptor.JwtAuthInterceptor;
import com.exam.project.interceptor.ProjectAccessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 — 注册拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final ProjectAccessInterceptor projectAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 注册 JWT 认证拦截器（优先级最高）
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有 API
                .excludePathPatterns(
                        "/api/auth/login",    // 登录接口不需要 Token
                        "/api/auth/register", // 注册接口不需要 Token
                        "/api/school/**"      // 学校查询接口不需要 Token（注册时用）
                )
                .order(1);  // 优先级最高

        // 2. 注册项目访问权限拦截器
        registry.addInterceptor(projectAccessInterceptor)
                .addPathPatterns("/api/project/**")  // 拦截所有项目相关接口
                .excludePathPatterns(
                        "/api/project/list",          // 项目列表不需要项目归属校验
                        "/api/project"                // 创建项目不需要校验
                )
                .order(2);  // 优先级次之
    }
}
