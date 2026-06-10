package com.exam.project.service;

import com.exam.project.model.dto.LoginDTO;
import com.exam.project.model.dto.RegisterDTO;
import com.exam.project.model.vo.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     * 
     * @param dto 登录请求
     * @return 登录响应（包含 Token 和用户信息）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 老师注册
     * 
     * @param dto 注册请求
     * @return 登录响应（注册成功后自动登录）
     */
    LoginVO register(RegisterDTO dto);
}
