package com.exam.project.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建题目请求 DTO
 */
@Data
public class QuestionCreateDTO {

    /** 题干内容 */
    @NotBlank(message = "题干不能为空")
    private String title;

    /** 题目类型：1=单选题、2=多选题、3=判断题、4=主观题 */
    @NotNull(message = "题目类型不能为空")
    private Integer questionType;

    /** 该题分值 */
    @NotNull(message = "分值不能为空")
    private BigDecimal score;

    /** 是否必答题：0=否、1=是 */
    private Integer isRequired;

    /** 排序号 */
    private Integer sortOrder;

    /** 题目解析 */
    private String analysis;

    /** 选项列表（选择题和判断题必填） */
    private List<OptionCreateDTO> options;
}
