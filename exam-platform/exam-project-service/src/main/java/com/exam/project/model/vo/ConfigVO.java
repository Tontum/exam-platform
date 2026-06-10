package com.exam.project.model.vo;

import lombok.Data;

import java.time.LocalTime;

/**
 * 工具配置 VO — 管理后台配置页面展示
 */
@Data
public class ConfigVO {

    /** 配置 ID */
    private Long id;

    /** 工具 ID */
    private Long toolId;

    /** 工具编码 */
    private String toolCode;

    /** 工具名称 */
    private String toolName;

    /** 角色 */
    private Integer role;

    /** 是否启用 */
    private Integer isEnabled;

    /** 是否允许发布 */
    private Integer allowPublish;

    /** 是否允许删除 */
    private Integer allowDelete;

    /** 是否允许批阅 */
    private Integer allowReview;

    /** 是否必须设置合格分 */
    private Integer requirePassScore;

    /** 主观题是否自动给分 */
    private Integer autoScore;

    /** 允许发布时间段-起始 */
    private LocalTime publishTimeStart;

    /** 允许发布时间段-截止 */
    private LocalTime publishTimeEnd;

    /** 每次提交试卷获得的考核加分 */
    private Integer scorePerSubmit;
}
