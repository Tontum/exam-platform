package com.exam.project.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 题目 VO
 */
@Data
public class QuestionVO {

    /** 题目 ID */
    private Long id;

    /** 试卷 ID */
    private Long paperId;

    /** 题干内容 */
    private String title;

    /** 题目类型：1=单选题、2=多选题、3=判断题、4=主观题 */
    private Integer questionType;

    /** 该题分值 */
    private BigDecimal score;

    /** 是否必答题：0=否、1=是 */
    private Integer isRequired;

    /** 排序号 */
    private Integer sortOrder;

    /** 题目解析 */
    private String analysis;

    /** 选项列表（选择题和判断题） */
    private List<OptionVO> options;
}
