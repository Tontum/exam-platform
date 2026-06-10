package com.exam.project.model.vo;

import lombok.Data;

/**
 * 学员答题页选项 VO — 不含正确答案标记
 */
@Data
public class ExamOptionVO {

    /** 选项 ID */
    private Long optionId;

    /** 选项标签（A/B/C/D/对/错） */
    private String optionKey;

    /** 选项内容 */
    private String content;
}
