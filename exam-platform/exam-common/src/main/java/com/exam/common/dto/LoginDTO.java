package com.exam.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginDTO {

    /** 登录账号 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
