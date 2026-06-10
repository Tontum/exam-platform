package com.exam.common.common;

import lombok.Getter;

/**
 * 业务异常 — 各服务层遇到业务规则校验失败时抛出
 * 由 GlobalExceptionHandler 统一捕获并转换为 Result 响应
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码 */
    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    // ==================== 常用静态工厂方法 ====================

    /** 资源不存在 */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    /** 未登录或 Token 过期 */
    public static BusinessException unauthorized() {
        return new BusinessException(401, "未登录或 Token 已过期");
    }

    /** 无权限访问 */
    public static BusinessException forbidden() {
        return new BusinessException(403, "无权限访问");
    }

    /** 参数校验失败 */
    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }
}
