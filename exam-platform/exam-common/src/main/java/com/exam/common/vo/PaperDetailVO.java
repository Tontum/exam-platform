package com.exam.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷详情 VO — 包含试卷基本信息 + 题目列表 + 选项列表
 * 用于校长端查看试卷详情和编辑题目
 */
@Data
public class PaperDetailVO {

    /** 试卷 ID */
    private Long id;

    /** 试卷名称 */
    private String title;

    /** 试卷描述 */
    private String description;

    /** 试卷类型 */
    private Integer paperType;

    /** 总分 */
    private BigDecimal totalScore;

    /** 及格分 */
    private BigDecimal passScore;

    /** 题目总数 */
    private Integer questionCount;

    /** 答题时间（分钟） */
    private Integer durationMinutes;

    /** 试卷状态 */
    private Integer status;

    /** 发布人 ID */
    private Long publisherId;

    /** 项目 ID */
    private Long projectId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 题目列表（含选项） */
    private List<QuestionVO> questions;

    /**
     * 题目 VO（含选项列表，用于试卷详情）
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

        /** 题目解析 */
        private String analysis;

        /** 选项列表 */
        private List<OptionVO> options;
    }

    /**
     * 选项 VO（含正确答案标识，仅校长和管理员可见）
     */
    @Data
    public static class OptionVO {

        /** 选项 ID */
        private Long id;

        /** 选项标签 */
        private String optionLabel;

        /** 选项内容 */
        private String optionContent;

        /** 是否为正确答案 */
        private Integer isCorrect;

        /** 排序号 */
        private Integer sortOrder;
    }
}
