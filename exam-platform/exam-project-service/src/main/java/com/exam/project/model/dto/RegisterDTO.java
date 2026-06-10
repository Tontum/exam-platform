package com.exam.project.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO — 仅用于老师注册
 */
@Data
public class RegisterDTO {

    /** 登录账号 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度为 4-20 个字符")
    private String username;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为 6-20 个字符")
    private String password;

    /** 确认密码 */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /** 学校 ID */
    @NotNull(message = "请选择学校")
    private Long schoolId;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;
}
