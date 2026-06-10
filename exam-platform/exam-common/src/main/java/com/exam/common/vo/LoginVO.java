package com.exam.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** JWT Token */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 角色：1=管理员、2=校长、3=老师 */
    private Integer role;

    /** 真实姓名 */
    private String realName;
}
