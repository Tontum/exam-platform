package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 工具/功能模块表 — 项目可配置的功能模块清单
 * 如：试题(paper)、文章(article)、直播(live)、问答(qa)、作业(homework)
 */
@Data
@TableName("tool")
public class Tool {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 工具编码（paper、article、live、qa、homework 等） */
    private String toolCode;

    /** 工具名称（试题、文章、直播、问答、作业等） */
    private String toolName;

    /** 工具描述 */
    private String description;
}
