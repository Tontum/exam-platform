package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权限表 — RBAC 权限控制，角色-功能-按钮级别
 * 如：paper:publish（试卷发布权限）、paper:delete（试卷删除权限）
 */
@Data
@TableName("permission")
public class Permission {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 角色：1=管理员、2=校长、3=老师 */
    private Integer role;

    /** 工具 ID */
    private Long toolId;

    /** 权限编码（如 paper:publish、paper:delete） */
    private String permissionCode;

    /** 权限名称 */
    private String permissionName;
}
