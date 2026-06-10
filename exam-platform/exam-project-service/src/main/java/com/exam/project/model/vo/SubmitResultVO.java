package com.exam.project.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 提交试卷结果 VO
 */
@Data
public class SubmitResultVO {

    /** 总分 */
    private BigDecimal totalScore;

    /** 客观题得分 */
    private BigDecimal objectiveScore;

    /** 客观题正确数 */
    private Integer correctCount;

    /** 客观题总数 */
    private Integer totalCount;

    /** 是否合格 */
    private Integer isPass;
}
