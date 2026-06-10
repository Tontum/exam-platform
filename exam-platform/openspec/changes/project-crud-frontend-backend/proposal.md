# 提案：项目增删改查前后端联调

## 背景

当前教师培训在线考试系统的管理后台项目管理页面（ProjectManage.vue）已经完成了前端 UI 设计，但所有数据都是写死的 mock 数据，需要与后端 exam-project-service 进行联调，实现真正的增删改查功能。

## 目标

1. 实现项目列表查询（分页）
2. 实现项目创建
3. 实现项目编辑
4. 实现项目删除
5. 前后端联调，数据持久化到 MySQL

## 非目标

1. 不涉及项目-用户关联（project_user 表）
2. 不涉及项目配置（config 表）
3. 不涉及权限管理（permission 表）
4. 不涉及角色切换功能

## 涉及模块

### 后端
- exam-project-service（端口 8087）
  - Controller: ProjectController
  - Service: ProjectService
  - Mapper: ProjectMapper

### 前端
- exam-frontend
  - 页面: views/admin/ProjectManage.vue
  - API: api/project.ts

### 数据库
- project 表（已有建表 SQL）

## 验收标准

- [ ] 管理员可以查看项目列表（分页）
- [ ] 管理员可以创建新项目
- [ ] 管理员可以编辑已有项目
- [ ] 管理员可以删除项目
- [ ] 所有操作数据持久化到 MySQL
- [ ] 前端页面显示真实数据
