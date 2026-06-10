package com.exam.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目 Mapper
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
