package com.exam.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建题目请求 DTO — 包含选项列表
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
    @NotNull(message = "题目分值不能为空")
    private BigDecimal score;

    /** 是否必答题：0=否、1=是 */
    private Integer isRequired = 1;

    /** 排序号 */
    private Integer sortOrder = 0;

    /** 选项列表（选择题和判断题必填） */
    private List<OptionDTO> options;

    /**
     * 选项 DTO（嵌套在题目创建请求中）
     */
    @Data
    public static class OptionDTO {

        /** 选项标签（A、B、C、D、对、错） */
        @NotBlank(message = "选项标签不能为空")
        private String optionLabel;

        /** 选项文本内容 */
        private String optionContent;

        /** 是否为正确答案：0=否、1=是 */
        @NotNull(message = "是否正确选项不能为空")
        private Integer isCorrect;

        /** 选项排序 */
        private Integer sortOrder = 0;
    }
}
