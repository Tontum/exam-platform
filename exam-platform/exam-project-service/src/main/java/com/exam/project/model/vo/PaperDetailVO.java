package com.exam.project.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷详情 VO — 包含题目和选项
 */
@Data
public class PaperDetailVO {

    /** 试卷 ID */
    private Long id;

    /** 试卷名称 */
    private String title;

    /** 试卷描述 */
    private String description;

    /** 试卷类型：1=普通考核、2=阶段考核 */
    private Integer paperType;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 及格分数线 */
    private BigDecimal passScore;

    /** 题目总数 */
    private Integer questionCount;

    /** 答题规定时间（分钟） */
    private Integer durationMinutes;

    /** 试卷状态：0=草稿、1=已发布、2=已截止 */
    private Integer status;

    /** 发布人姓名 */
    private String publisherName;

    /** 所属项目 ID */
    private Long projectId;

    /** 所属项目名称 */
    private String projectName;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 题目列表 */
    private List<QuestionVO> questions;
}
