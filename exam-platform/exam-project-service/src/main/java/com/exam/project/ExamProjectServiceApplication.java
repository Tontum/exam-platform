package com.exam.project;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目服务 — 启动类
 * 负责项目管理、工具配置、权限配置
 */
@SpringBootApplication(scanBasePackages = {"com.exam.project", "com.exam.common"})
@MapperScan("com.exam.project.mapper")
public class ExamProjectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamProjectServiceApplication.class, args);
    }
}
