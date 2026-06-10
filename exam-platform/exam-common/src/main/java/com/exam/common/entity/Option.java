package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 选项表 — 每道选择题/判断题的选项及正确答案标注
 */
@Data
@TableName("`option`")
public class Option {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 外键，关联 question.id */
    private Long questionId;

    /** 冗余外键，关联 paper.id，提升关联查询效率 */
    private Long paperId;

    /** 选项标签（A、B、C、D、对、错） */
    private String optionLabel;

    /** 选项文本内容 */
    private String optionContent;

    /** 是否为正确答案：0=否、1=是 */
    private Integer isCorrect;

    /** 选项排序 */
    private Integer sortOrder;
}
