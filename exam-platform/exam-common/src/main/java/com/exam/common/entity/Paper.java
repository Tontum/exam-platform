package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.common.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 试卷表 — 校长发布试卷时创建，一张试卷包含多道题目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("paper")
public class Paper extends BaseEntity {

    /** 主键（雪花算法分布式 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 试卷名称（考核名称） */
    private String title;

    /** 试卷描述/说明 */
    private String description;

    /** 试卷类型：1=普通考核、2=阶段考核 */
    private Integer paperType;

    /** 试卷总分 */
    private BigDecimal totalScore;

    /** 及格分数线 */
    private BigDecimal passScore;

    /** 题目总数（冗余字段，方便展示） */
    private Integer questionCount;

    /** 答题规定时间（分钟） */
    private Integer durationMinutes;

    /** 试卷状态：0=草稿、1=已发布、2=已截止 */
    private Integer status;

    /** 发布人 user_id（校长） */
    private Long publisherId;

    /** 试卷所属省 */
    private String province;

    /** 试卷所属市 */
    private String city;

    /** 试卷所属县 */
    private String county;

    /** 试卷所属学校 */
    private String school;

    /** 所属项目 ID */
    private Long projectId;

    /** 逻辑删除：0=未删除、1=已删除 */
    @TableLogic
    private Integer deleted;
}
