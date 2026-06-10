package com.exam.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

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
    private Integer paperType = 1;

    /** 试卷总分 */
    @NotNull(message = "总分不能为空")
    @Min(value = 1, message = "总分必须大于 0")
    private BigDecimal totalScore;

    /** 及格分数线 */
    @NotNull(message = "及格分不能为空")
    @Min(value = 0, message = "及格分不能为负数")
    private BigDecimal passScore;

    /** 答题规定时间（分钟） */
    @NotNull(message = "答题时间不能为空")
    @Min(value = 1, message = "答题时间必须大于 0")
    private Integer durationMinutes;

    /** 所属项目 ID */
    private Long projectId;

    /** 省级筛选 */
    private String province;

    /** 市级筛选 */
    private String city;

    /** 县级筛选 */
    private String county;

    /** 校级筛选 */
    private String school;
}
