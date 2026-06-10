package com.exam.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.common.common.BusinessException;
import com.exam.common.common.Result;
import com.exam.project.model.dto.ProjectCreateDTO;
import com.exam.project.model.dto.ProjectQueryDTO;
import com.exam.project.model.vo.ProjectVO;
import com.exam.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目控制器 — 管理项目的 CRUD 和用户加入/退出
 */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 分页查询项目列表（管理员用，查所有项目）
     * 省级管理员自动按省份过滤
     */
    @GetMapping("/list")
    public Result<IPage<ProjectVO>> listProjects(ProjectQueryDTO query,
                                                  @RequestAttribute(required = false) String scope,
                                                  @RequestAttribute(required = false) String province) {
        if ("PROVINCE".equals(scope) && province != null) {
            query.setProvince(province);
        }
        return Result.ok(projectService.listProjects(query));
    }

    /**
     * 查询用户参与的项目列表（校长/老师用）
     */
    @GetMapping("/my")
    public Result<List<ProjectVO>> getMyProjects(@RequestAttribute("userId") Long userId) {
        return Result.ok(projectService.getProjectsByUserId(userId));
    }

    /**
     * 查询项目详情
     */
    @GetMapping("/{id}")
    public Result<ProjectVO> getProject(@PathVariable Long id) {
        return Result.ok(projectService.getProjectById(id));
    }

    /**
     * 管理员创建项目
     * 省级管理员自动绑定省份，校长只能创建 type=2 校级项目
     */
    @PostMapping
    public Result<ProjectVO> createProject(@Valid @RequestBody ProjectCreateDTO dto,
                                            @RequestAttribute("userId") Long userId,
                                            @RequestAttribute("role") Integer role,
                                            @RequestAttribute(required = false) String scope,
                                            @RequestAttribute(value = "province", required = false) String tokenProvince) {
        // 权限校验：只有管理员(role=1)和校长(role=2)可以创建项目
        if (role != 1 && role != 2) {
            throw BusinessException.forbidden();
        }
        // 校长只能创建校级项目(type=2)
        if (role == 2 && (dto.getType() == null || dto.getType() != 2)) {
            throw BusinessException.badRequest("校长只能创建校级项目");
        }
        // 省级管理员创建的项目自动绑定省份
        if ("PROVINCE".equals(scope) && org.springframework.util.StringUtils.hasText(tokenProvince)) {
            dto.setProvince(tokenProvince);
        }
        return Result.ok(projectService.createProject(dto, userId));
    }

    /**
     * 编辑项目（仅管理员或项目创建者可操作）
     */
    @PutMapping("/{id}")
    public Result<ProjectVO> updateProject(@PathVariable Long id,
                                            @Valid @RequestBody ProjectCreateDTO dto,
                                            @RequestAttribute("userId") Long userId,
                                            @RequestAttribute("role") Integer role) {
        if (role != 1) {
            throw BusinessException.forbidden();
        }
        return Result.ok(projectService.updateProject(id, dto));
    }

    /**
     * 管理员将用户加入项目（批量，仅管理员可操作）
     */
    @PostMapping("/{id}/users")
    public Result<Void> addUsersToProject(@PathVariable Long id,
                                           @RequestBody List<Long> userIds,
                                           @RequestAttribute("role") Integer role) {
        if (role != 1) {
            throw BusinessException.forbidden();
        }
        for (Long userId : userIds) {
            try {
                projectService.joinProject(id, userId);
            } catch (BusinessException e) {
                // 已加入的跳过
                if (!e.getMessage().contains("已加入")) {
                    throw e;
                }
            }
        }
        return Result.ok();
    }

    /**
     * 老师加入项目
     */
    @PostMapping("/{id}/join")
    public Result<Void> joinProject(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId) {
        projectService.joinProject(id, userId);
        return Result.ok();
    }

    /**
     * 老师退出项目
     */
    @DeleteMapping("/{id}/leave")
    public Result<Void> leaveProject(@PathVariable Long id,
                                      @RequestAttribute("userId") Long userId) {
        projectService.leaveProject(id, userId);
        return Result.ok();
    }

    /**
     * 删除项目（仅管理员可操作）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(@PathVariable Long id,
                                       @RequestAttribute("role") Integer role) {
        if (role != 1) {
            throw BusinessException.forbidden();
        }
        projectService.deleteProject(id);
        return Result.ok();
    }
}
