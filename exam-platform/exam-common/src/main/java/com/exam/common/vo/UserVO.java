package com.exam.common.vo;

import lombok.Data;

/**
 * 用户信息 VO（不含密码，前端展示用）
 */
@Data
public class UserVO {

    /** 用户 ID */
    private Long id;

    /** 登录账号 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色 */
    private Integer role;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 学校 ID */
    private Long schoolId;

    /** 学校名称 */
    private String schoolName;

    /** 账号状态 */
    private Integer status;
}
