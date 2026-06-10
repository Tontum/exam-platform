package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.common.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学校表 — 管理全国学校信息
 * 层级：省 → 市 → 县 → 学校
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("school")
public class School extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学校名称 */
    private String name;

    /** 所属省 */
    private String province;

    /** 所属市 */
    private String city;

    /** 所属县/区 */
    private String county;

    /** 详细地址 */
    private String address;

    /** 状态：0=禁用、1=启用 */
    private Integer status;
}
