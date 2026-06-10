package com.exam.common.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体基类 — 所有数据库实体继承此类，统一管理审计字段
 * createdAt 和 updatedAt 由 MyBatis-Plus 自动填充
 */
@Data
public abstract class BaseEntity {

    /** 创建时间（insert 时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（insert 和 update 时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
