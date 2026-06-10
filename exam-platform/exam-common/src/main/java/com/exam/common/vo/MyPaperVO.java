package com.exam.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员端试卷列表项 VO
 * 展示试卷名称、答题状态、得分、提交时间
 */
@Data
public class MyPaperVO {

    /** 答题记录 ID（response_id） */
    private Long responseId;

    /** 试卷 ID */
    private Long paperId;

    /** 试卷名称 */
    private String paperTitle;

    /** 试卷类型 */
    private Integer paperType;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 及格分 */
    private BigDecimal passScore;

    /** 题目数 */
    private Integer questionCount;

    /** 答题时间（分钟） */
    private Integer durationMinutes;

    /** 发布人姓名 */
    private String publisherName;

    /** 答题状态：0=未答题、1=正在答题、2=已提交、3=已批阅 */
    private Integer status;

    /** 得分（批阅后可见） */
    private BigDecimal score;

    /** 是否合格 */
    private Integer isPass;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 批阅时间 */
    private LocalDateTime reviewTime;
}
