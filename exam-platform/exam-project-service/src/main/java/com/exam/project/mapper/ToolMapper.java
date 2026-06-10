package com.exam.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.Tool;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工具 Mapper
 */
@Mapper
public interface ToolMapper extends BaseMapper<Tool> {
}
