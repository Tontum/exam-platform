package com.exam.project.interceptor;

import com.exam.common.common.BusinessException;
import com.exam.common.entity.Project;
import com.exam.common.entity.ProjectUser;
import com.exam.project.mapper.ProjectMapper;
import com.exam.project.mapper.ProjectUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 项目访问权限拦截器 — 校验用户是否属于该项目
 * 
 * 拦截规则：
 * 1. 超级管理员（scope=ALL）可访问所有项目
 * 2. 省级管理员（scope=PROVINCE）只能访问本省项目
 * 3. 校长/老师（role=2/3）只能访问 project_user 表中有记录的项目
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectAccessInterceptor implements HandlerInterceptor {

    private final ProjectUserMapper projectUserMapper;
    private final ProjectMapper projectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String userIdStr = (String) request.getAttribute("X-User-Id");
        String userRoleStr = (String) request.getAttribute("X-User-Role");

        if (!StringUtils.hasText(userIdStr) || !StringUtils.hasText(userRoleStr)) {
            return true;
        }

        Long userId = Long.parseLong(userIdStr);
        Integer userRole = Integer.parseInt(userRoleStr);
        String scope = (String) request.getAttribute("scope");
        String tokenProvince = (String) request.getAttribute("province");

        Long projectId = extractProjectId(request.getRequestURI());
        if (projectId == null) {
            return true;
        }

        // 超级管理员可访问所有项目
        if (userRole == 1 && "ALL".equals(scope)) {
            return true;
        }

        // 省级管理员：检查项目是否属于本省
        if (userRole == 1 && "PROVINCE".equals(scope)) {
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                throw BusinessException.notFound("项目不存在");
            }
            if (tokenProvince != null && !tokenProvince.equals(project.getProvince())) {
                log.warn("省级管理员无权访问外省项目: userId={}, projectId={}, adminProvince={}, projectProvince={}",
                        userId, projectId, tokenProvince, project.getProvince());
                throw BusinessException.forbidden();
            }
            return true;
        }

        // 非管理员：校验 project_user 成员关系
        LambdaQueryWrapper<ProjectUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectUser::getProjectId, projectId)
                .eq(ProjectUser::getUserId, userId);
        Long count = projectUserMapper.selectCount(wrapper);

        if (count == 0) {
            log.warn("用户无权访问项目: userId={}, projectId={}", userId, projectId);
            throw BusinessException.forbidden();
        }

        return true;
    }

    private Long extractProjectId(String uri) {
        String prefix = "/api/project/";
        int startIndex = uri.indexOf(prefix);
        if (startIndex == -1) {
            return null;
        }

        String remaining = uri.substring(startIndex + prefix.length());
        int queryIndex = remaining.indexOf('?');
        if (queryIndex != -1) {
            remaining = remaining.substring(0, queryIndex);
        }

        int slashIndex = remaining.indexOf('/');
        String projectIdStr = (slashIndex == -1) ? remaining : remaining.substring(0, slashIndex);

        try {
            return Long.parseLong(projectIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
