package com.exam.project.model.vo;

import lombok.Data;

/**
 * 选项 VO
 */
@Data
public class OptionVO {

    /** 选项 ID */
    private Long id;

    /** 选项标签（A、B、C、D、对、错） */
    private String optionLabel;

    /** 选项文本内容 */
    private String optionContent;

    /** 是否为正确答案：0=否、1=是 */
    private Integer isCorrect;

    /** 选项排序 */
    private Integer sortOrder;
}
