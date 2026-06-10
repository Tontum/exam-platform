package com.exam.common.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新试卷请求 DTO — 所有字段可选，仅更新传入的字段
 */
@Data
public class PaperUpdateDTO {

    /** 试卷名称 */
    private String title;

    /** 试卷描述 */
    private String description;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 及格分数线 */
    private BigDecimal passScore;

    /** 答题规定时间（分钟） */
    private Integer durationMinutes;

    /** 省级筛选 */
    private String province;

    /** 市级筛选 */
    private String city;

    /** 县级筛选 */
    private String county;

    /** 校级筛选 */
    private String school;
}
