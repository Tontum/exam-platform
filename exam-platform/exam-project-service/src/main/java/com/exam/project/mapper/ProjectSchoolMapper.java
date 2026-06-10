package com.exam.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.ProjectSchool;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目-学校关联 Mapper
 */
@Mapper
public interface ProjectSchoolMapper extends BaseMapper<ProjectSchool> {
}
