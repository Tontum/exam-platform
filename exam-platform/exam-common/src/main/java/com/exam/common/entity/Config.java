package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.common.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 配置表 — 以项目为维度，配置各工具的功能开关和规则
 * 唯一约束：UNIQUE(project_id, tool_id, role)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("config")
public class Config extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目 ID */
    private Long projectId;

    /** 工具 ID */
    private Long toolId;

    /** 角色：1=管理员、2=校长、3=老师 */
    private Integer role;

    /** 是否启用该工具（控制左侧菜单是否显示） */
    private Integer isEnabled;

    /** 是否允许发布 */
    private Integer allowPublish;

    /** 是否允许删除 */
    private Integer allowDelete;

    /** 是否允许批阅（针对试题工具） */
    private Integer allowReview;

    /** 是否必须设置合格分 */
    private Integer requirePassScore;

    /** 主观题是否自动给分 */
    private Integer autoScore;

    /** 允许发布时间段 - 起始 */
    private LocalTime publishTimeStart;

    /** 允许发布时间段 - 截止 */
    private LocalTime publishTimeEnd;

    /** 每次提交试卷获得的考核加分 */
    private Integer scorePerSubmit;
}
