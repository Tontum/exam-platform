package com.exam.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目 Mapper — 基于 MyBatis-Plus BaseMapper，单表 CRUD 自动生成
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
