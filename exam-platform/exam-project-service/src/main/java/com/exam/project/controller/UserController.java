package com.exam.project.controller;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.common.BusinessException;
import com.exam.common.common.Result;
import com.exam.common.entity.School;
import com.exam.common.entity.User;
import com.exam.project.mapper.SchoolMapper;
import com.exam.project.mapper.UserMapper;
import com.exam.project.model.dto.CreateUserDTO;
import com.exam.project.model.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器 — 管理员管理校长和老师账号
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final SchoolMapper schoolMapper;

    /**
     * 查询用户列表（分页）
     * 省级管理员只能看到本省的用户
     */
    @GetMapping("/list")
    public Result<IPage<User>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestAttribute(required = false) String scope,
            @RequestAttribute(value = "province", required = false) String tokenProvince) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 省级管理员只能看到本省的用户（通过学校关联省份）
        String effectiveProvince = province;
        if ("PROVINCE".equals(scope) && StringUtils.hasText(tokenProvince)) {
            effectiveProvince = tokenProvince;
        }

        // 按地区筛选（通过学校表关联）
        if (StringUtils.hasText(effectiveProvince) || StringUtils.hasText(city)) {
            LambdaQueryWrapper<School> schoolWrapper = new LambdaQueryWrapper<>();
            schoolWrapper.select(School::getId);
            if (StringUtils.hasText(effectiveProvince)) {
                schoolWrapper.eq(School::getProvince, effectiveProvince);
            }
            if (StringUtils.hasText(city)) {
                schoolWrapper.eq(School::getCity, city);
            }
            List<Long> schoolIds = schoolMapper.selectList(schoolWrapper)
                    .stream().map(School::getId).toList();
            if (!schoolIds.isEmpty()) {
                wrapper.in(User::getSchoolId, schoolIds);
            } else {
                return Result.ok(new Page<>(page, size));
            }
        }

        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getRealName, keyword)
                    .or().like(User::getUsername, keyword));
        }

        wrapper.orderByDesc(User::getCreatedAt);

        IPage<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        // 清除密码字段，填充学校名称
        result.getRecords().forEach(u -> {
            u.setPassword(null);
            if (u.getSchoolId() != null) {
                School school = schoolMapper.selectById(u.getSchoolId());
                if (school != null) {
                    u.setSchoolName(school.getName());
                }
            }
        });
        return Result.ok(result);
    }

    /**
     * 管理员创建用户
     * 超级管理员（scope=ALL）：可创建管理员、校长、老师
     * 省级管理员（scope=PROVINCE）：只能创建老师
     */
    @PostMapping
    public Result<User> createUser(@RequestBody CreateUserDTO dto,
                                    @RequestAttribute("role") Integer currentRole,
                                    @RequestAttribute(required = false) String scope,
                                    @RequestAttribute(value = "province", required = false) String tokenProvince) {

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }

        // 权限校验：省级管理员不能创建管理员
        if ("PROVINCE".equals(scope)) {
            if (dto.getRole() == 1) {
                throw BusinessException.badRequest("省级管理员不能创建管理员账号");
            }
            // 省级管理员创建的用户（校长/老师）必须属于本省的学校
            if (dto.getSchoolId() != null) {
                School school = schoolMapper.selectById(dto.getSchoolId());
                if (school == null) {
                    throw BusinessException.badRequest("学校不存在");
                }
                if (tokenProvince != null && !tokenProvince.equals(school.getProvince())) {
                    throw BusinessException.badRequest("不能创建外省学校的用户");
                }
            }
        }

        // 验证角色
        if (dto.getRole() != 1 && dto.getRole() != 2 && dto.getRole() != 3) {
            throw BusinessException.badRequest("角色无效");
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(DigestUtil.md5Hex(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole());
        // 管理员角色可以设置 scope 和 province
        if (dto.getRole() == 1) {
            user.setScope(dto.getScope() != null ? dto.getScope() : "ALL");
            if ("PROVINCE".equals(user.getScope()) && StringUtils.hasText(dto.getProvince())) {
                user.setProvince(dto.getProvince());
            }
        }
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1);

        // 如果指定了学校，填充学校信息
        if (dto.getSchoolId() != null) {
            School school = schoolMapper.selectById(dto.getSchoolId());
            if (school != null) {
                user.setSchoolId(school.getId());
            }
        }

        userMapper.insert(user);
        
        // 返回时清除密码
        user.setPassword(null);
        return Result.ok(user);
    }

    /**
     * 更新用户状态（启用/禁用，仅管理员可操作）
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status,
                                          @RequestAttribute("role") Integer currentRole,
                                          @RequestAttribute(required = false) String scope,
                                          @RequestAttribute(value = "province", required = false) String tokenProvince) {
        if (currentRole != 1) {
            throw BusinessException.forbidden();
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        // 省级管理员不能操作超级管理员
        if ("PROVINCE".equals(scope) && user.getRole() == 1 && "ALL".equals(user.getScope())) {
            throw BusinessException.forbidden();
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.ok();
    }

    /**
     * 重置用户密码（仅管理员可操作）
     */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword,
                                       @RequestAttribute("role") Integer currentRole,
                                       @RequestAttribute(required = false) String scope) {
        if (currentRole != 1) {
            throw BusinessException.forbidden();
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        // 省级管理员不能重置超级管理员密码
        if ("PROVINCE".equals(scope) && user.getRole() == 1 && "ALL".equals(user.getScope())) {
            throw BusinessException.forbidden();
        }
        user.setPassword(DigestUtil.md5Hex(newPassword));
        userMapper.updateById(user);
        return Result.ok();
    }

    /**
     * 查询本校老师列表（校长用）
     */
    @GetMapping("/teachers")
    public Result<List<User>> listSchoolTeachers(@RequestAttribute("userId") Long userId) {
        // 获取当前用户的学校ID
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null || currentUser.getSchoolId() == null) {
            return Result.ok(List.of());
        }

        // 查询同校的所有老师
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getSchoolId, currentUser.getSchoolId())
                .eq(User::getRole, 3)  // 只查老师
                .eq(User::getStatus, 1);  // 只查启用的
        wrapper.orderByAsc(User::getRealName);

        List<User> teachers = userMapper.selectList(wrapper);
        // 清除密码字段，填充学校名称
        School school = schoolMapper.selectById(currentUser.getSchoolId());
        String schoolName = school != null ? school.getName() : null;
        teachers.forEach(u -> {
            u.setPassword(null);
            u.setSchoolName(schoolName);
        });
        return Result.ok(teachers);
    }
}
