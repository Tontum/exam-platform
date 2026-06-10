package com.exam.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.common.entity.Project;
import com.exam.project.model.dto.ProjectCreateDTO;
import com.exam.project.model.dto.ProjectQueryDTO;
import com.exam.project.model.vo.ProjectVO;

import java.util.List;

/**
 * 项目服务接口
 */
public interface ProjectService {

    /**
     * 分页查询项目列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<ProjectVO> listProjects(ProjectQueryDTO query);

    /**
     * 查询项目详情
     *
     * @param projectId 项目 ID
     * @return 项目详情 VO
     */
    ProjectVO getProjectById(Long projectId);

    /**
     * 管理员创建项目
     *
     * @param dto       项目信息
     * @param creatorId 创建人 ID
     * @return 创建后的项目
     */
    ProjectVO createProject(ProjectCreateDTO dto, Long creatorId);

    /**
     * 编辑项目
     *
     * @param projectId 项目 ID
     * @param dto       修改内容
     * @return 更新后的项目
     */
    ProjectVO updateProject(Long projectId, ProjectCreateDTO dto);

    /**
     * 老师加入项目
     *
     * @param projectId 项目 ID
     * @param userId    用户 ID
     */
    void joinProject(Long projectId, Long userId);

    /**
     * 老师退出项目
     *
     * @param projectId 项目 ID
     * @param userId    用户 ID
     */
    void leaveProject(Long projectId, Long userId);

    /**
     * 查询老师参与的项目列表
     *
     * @param userId 用户 ID
     * @return 项目列表
     */
    List<ProjectVO> getProjectsByUserId(Long userId);

    /**
     * 删除项目
     *
     * @param projectId 项目 ID
     */
    void deleteProject(Long projectId);
}
