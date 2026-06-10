package com.exam.project.model.vo;

import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
public class LoginVO {

    /** JWT Token */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色编码：1=管理员、2=校长、3=老师 */
    private Integer role;

    /** 角色名称 */
    private String roleName;

    /** 学校 ID */
    private Long schoolId;

    /** 学校名称 */
    private String schoolName;

    /** 管理员权限范围：ALL/PROVINCE（仅管理员角色有值） */
    private String scope;

    /** 管理员所属省份（scope=PROVINCE 时有值） */
    private String province;
}
