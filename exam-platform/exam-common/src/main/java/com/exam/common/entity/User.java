package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.common.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表 — 教师培训在线考试平台用户
 * 角色：1=管理员、2=校长（管理端）、3=老师/学员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 加密密码 */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 角色：1=管理员、2=校长、3=老师/学员 */
    private Integer role;

    /** 管理员权限范围：ALL=全国、PROVINCE=省级（仅管理员角色有效） */
    private String scope;

    /** 管理员所属省份（scope=PROVINCE 时必填） */
    private String province;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 学校 ID（关联 school 表） */
    private Long schoolId;

    /** 学校名称（从 school 表关联，不存 user 表） */
    @TableField(exist = false)
    private String schoolName;

    /** 账号状态：0=禁用、1=启用 */
    private Integer status;
}
