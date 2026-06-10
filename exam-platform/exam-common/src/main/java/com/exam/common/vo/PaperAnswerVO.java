package com.exam.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷答题 VO — 老师答题页面展示
 * 包含试卷信息和题目列表，不含正确答案，若已有答案则一并返回
 */
@Data
public class PaperAnswerVO {

    /** 答题记录 ID（response_id） */
    private Long responseId;

    /** 试卷 ID */
    private Long paperId;

    /** 试卷名称 */
    private String title;

    /** 答题时间（分钟） */
    private Integer durationMinutes;

    /** 总分 */
    private BigDecimal totalScore;

    /** 题目列表 */
    private List<QuestionVO> questions;

    /**
     * 答题题目 VO（不含正确答案，含已作答内容）
     */
    @Data
    public static class QuestionVO {

        /** 题目 ID */
        private Long id;

        /** 题干 */
        private String title;

        /** 题目类型 */
        private Integer questionType;

        /** 分值 */
        private BigDecimal score;

        /** 是否必答 */
        private Integer isRequired;

        /** 排序号 */
        private Integer sortOrder;

        /** 已作答内容（若已答过） */
        private String answerContent;

        /** 选项列表（不含正确答案标识） */
        private List<OptionVO> options;
    }

    /**
     * 答题选项 VO（不含 isCorrect 字段）
     */
    @Data
    public static class OptionVO {

        /** 选项 ID */
        private Long id;

        /** 选项标签 */
        private String optionLabel;

        /** 选项内容 */
        private String optionContent;

        /** 排序号 */
        private Integer sortOrder;
    }
}
