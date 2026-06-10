package com.exam.project.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建试卷请求 DTO
 */
@Data
public class PaperCreateDTO {

    /** 试卷名称 */
    @NotBlank(message = "试卷名称不能为空")
    private String title;

    /** 试卷描述 */
    private String description;

    /** 试卷类型：1=普通考核、2=阶段考核 */
    private Integer paperType;

    /** 试卷总分 */
    @NotNull(message = "总分不能为空")
    private BigDecimal totalScore;

    /** 及格分数线 */
    @NotNull(message = "及格分数线不能为空")
    private BigDecimal passScore;

    /** 答题规定时间（分钟） */
    @NotNull(message = "答题时间不能为空")
    private Integer durationMinutes;

    /** 所属项目 ID */
    @NotNull(message = "请选择所属项目")
    private Long projectId;

    /** 题目列表（可选，创建试卷时一起添加题目） */
    private List<QuestionCreateDTO> questions;
}
