package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.common.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 答题记录表 — 记录每位老师对每份试卷的答题状态与成绩
 * 校长发布试卷时向此表批量插入记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("response")
public class Response extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 外键，关联 paper.id */
    private Long paperId;

    /** 答题老师 user_id */
    private Long userId;

    /** 答题状态：0=未答题、1=正在答题、2=已提交、3=已批阅 */
    private Integer status;

    /** 最终得分（批阅后填写） */
    private BigDecimal score;

    /** 是否合格：0=不合格、1=合格 */
    private Integer isPass;

    /** 提交时间（老师点提交按钮时更新） */
    private LocalDateTime submitTime;

    /** 批阅时间（校长批阅完成时更新） */
    private LocalDateTime reviewTime;

    /** 批阅人 user_id（校长） */
    private Long reviewerId;

    /** 答题老师所属省（冗余，方便统计） */
    private String province;

    /** 答题老师所属市 */
    private String city;

    /** 答题老师所属县 */
    private String county;

    /** 答题老师所属学校 */
    private String school;
}
