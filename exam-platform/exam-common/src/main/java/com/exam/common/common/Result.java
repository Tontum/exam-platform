package com.exam.common.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体 — 所有 Controller 接口统一返回此格式
 *
 * @param <T> data 字段的泛型类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码：200=成功，400=参数错误，401=未登录，403=无权限，404=不存在，500=服务器错误 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 响应时间戳（毫秒） */
    private Long timestamp;

    // ==================== 成功响应 ====================

    /** 成功（无数据） */
    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null, System.currentTimeMillis());
    }

    /** 成功（有数据） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data, System.currentTimeMillis());
    }

    /** 成功（自定义消息 + 数据） */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data, System.currentTimeMillis());
    }

    // ==================== 失败响应 ====================

    /** 失败（自定义状态码和消息） */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    /** 参数校验失败 */
    public static <T> Result<T> badRequest(String message) {
        return fail(400, message);
    }

    /** 未登录 */
    public static <T> Result<T> unauthorized() {
        return fail(401, "未登录或 Token 已过期");
    }

    /** 无权限 */
    public static <T> Result<T> forbidden() {
        return fail(403, "无权限访问");
    }

    /** 资源不存在 */
    public static <T> Result<T> notFound(String message) {
        return fail(404, message);
    }

    /** 服务器内部错误 */
    public static <T> Result<T> error(String message) {
        return fail(500, message);
    }
}
