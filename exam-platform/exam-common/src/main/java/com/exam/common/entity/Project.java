package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.common.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目表 — 管理员/校长创建培训考核项目
 * type=1: 省级项目（管理员创建，可分配多个学校）
 * type=2: 校级项目（校长创建，仅限本校）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目名称 */
    private String name;

    /** 项目描述 */
    private String description;

    /** 创建人（管理员/校长 user_id） */
    private Long creatorId;

    /** 所属省 */
    private String province;

    /** 所属市 */
    private String city;

    /** 项目状态：0=未开始、1=进行中、2=已结束 */
    private Integer status;

    /** 项目类型：1=省级项目（管理员创建）、2=校级项目（校长创建） */
    private Integer type;

    /** 校级项目所属学校ID（省级项目为 null） */
    private Long schoolId;
}
