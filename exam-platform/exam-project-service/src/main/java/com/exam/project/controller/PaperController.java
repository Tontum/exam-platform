package com.exam.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.common.common.Result;
import com.exam.common.vo.MyPaperVO;
import com.exam.project.model.dto.PaperCreateDTO;
import com.exam.project.model.dto.PaperUpdateDTO;
import com.exam.project.model.dto.QuestionCreateDTO;
import com.exam.project.model.vo.ExamPaperVO;
import com.exam.project.model.vo.PaperDetailVO;
import com.exam.project.service.PaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 试卷控制器 — 校长发布试卷、管理题目
 */
@RestController
@RequestMapping("/api/paper")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    /**
     * 创建试卷（草稿状态）
     */
    @PostMapping
    public Result<PaperDetailVO> createPaper(@Valid @RequestBody PaperCreateDTO dto,
                                              @RequestAttribute("userId") Long userId) {
        return Result.ok(paperService.createPaper(dto, userId));
    }

    /**
     * 更新试卷信息（草稿状态）
     */
    @PutMapping("/{id}")
    public Result<PaperDetailVO> updatePaper(@PathVariable Long id,
                                              @RequestBody PaperUpdateDTO dto,
                                              @RequestAttribute("userId") Long userId) {
        return Result.ok(paperService.updatePaper(id, dto, userId));
    }

    /**
     * 查询项目下的试卷列表（支持名称和状态筛选）
     */
    @GetMapping("/list")
    public Result<IPage<PaperDetailVO>> listPapers(@RequestParam Long projectId,
                                                    @RequestParam(required = false) String name,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(paperService.listPapers(projectId, name, status, page, size));
    }

    /**
     * 查询学员的试卷列表（从 response 表）
     */
    @GetMapping("/my")
    public Result<List<MyPaperVO>> listMyPapers(@RequestParam Long projectId,
                                                 @RequestAttribute("userId") Long userId) {
        return Result.ok(paperService.listMyPapers(projectId, userId));
    }

    /**
     * 查询试卷详情（含题目和选项）
     */
    @GetMapping("/{id}")
    public Result<PaperDetailVO> getPaperDetail(@PathVariable Long id) {
        return Result.ok(paperService.getPaperDetail(id));
    }

    /**
     * 获取学员答题页试卷数据（不含正确答案）
     */
    @GetMapping("/{id}/exam")
    public Result<ExamPaperVO> getExamPaper(@PathVariable Long id,
                                             @RequestAttribute("userId") Long userId) {
        return Result.ok(paperService.getExamPaper(id, userId));
    }

    /**
     * 添加题目到试卷
     */
    @PostMapping("/{id}/question")
    public Result<Void> addQuestion(@PathVariable Long id,
                                     @Valid @RequestBody QuestionCreateDTO dto,
                                     @RequestAttribute("userId") Long userId) {
        paperService.addQuestion(id, dto, userId);
        return Result.ok();
    }

    /**
     * 更新题目及其选项
     */
    @PutMapping("/{paperId}/question/{questionId}")
    public Result<Void> updateQuestion(@PathVariable Long paperId,
                                        @PathVariable Long questionId,
                                        @Valid @RequestBody QuestionCreateDTO dto,
                                        @RequestAttribute("userId") Long userId) {
        paperService.updateQuestion(paperId, questionId, dto, userId);
        return Result.ok();
    }

    /**
     * 删除题目
     */
    @DeleteMapping("/{paperId}/question/{questionId}")
    public Result<Void> deleteQuestion(@PathVariable Long paperId,
                                        @PathVariable Long questionId,
                                        @RequestAttribute("userId") Long userId) {
        paperService.deleteQuestion(paperId, questionId, userId);
        return Result.ok();
    }

    /**
     * 发布试卷
     */
    @PostMapping("/{id}/publish")
    public Result<Void> publishPaper(@PathVariable Long id,
                                      @RequestAttribute("userId") Long userId) {
        paperService.publishPaper(id, userId);
        return Result.ok();
    }

    /**
     * 下线试卷（已发布 → 已截止）
     */
    @PostMapping("/{id}/close")
    public Result<Void> closePaper(@PathVariable Long id,
                                    @RequestAttribute("userId") Long userId) {
        paperService.closePaper(id, userId);
        return Result.ok();
    }

    /**
     * 删除试卷（草稿状态才能删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePaper(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId) {
        paperService.deletePaper(id, userId);
        return Result.ok();
    }
}
