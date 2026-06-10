package com.exam.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.common.BusinessException;
import com.exam.common.entity.Config;
import com.exam.common.entity.Project;
import com.exam.common.entity.ProjectSchool;
import com.exam.common.entity.ProjectUser;
import com.exam.common.entity.School;
import com.exam.common.entity.Tool;
import com.exam.common.entity.User;
import com.exam.project.mapper.ConfigMapper;
import com.exam.project.mapper.ProjectMapper;
import com.exam.project.mapper.ProjectSchoolMapper;
import com.exam.project.mapper.ProjectUserMapper;
import com.exam.project.mapper.SchoolMapper;
import com.exam.project.mapper.ToolMapper;
import com.exam.project.mapper.UserMapper;
import com.exam.project.model.dto.ProjectCreateDTO;
import com.exam.project.model.dto.ProjectQueryDTO;
import com.exam.project.model.vo.ProjectVO;
import com.exam.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectUserMapper projectUserMapper;
    private final ProjectSchoolMapper projectSchoolMapper;
    private final ToolMapper toolMapper;
    private final ConfigMapper configMapper;
    private final UserMapper userMapper;
    private final SchoolMapper schoolMapper;

    @Override
    public IPage<ProjectVO> listProjects(ProjectQueryDTO query) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        // 按状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(Project::getStatus, query.getStatus());
        }
        // 按关键词模糊查询
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Project::getName, query.getKeyword());
        }
        // 按省份筛选（省级管理员用）
        if (StringUtils.hasText(query.getProvince())) {
            wrapper.eq(Project::getProvince, query.getProvince());
        }
        // 按城市筛选
        if (StringUtils.hasText(query.getCity())) {
            wrapper.eq(Project::getCity, query.getCity());
        }
        wrapper.orderByDesc(Project::getCreatedAt);

        Page<Project> page = new Page<>(query.getPage(), query.getSize());
        IPage<Project> projectPage = projectMapper.selectPage(page, wrapper);

        // 转换为 VO 分页
        return projectPage.convert(this::toVO);
    }

    @Override
    public ProjectVO getProjectById(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        return toVO(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO createProject(ProjectCreateDTO dto, Long creatorId) {
        // 获取创建者信息
        User creator = userMapper.selectById(creatorId);
        if (creator == null) {
            throw BusinessException.badRequest("创建者不存在");
        }

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setCreatorId(creatorId);
        project.setProvince(dto.getProvince());
        project.setCity(dto.getCity());
        project.setStatus(0); // 默认状态：未开始

        // 设置项目类型
        if (dto.getType() != null) {
            project.setType(dto.getType());
        } else {
            project.setType(1); // 默认省级项目
        }

        // 校级项目：自动从创建者获取学校ID
        if (project.getType() == 2) {
            if (creator.getSchoolId() != null) {
                project.setSchoolId(creator.getSchoolId());
                School school = schoolMapper.selectById(creator.getSchoolId());
                if (school != null) {
                    project.setProvince(school.getProvince());
                    project.setCity(school.getCity());
                }
            } else if (dto.getSchoolId() != null) {
                project.setSchoolId(dto.getSchoolId());
            }
        }

        projectMapper.insert(project);

        // 省级项目：关联学校，并自动将各校老师加入项目
        if (project.getType() == 1 && dto.getSchoolIds() != null && !dto.getSchoolIds().isEmpty()) {
            for (Long schoolId : dto.getSchoolIds()) {
                ProjectSchool ps = new ProjectSchool();
                ps.setProjectId(project.getId());
                ps.setSchoolId(schoolId);
                ps.setCreatedAt(LocalDateTime.now());
                projectSchoolMapper.insert(ps);

                // 自动将本校所有老师加入项目
                autoAssignSchoolUsers(project.getId(), schoolId);
            }
            log.info("省级项目关联学校并分配老师: projectId={}, schoolIds={}", project.getId(), dto.getSchoolIds());
        }

        // 校级项目：自动把创建者加入项目
        if (project.getType() == 2) {
            addToProject(project.getId(), creatorId);
            // 如果指定了老师，只关联指定的老师；否则关联本校所有老师
            if (dto.getTeacherIds() != null && !dto.getTeacherIds().isEmpty()) {
                for (Long teacherId : dto.getTeacherIds()) {
                    addToProject(project.getId(), teacherId);
                }
                log.info("校级项目关联指定老师: projectId={}, teacherIds={}", project.getId(), dto.getTeacherIds());
            } else if (project.getSchoolId() != null) {
                autoAssignSchoolUsers(project.getId(), project.getSchoolId());
            }
        }

        // 初始化项目工具配置
        initProjectConfigs(project.getId());

        log.info("创建项目成功: id={}, name={}, type={}, creatorId={}", project.getId(), project.getName(), project.getType(), creatorId);
        return toVO(project);
    }

    /**
     * 将用户加入项目（如果未加入）
     */
    private void addToProject(Long projectId, Long userId) {
        LambdaQueryWrapper<ProjectUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectUser::getProjectId, projectId)
                .eq(ProjectUser::getUserId, userId);
        if (projectUserMapper.selectCount(wrapper) == 0) {
            ProjectUser projectUser = new ProjectUser();
            projectUser.setProjectId(projectId);
            projectUser.setUserId(userId);
            projectUser.setJoinedAt(LocalDateTime.now());
            projectUserMapper.insert(projectUser);
        }
    }

    /**
     * 校级项目：自动关联本校所有老师
     */
    private void autoAssignSchoolUsers(Long projectId, Long schoolId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getSchoolId, schoolId)
                .eq(User::getStatus, 1);
        List<User> users = userMapper.selectList(wrapper);

        for (User user : users) {
            addToProject(projectId, user.getId());
        }
        log.info("校级项目自动关联用户: projectId={}, schoolId={}, 用户数={}", projectId, schoolId, users.size());
    }

    /**
     * 初始化项目工具配置 — 为所有工具和所有角色创建默认配置
     * 管理员/校长：默认启用所有工具
     * 老师：默认启用所有工具
     */
    private void initProjectConfigs(Long projectId) {
        List<Tool> tools = toolMapper.selectList(null);
        int[] roles = {1, 2, 3}; // 管理员、校长、老师

        for (Tool tool : tools) {
            for (int role : roles) {
                Config config = new Config();
                config.setProjectId(projectId);
                config.setToolId(tool.getId());
                config.setRole(role);
                config.setIsEnabled(1); // 默认启用
                config.setAllowPublish(role == 2 ? 1 : 0); // 校长可发布
                config.setAllowDelete(role == 2 ? 1 : 0); // 校长可删除
                config.setAllowReview(role == 2 ? 1 : 0); // 校长可批阅
                config.setRequirePassScore(0);
                config.setAutoScore(0);
                config.setScorePerSubmit(0);
                configMapper.insert(config);
            }
        }
        log.info("初始化项目工具配置完成: projectId={}, 工具数={}, 角色数={}", projectId, tools.size(), roles.length);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateProject(Long projectId, ProjectCreateDTO dto) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        if (StringUtils.hasText(dto.getName())) {
            project.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            project.setDescription(dto.getDescription());
        }
        if (dto.getProvince() != null) {
            project.setProvince(dto.getProvince());
        }
        if (dto.getCity() != null) {
            project.setCity(dto.getCity());
        }
        projectMapper.updateById(project);

        log.info("更新项目成功: id={}", projectId);
        return toVO(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinProject(Long projectId, Long userId) {
        // 校验项目存在
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        // 检查是否已加入
        LambdaQueryWrapper<ProjectUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectUser::getProjectId, projectId)
                .eq(ProjectUser::getUserId, userId);
        if (projectUserMapper.selectCount(wrapper) > 0) {
            throw BusinessException.badRequest("已加入该项目，无需重复加入");
        }
        // 加入项目
        ProjectUser projectUser = new ProjectUser();
        projectUser.setProjectId(projectId);
        projectUser.setUserId(userId);
        projectUser.setJoinedAt(LocalDateTime.now());
        projectUserMapper.insert(projectUser);

        log.info("用户加入项目: projectId={}, userId={}", projectId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveProject(Long projectId, Long userId) {
        LambdaQueryWrapper<ProjectUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectUser::getProjectId, projectId)
                .eq(ProjectUser::getUserId, userId);
        int deleted = projectUserMapper.delete(wrapper);
        if (deleted == 0) {
            throw BusinessException.badRequest("未加入该项目");
        }

        log.info("用户退出项目: projectId={}, userId={}", projectId, userId);
    }

    @Override
    public List<ProjectVO> getProjectsByUserId(Long userId) {
        // 查询用户关联的项目 ID 列表
        LambdaQueryWrapper<ProjectUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectUser::getUserId, userId);
        List<ProjectUser> projectUsers = projectUserMapper.selectList(wrapper);
        List<Long> projectIds = projectUsers.stream()
                .map(ProjectUser::getProjectId)
                .collect(Collectors.toList());

        if (projectIds.isEmpty()) {
            return List.of();
        }
        // 批量查询项目
        List<Project> projects = projectMapper.selectBatchIds(projectIds);
        return projects.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        projectMapper.deleteById(projectId);
        log.info("删除项目成功: id={}, name={}", projectId, project.getName());
    }

    /**
     * Entity → VO 转换
     */
    private ProjectVO toVO(Project project) {
        ProjectVO vo = new ProjectVO();
        BeanUtil.copyProperties(project, vo);
        return vo;
    }
}
