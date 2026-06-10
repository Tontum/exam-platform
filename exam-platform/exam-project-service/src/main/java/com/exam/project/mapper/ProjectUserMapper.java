package com.exam.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.ProjectUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目-用户关联 Mapper
 */
@Mapper
public interface ProjectUserMapper extends BaseMapper<ProjectUser> {
}
