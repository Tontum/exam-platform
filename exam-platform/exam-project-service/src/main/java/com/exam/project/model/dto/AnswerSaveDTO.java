package com.exam.project.model.dto;

import lombok.Data;

import java.util.Map;

/**
 * 保存答题进度请求 DTO
 */
@Data
public class AnswerSaveDTO {

    /** 答案：questionId → answerContent（单选/判断存选项标签，多选存逗号分隔，主观存文本） */
    private Map<Long, String> answers;

    /** 剩余答题时间（秒） */
    private Integer remainingSeconds;
}
