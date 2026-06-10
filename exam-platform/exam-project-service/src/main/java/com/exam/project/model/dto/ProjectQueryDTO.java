package com.exam.project.model.dto;

import lombok.Data;

/**
 * 项目列表查询 DTO
 */
@Data
public class ProjectQueryDTO {

    /** 项目状态：0=未开始、1=进行中、2=已结束 */
    private Integer status;

    /** 关键词（模糊查询项目名称） */
    private String keyword;

    /** 按省份筛选（省级管理员用） */
    private String province;

    /** 按城市筛选 */
    private String city;

    /** 页码（从 1 开始） */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 20;
}
