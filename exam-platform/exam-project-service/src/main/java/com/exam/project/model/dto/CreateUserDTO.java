package com.exam.project.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建用户 DTO — 管理员创建校长/老师账号
 */
@Data
public class CreateUserDTO {

    /** 登录账号 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 初始密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /** 角色：1=管理员、2=校长、3=老师 */
    @NotNull(message = "请选择角色")
    private Integer role;

    /** 管理员权限范围：ALL=全国、PROVINCE=省级（仅管理员角色有效） */
    private String scope;

    /** 管理员所属省份（scope=PROVINCE 时必填） */
    private String province;

    /** 学校 ID */
    private Long schoolId;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;
}
