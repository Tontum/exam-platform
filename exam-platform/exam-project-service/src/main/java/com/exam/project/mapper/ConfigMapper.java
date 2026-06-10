package com.exam.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.Config;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配置 Mapper
 */
@Mapper
public interface ConfigMapper extends BaseMapper<Config> {
}
