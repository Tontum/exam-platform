package com.exam.project.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建选项请求 DTO
 */
@Data
public class OptionCreateDTO {

    /** 选项标签（A、B、C、D、对、错） */
    @NotBlank(message = "选项标签不能为空")
    private String optionLabel;

    /** 选项文本内容 */
    @NotBlank(message = "选项内容不能为空")
    private String optionContent;

    /** 是否为正确答案：0=否、1=是 */
    private Integer isCorrect;

    /** 选项排序 */
    private Integer sortOrder;
}
