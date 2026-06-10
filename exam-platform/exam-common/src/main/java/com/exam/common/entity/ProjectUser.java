package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目-用户关联表（N:M 中间表）
 * 记录用户参与了哪些项目
 */
@Data
@TableName("project_user")
public class ProjectUser {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目 ID */
    private Long projectId;

    /** 用户 ID */
    private Long userId;

    /** 加入时间 */
    private LocalDateTime joinedAt;
}
