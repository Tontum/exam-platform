package com.exam.project.model.vo;

import lombok.Data;

/**
 * 工具 VO — 学员端查看项目下可用的工具
 * 只返回学员需要的简化信息
 */
@Data
public class ToolVO {

    /** 工具编码（paper、homework 等） */
    private String code;

    /** 工具名称（试卷工具、作业工具等） */
    private String name;

    /** 工具描述 */
    private String description;

    /** 是否启用 */
    private Boolean isEnabled;
}
