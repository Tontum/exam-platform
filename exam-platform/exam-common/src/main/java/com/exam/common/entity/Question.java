package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.common.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 题目表 — 每张试卷下的题目信息（题干）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question")
public class Question extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 外键，关联 paper.id */
    private Long paperId;

    /** 题干内容 */
    private String title;

    /** 题目类型：1=单选题、2=多选题、3=判断题、4=主观题（简答题） */
    private Integer questionType;

    /** 该题分值 */
    private BigDecimal score;

    /** 是否必答题：0=否、1=是 */
    private Integer isRequired;

    /** 排序号，题目在试卷中的展示顺序 */
    private Integer sortOrder;

    /** 题目解析（批阅后展示） */
    private String analysis;
}
