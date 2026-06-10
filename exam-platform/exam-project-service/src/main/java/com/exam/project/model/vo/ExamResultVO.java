package com.exam.project.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成绩详情 — 试卷级 VO
 */
@Data
public class ExamResultVO {

    /** 试卷名称 */
    private String paperName;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 及格线 */
    private BigDecimal passScore;

    /** 用户得分 */
    private BigDecimal userScore;

    /** 是否合格 */
    private Boolean isPass;

    /** 题目总数 */
    private Integer questionCount;

    /** 客观题正确数 */
    private Integer correctCount;

    /** 客观题总数 */
    private Integer objectiveCount;

    /** 提交时间 */
    private String submitTime;

    /** 批阅时间 */
    private String reviewTime;

    /** 题目列表 */
    private List<ExamResultQuestionVO> questions;
}
