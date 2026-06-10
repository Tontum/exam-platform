package com.exam.project.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目详情 VO
 */
@Data
public class ProjectVO {

    /** 项目 ID */
    private Long id;

    /** 项目名称 */
    private String name;

    /** 项目描述 */
    private String description;

    /** 创建人 ID */
    private Long creatorId;

    /** 所属省 */
    private String province;

    /** 所属市 */
    private String city;

    /** 项目状态 */
    private Integer status;

    /** 项目类型：1=省级项目、2=校级项目 */
    private Integer type;

    /** 校级项目所属学校ID */
    private Long schoolId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
