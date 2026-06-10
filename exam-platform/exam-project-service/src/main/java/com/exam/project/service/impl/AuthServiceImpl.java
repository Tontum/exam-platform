package com.exam.project.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.common.BusinessException;
import com.exam.common.entity.School;
import com.exam.common.entity.User;
import com.exam.common.entity.ProjectSchool;
import com.exam.common.entity.ProjectUser;
import com.exam.common.utils.JwtUtils;
import com.exam.project.mapper.SchoolMapper;
import com.exam.project.mapper.UserMapper;
import com.exam.project.mapper.ProjectSchoolMapper;
import com.exam.project.mapper.ProjectUserMapper;
import com.exam.project.model.dto.LoginDTO;
import com.exam.project.model.dto.RegisterDTO;
import com.exam.project.model.vo.LoginVO;
import com.exam.project.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final SchoolMapper schoolMapper;
    private final ProjectSchoolMapper projectSchoolMapper;
    private final ProjectUserMapper projectUserMapper;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 2. 验证密码（MD5 加密后比对）
        String encryptedPassword = DigestUtil.md5Hex(dto.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            throw BusinessException.badRequest("账号已被禁用");
        }

        // 3.5 校长/老师登录时，自动加入所属学校关联的省级项目
        if ((user.getRole() == 2 || user.getRole() == 3) && user.getSchoolId() != null) {
            autoJoinProvincialProjects(user);
        }

        // 4. 生成 JWT Token（管理员携带 scope 和 province）
        String token = JwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getScope(), user.getProvince());

        // 5. 构建返回结果
        return buildLoginVO(user, token);
    }

    @Override
    public LoginVO register(RegisterDTO dto) {
        // 1. 验证两次密码是否一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw BusinessException.badRequest("两次输入的密码不一致");
        }

        // 2. 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }

        // 3. 验证学校是否存在
        School school = schoolMapper.selectById(dto.getSchoolId());
        if (school == null) {
            throw BusinessException.badRequest("选择的学校不存在");
        }

        // 4. 创建用户（默认角色为老师 role=3）
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(DigestUtil.md5Hex(dto.getPassword())); // MD5 加密
        user.setRealName(dto.getRealName());
        user.setRole(3); // 老师角色
        user.setSchoolId(dto.getSchoolId());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1); // 默认启用

        userMapper.insert(user);

        // 5. 生成 Token（注册后自动登录，老师无 scope/province）
        String token = JwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole(), null, null);

        log.info("老师注册成功: userId={}, username={}, schoolId={}", user.getId(), user.getUsername(), dto.getSchoolId());
        return buildLoginVO(user, token);
    }

    /**
     * 构建登录响应 VO
     */
    private LoginVO buildLoginVO(User user, String token) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setRoleName(getRoleName(user.getRole()));
        // 从 school 表获取学校信息
        School school = schoolMapper.selectById(user.getSchoolId());
        if (school != null) {
            vo.setSchoolId(school.getId());
            vo.setSchoolName(school.getName());
        }
        vo.setScope(user.getScope());
        vo.setProvince(user.getProvince());
        return vo;
    }

    /**
     * 获取角色名称
     */
    private String getRoleName(Integer role) {
        switch (role) {
            case 1: return "管理员";
            case 2: return "校长";
            case 3: return "老师";
            default: return "未知";
        }
    }

    /**
     * 老师登录时，自动加入所属学校关联的省级项目
     */
    private void autoJoinProvincialProjects(User teacher) {
        // 查找该学校关联的所有省级项目
        LambdaQueryWrapper<ProjectSchool> psWrapper = new LambdaQueryWrapper<>();
        psWrapper.eq(ProjectSchool::getSchoolId, teacher.getSchoolId());
        List<ProjectSchool> projectSchools = projectSchoolMapper.selectList(psWrapper);

        int joinedCount = 0;
        for (ProjectSchool ps : projectSchools) {
            // 检查是否已在项目中
            LambdaQueryWrapper<ProjectUser> puWrapper = new LambdaQueryWrapper<>();
            puWrapper.eq(ProjectUser::getProjectId, ps.getProjectId())
                    .eq(ProjectUser::getUserId, teacher.getId());
            if (projectUserMapper.selectCount(puWrapper) == 0) {
                ProjectUser pu = new ProjectUser();
                pu.setProjectId(ps.getProjectId());
                pu.setUserId(teacher.getId());
                pu.setJoinedAt(java.time.LocalDateTime.now());
                projectUserMapper.insert(pu);
                joinedCount++;
            }
        }

        if (joinedCount > 0) {
            log.info("老师登录时自动加入省级项目: userId={}, schoolId={}, 加入了 {} 个项目",
                    teacher.getId(), teacher.getSchoolId(), joinedCount);
        }
    }
}
