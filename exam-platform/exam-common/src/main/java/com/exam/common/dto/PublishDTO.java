package com.exam.common.dto;

import lombok.Data;

import java.util.List;

/**
 * 发布试卷请求 DTO
 * 支持两种分发模式：指定用户 ID 列表 或 按层级筛选
 */
@Data
public class PublishDTO {

    /** 目标老师用户 ID 列表（精确指定） */
    private List<Long> targetUserIds;

    /** 省级筛选（按层级筛选用） */
    private String province;

    /** 市级筛选 */
    private String city;

    /** 县级筛选 */
    private String county;

    /** 校级筛选 */
    private String school;
}
