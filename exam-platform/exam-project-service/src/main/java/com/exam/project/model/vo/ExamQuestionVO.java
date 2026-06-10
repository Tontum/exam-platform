package com.exam.project.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学员答题页题目 VO — 不含正确答案
 */
@Data
public class ExamQuestionVO {

    /** 题目 ID */
    private Long questionId;

    /** 题干 */
    private String stem;

    /** 题目类型：single/multiple/judge/essay */
    private String type;

    /** 分值 */
    private BigDecimal score;

    /** 选项列表 */
    private List<ExamOptionVO> options;
}
