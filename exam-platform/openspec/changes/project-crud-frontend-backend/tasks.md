# 任务拆解：项目增删改查

## 任务列表

### 后端任务

- [ ] **Task 1**: 创建 Project 实体类
  - 文件: exam-project-service/src/main/java/com/exam/project/model/entity/Project.java
  - 依赖: 无

- [ ] **Task 2**: 创建 ProjectMapper 接口
  - 文件: exam-project-service/src/main/java/com/exam/project/mapper/ProjectMapper.java
  - 依赖: Task 1

- [ ] **Task 3**: 创建 DTO 类
  - 文件: exam-project-service/src/main/java/com/exam/project/model/dto/ProjectCreateDTO.java
  - 文件: exam-project-service/src/main/java/com/exam/project/model/dto/ProjectUpdateDTO.java
  - 依赖: 无

- [ ] **Task 4**: 创建 ProjectService 接口和实现
  - 文件: exam-project-service/src/main/java/com/exam/project/service/ProjectService.java
  - 文件: exam-project-service/src/main/java/com/exam/project/service/impl/ProjectServiceImpl.java
  - 依赖: Task 1, Task 2, Task 3

- [ ] **Task 5**: 创建 ProjectController
  - 文件: exam-project-service/src/main/java/com/exam/project/controller/ProjectController.java
  - 依赖: Task 4

- [ ] **Task 6**: 配置 application.yml
  - 文件: exam-project-service/src/main/resources/application.yml
  - 内容: 数据库连接、端口配置
  - 依赖: 无

- [ ] **Task 7**: 编写单元测试
  - 文件: exam-project-service/src/test/java/com/exam/project/controller/ProjectControllerTest.java
  - 依赖: Task 5

### 前端任务

- [ ] **Task 8**: 完善 API 接口定义
  - 文件: exam-frontend/src/api/project.ts
  - 依赖: 无

- [ ] **Task 9**: 确保 ProjectManage.vue 正确调用 API
  - 文件: exam-frontend/src/views/admin/ProjectManage.vue
  - 依赖: Task 8

### 联调任务

- [ ] **Task 10**: 启动后端服务并验证 API
  - 依赖: Task 7

- [ ] **Task 11**: 启动前端服务并验证页面
  - 依赖: Task 9, Task 10

- [ ] **Task 12**: 端到端测试
  - 依赖: Task 11

## 执行顺序

```
Task 1 → Task 2 → Task 4 → Task 5 → Task 7
    ↓
Task 3 → Task 4
    ↓
Task 6
    ↓
Task 8 → Task 9 → Task 11 → Task 12
```

## 预计时间

| 阶段 | 预计时间 |
|------|----------|
| 后端开发 | 30 分钟 |
| 前端开发 | 15 分钟 |
| 联调测试 | 15 分钟 |
| **总计** | **60 分钟** |
