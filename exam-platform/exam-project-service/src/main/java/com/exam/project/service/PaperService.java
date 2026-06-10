package com.exam.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.common.vo.MyPaperVO;
import com.exam.project.model.dto.PaperCreateDTO;
import com.exam.project.model.dto.PaperUpdateDTO;
import com.exam.project.model.dto.QuestionCreateDTO;
import com.exam.project.model.vo.ExamPaperVO;
import com.exam.project.model.vo.PaperDetailVO;

import java.util.List;

/**
 * 试卷服务接口
 */
public interface PaperService {

    /**
     * 创建试卷（草稿状态）
     * 
     * @param dto       试卷信息
     * @param creatorId 创建人 ID
     * @return 试卷详情
     */
    PaperDetailVO createPaper(PaperCreateDTO dto, Long creatorId);

    /**
     * 更新试卷信息（草稿状态才能更新）
     * 
     * @param paperId 试卷 ID
     * @param dto     更新内容
     * @param userId  操作人 ID
     * @return 试卷详情
     */
    PaperDetailVO updatePaper(Long paperId, PaperUpdateDTO dto, Long userId);

    /**
     * 查询项目下的试卷列表
     * 
     * @param projectId 项目 ID
     * @param name      试卷名称模糊搜索
     * @param status    试卷状态
     * @param page      页码
     * @param size      每页数量
     * @return 试卷列表
     */
    IPage<PaperDetailVO> listPapers(Long projectId, String name, Integer status, Integer page, Integer size);

    /**
     * 查询试卷详情（含题目和选项）
     * 
     * @param paperId 试卷 ID
     * @return 试卷详情
     */
    PaperDetailVO getPaperDetail(Long paperId);

    /**
     * 添加题目到试卷
     * 
     * @param paperId  试卷 ID
     * @param question 题目信息
     * @param creatorId 创建人 ID
     */
    void addQuestion(Long paperId, QuestionCreateDTO question, Long creatorId);

    /**
     * 更新题目及其选项
     * 
     * @param paperId    试卷 ID
     * @param questionId 题目 ID
     * @param dto        题目信息
     * @param userId     操作人 ID
     */
    void updateQuestion(Long paperId, Long questionId, QuestionCreateDTO dto, Long userId);

    /**
     * 删除题目
     * 
     * @param paperId    试卷 ID
     * @param questionId 题目 ID
     * @param userId     操作人 ID
     */
    void deleteQuestion(Long paperId, Long questionId, Long userId);

    /**
     * 获取学员答题页试卷数据（不含正确答案）
     * 
     * @param paperId 试卷 ID
     * @param userId  学员 ID
     * @return 答题页试卷
     */
    ExamPaperVO getExamPaper(Long paperId, Long userId);

    /**
     * 查询学员的试卷列表（从 response 表）
     * 
     * @param projectId 项目 ID
     * @param userId    学员 ID
     * @return 试卷列表
     */
    List<MyPaperVO> listMyPapers(Long projectId, Long userId);

    /**
     * 发布试卷
     * 
     * @param paperId   试卷 ID
     * @param publisherId 发布人 ID
     */
    void publishPaper(Long paperId, Long publisherId);

    /**
     * 下线试卷（已发布 → 已截止）
     * 
     * @param paperId 试卷 ID
     * @param userId  操作人 ID
     */
    void closePaper(Long paperId, Long userId);

    /**
     * 删除试卷（草稿状态才能删除）
     * 
     * @param paperId   试卷 ID
     * @param creatorId 创建人 ID
     */
    void deletePaper(Long paperId, Long creatorId);
}
