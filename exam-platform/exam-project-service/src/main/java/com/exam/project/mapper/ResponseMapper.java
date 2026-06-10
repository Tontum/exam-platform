package com.exam.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.Response;
import org.apache.ibatis.annotations.Mapper;

/**
 * 答题记录 Mapper
 */
@Mapper
public interface ResponseMapper extends BaseMapper<Response> {
}
