package com.exam.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批阅详情 VO — 校长查看某位老师的完整答题内容
 * 含客观题自动判分结果和正确答案
 */
@Data
public class ReviewDetailVO {

    /** 答题记录 ID */
    private Long responseId;

    /** 试卷名称 */
    private String paperTitle;

    /** 老师姓名 */
    private String teacherName;

    /** 答题状态：2=已提交、3=已批阅 */
    private Integer status;

    /** 总得分 */
    private BigDecimal totalScore;

    /** 是否合格 */
    private Integer isPass;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 题目作答列表 */
    private List<QuestionAnswerVO> questionAnswers;

    /**
     * 题目作答 VO（含正确答案和批阅结果）
     */
    @Data
    public static class QuestionAnswerVO {

        /** 题目 ID */
        private Long questionId;

        /** 题干 */
        private String title;

        /** 题目类型 */
        private Integer questionType;

        /** 每题分值 */
        private BigDecimal score;

        /** 老师的作答内容 */
        private String answerContent;

        /** 正确答案（客观题） */
        private String correctAnswer;

        /** 客观题自动判分是否正确 */
        private Integer isCorrect;

        /** 该题得分（批阅后） */
        private BigDecimal answerScore;

        /** 批阅评语 */
        private String reviewComment;

        /** 选项列表（含正确答案标识） */
        private List<OptionVO> options;
    }

    /**
     * 选项 VO（含正确答案标识）
     */
    @Data
    public static class OptionVO {

        /** 选项标签 */
        private String optionLabel;

        /** 选项内容 */
        private String optionContent;

        /** 是否为正确答案 */
        private Integer isCorrect;
    }
}
