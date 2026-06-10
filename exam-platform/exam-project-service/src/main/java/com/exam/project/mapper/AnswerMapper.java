package com.exam.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.Answer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 答案 Mapper
 */
@Mapper
public interface AnswerMapper extends BaseMapper<Answer> {
}
