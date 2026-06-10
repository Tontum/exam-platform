package com.exam.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.School;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学校 Mapper
 */
@Mapper
public interface SchoolMapper extends BaseMapper<School> {
}
