package com.exam.project.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新试卷请求 DTO
 */
@Data
public class PaperUpdateDTO {

    /** 试卷名称 */
    private String title;

    /** 试卷描述 */
    private String description;

    /** 试卷类型：1=普通考核、2=阶段考核 */
    private Integer paperType;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 及格分数线 */
    private BigDecimal passScore;

    /** 答题规定时间（分钟） */
    private Integer durationMinutes;
}
