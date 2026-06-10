package com.exam.project.model.vo;

import lombok.Data;

/**
 * 成绩详情 — 选项 VO（含正确答案标记）
 */
@Data
public class ExamResultOptionVO {

    /** 选项 ID */
    private Long optionId;

    /** 选项标签（A/B/C/D/对/错） */
    private String optionLabel;

    /** 选项内容 */
    private String optionContent;

    /** 是否为正确答案 */
    private Boolean isCorrect;
}
