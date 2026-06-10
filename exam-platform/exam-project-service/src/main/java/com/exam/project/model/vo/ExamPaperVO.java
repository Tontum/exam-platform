package com.exam.project.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学员答题页试卷 VO — 不含正确答案
 */
@Data
public class ExamPaperVO {

    /** 试卷 ID */
    private Long paperId;

    /** 试卷名称 */
    private String paperName;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 及格分 */
    private BigDecimal passScore;

    /** 题目数 */
    private Integer questionCount;

    /** 答题时间（分钟） */
    private Integer durationMinutes;

    /** 题目列表 */
    private List<ExamQuestionVO> questions;
}
