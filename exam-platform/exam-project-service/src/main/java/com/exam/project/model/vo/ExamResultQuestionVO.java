package com.exam.project.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成绩详情 — 逐题结果 VO（含用户答案和批改结果）
 */
@Data
public class ExamResultQuestionVO {

    /** 题目 ID */
    private Long questionId;

    /** 题干 */
    private String stem;

    /** 题目类型：1=单选 2=多选 3=判断 4=主观 */
    private Integer questionType;

    /** 该题分值 */
    private BigDecimal score;

    /** 排序号 */
    private Integer sortOrder;

    /** 用户答案（选项标签如 "B" 或 "A,C"，或主观题文本） */
    private String userAnswer;

    /** 该题得分 */
    private BigDecimal gotScore;

    /** 客观题是否正确，主观题为 null */
    private Boolean isCorrect;

    /** 批阅评语（主观题） */
    private String reviewComment;

    /** 选项列表（客观题有，主观题为空） */
    private List<ExamResultOptionVO> options;
}
