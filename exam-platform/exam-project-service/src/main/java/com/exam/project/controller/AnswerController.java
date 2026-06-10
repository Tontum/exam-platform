package com.exam.project.controller;

import com.exam.common.common.Result;
import com.exam.project.model.dto.AnswerSaveDTO;
import com.exam.project.model.vo.ExamResultVO;
import com.exam.project.model.vo.SubmitResultVO;
import com.exam.project.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 答题控制器 — 学员答题、保存、提交
 */
@RestController
@RequestMapping("/api/answer")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    /**
     * 开始答题（response.status 0 → 1）
     */
    @PostMapping("/{paperId}/start")
    public Result<Void> startExam(@PathVariable Long paperId,
                                   @RequestAttribute("userId") Long userId) {
        answerService.startExam(paperId, userId);
        return Result.ok();
    }

    /**
     * 保存答题进度到 Redis
     */
    @PostMapping("/{paperId}/save")
    public Result<Void> saveProgress(@PathVariable Long paperId,
                                      @RequestAttribute("userId") Long userId,
                                      @RequestBody AnswerSaveDTO dto) {
        answerService.saveProgress(paperId, userId, dto);
        return Result.ok();
    }

    /**
     * 提交试卷：Redis → MySQL，自动批阅客观题
     */
    @PostMapping("/{paperId}/submit")
    public Result<SubmitResultVO> submitExam(@PathVariable Long paperId,
                                              @RequestAttribute("userId") Long userId) {
        return Result.ok(answerService.submitExam(paperId, userId));
    }

    /**
     * 查询已批阅试卷的完整答卷结果
     */
    @GetMapping("/{paperId}/result")
    public Result<ExamResultVO> getExamResult(@PathVariable Long paperId,
                                               @RequestAttribute("userId") Long userId) {
        return Result.ok(answerService.getExamResult(paperId, userId));
    }
}
