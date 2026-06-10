package com.exam.project.service;

import com.exam.project.model.dto.AnswerSaveDTO;
import com.exam.project.model.vo.ExamResultVO;
import com.exam.project.model.vo.SubmitResultVO;

/**
 * 答题服务 — 答题进度、答案 Redis 暂存、提交、自动批阅
 */
public interface AnswerService {

    /**
     * 开始答题（设置 response.status = 1）
     *
     * @param paperId 试卷 ID
     * @param userId  学员 ID
     */
    void startExam(Long paperId, Long userId);

    /**
     * 保存答题进度到 Redis
     *
     * @param paperId 试卷 ID
     * @param userId  学员 ID
     * @param dto     答案和剩余时间
     */
    void saveProgress(Long paperId, Long userId, AnswerSaveDTO dto);

    /**
     * 提交试卷：Redis → MySQL，自动批阅客观题
     *
     * @param paperId 试卷 ID
     * @param userId  学员 ID
     * @return 提交结果
     */
    SubmitResultVO submitExam(Long paperId, Long userId);

    /**
     * 查询已批阅试卷的完整答卷结果
     *
     * @param paperId 试卷 ID
     * @param userId  学员 ID
     * @return 完整答卷结果
     */
    ExamResultVO getExamResult(Long paperId, Long userId);
}
