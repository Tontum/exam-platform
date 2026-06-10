package com.exam.project.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建项目请求 DTO
 */
@Data
public class ProjectCreateDTO {

    /** 项目名称 */
    @NotBlank(message = "项目名称不能为空")
    private String name;

    /** 项目描述 */
    private String description;

    /** 所属省 */
    private String province;

    /** 所属市 */
    private String city;

    /** 项目类型：1=省级项目（管理员创建）、2=校级项目（校长创建） */
    private Integer type;

    /** 校级项目所属学校ID */
    private Long schoolId;

    /** 省级项目关联的学校ID列表 */
    private List<Long> schoolIds;

    /** 校级项目选择的老师ID列表（为空则自动关联本校所有老师） */
    private List<Long> teacherIds;
}
