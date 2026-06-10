package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.common.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 答案表 — 记录老师对每道题的具体作答内容和批阅结果
 * response_id + question_id 组合唯一确定一条记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("answer")
public class Answer extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 外键，关联 response.id */
    private Long responseId;

    /** 外键，关联 question.id */
    private Long questionId;

    /** 答题老师 user_id（冗余，方便查询） */
    private Long userId;

    /** 老师作答内容（选择题存选项标签，主观题存文本） */
    private String answerContent;

    /** 该题得分（校长批阅后填写） */
    private BigDecimal score;

    /** 校长批阅评语 */
    private String reviewComment;

    /** 客观题自动判分：0=错误、1=正确 */
    private Integer isCorrect;
}
