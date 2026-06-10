package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目-学校关联表
 * 记录项目覆盖的学校
 */
@Data
@TableName("project_school")
public class ProjectSchool {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目 ID */
    private Long projectId;

    /** 学校 ID */
    private Long schoolId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
