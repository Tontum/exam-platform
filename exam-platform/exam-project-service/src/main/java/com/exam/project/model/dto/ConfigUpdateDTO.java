package com.exam.project.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

/**
 * 更新工具配置请求 DTO
 */
@Data
public class ConfigUpdateDTO {

    /** 配置 ID */
    @NotNull(message = "配置 ID 不能为空")
    private Long id;

    /** 是否启用该工具 */
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

    /** 允许发布时间段 - 起始 */
    private LocalTime publishTimeStart;

    /** 允许发布时间段 - 截止 */
    private LocalTime publishTimeEnd;

    /** 每次提交试卷获得的考核加分 */
    private Integer scorePerSubmit;
}
