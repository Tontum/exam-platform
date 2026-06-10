package com.exam.project.controller;

import com.exam.common.common.Result;
import com.exam.project.model.dto.LoginDTO;
import com.exam.project.model.dto.RegisterDTO;
import com.exam.project.model.vo.LoginVO;
import com.exam.project.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 — 登录、注册
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     * 
     * @param dto 登录请求（用户名 + 密码）
     * @return 登录响应（Token + 用户信息）
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    /**
     * 老师注册
     * 
     * @param dto 注册请求
     * @return 登录响应（注册成功后自动登录）
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(authService.register(dto));
    }
}
