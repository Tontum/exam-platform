package com.exam.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交答案请求 DTO
 */
@Data
public class AnswerSubmitDTO {

    /** 作答内容（选择题存选项标签，主观题存文本） */
    @NotBlank(message = "答案内容不能为空")
    private String answerContent;

    /** 该题答题耗时（秒），用于统计 */
    private Integer durationSeconds;
}
