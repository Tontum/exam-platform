# 功能链路分析文档

> 本文档追踪每个功能从前端到数据库的完整实现链路，便于学习和维护。

---

## 一、创建项目（管理员）

### 1.1 功能概述
管理员在管理后台创建新项目，可选择项目类型（省级/校级）并关联学校。

### 1.2 完整链路

```
前端页面 → API 调用 → 后端 Controller → Service → Mapper → 数据库
```

---

### 1.3 前端层

**页面文件**：`exam-frontend/src/views/admin/ProjectManage.vue`

**触发入口**：
```vue
<!-- 第 14-18 行 -->
<el-button type="primary" @click="openCreateDialog" class="create-btn">
  <el-icon><Plus /></el-icon>
  创建项目
</el-button>
```

**表单数据**（第 320-327 行）：
```typescript
const form = reactive({ 
  name: '',           // 项目名称
  description: '',    // 项目描述
  province: '',       // 省份
  city: '',           // 城市
  type: 1,            // 项目类型：1=省级、2=校级
  schoolId: null,     // 校级项目-单个学校ID
  schoolIds: []       // 省级项目-多个学校ID
})
```

**提交函数**（第 357-374 行）：
```typescript
async function handleSubmit() {
  // 1. 表单验证
  if (!form.name.trim()) {
    ElMessage.warning('项目名称不能为空')
    return
  }
  // 2. 调用 API
  await createProject(form)
  // 3. 刷新列表
  fetchProjects()
}
```

---

### 1.4 API 层

**文件**：`exam-frontend/src/api/project.ts`

**函数定义**（第 65-76 行）：
```typescript
export function createProject(data: { 
  name: string; 
  description?: string; 
  province?: string; 
  city?: string;
  type?: number;
  schoolId?: number;
  schoolIds?: number[];
}) {
  return post<ProjectVO>('/project', data)
}
```

**请求详情**：
- 方法：`POST`
- 路径：`/api/project`
- 请求体：`{ name, description, type, schoolIds }`
- 请求头：`Authorization: Bearer {jwt_token}`

---

### 1.5 后端 Controller 层

**文件**：`exam-project-service/src/main/java/com/exam/project/controller/ProjectController.java`

**方法**（第 53-57 行）：
```java
@PostMapping
public Result<ProjectVO> createProject(@Valid @RequestBody ProjectCreateDTO dto,
                                        @RequestAttribute("userId") Long userId) {
    return Result.ok(projectService.createProject(dto, userId));
}
```

**职责**：
- 接收前端请求
- 从 JWT Token 中获取 userId（通过 `@RequestAttribute`）
- 参数校验（`@Valid`）
- 调用 Service 层

---

### 1.6 DTO 层

**文件**：`exam-project-service/src/main/java/com/exam/project/model/dto/ProjectCreateDTO.java`

```java
@Data
public class ProjectCreateDTO {
    @NotBlank(message = "项目名称不能为空")
    private String name;
    private String description;
    private String province;
    private String city;
    private Integer type;        // 1=省级、2=校级
    private Long schoolId;       // 校级项目-单个学校
    private List<Long> schoolIds; // 省级项目-多个学校
    private List<Long> teacherIds; // 选择的老师ID列表
}
```

---

### 1.7 Service 层

**文件**：`exam-project-service/src/main/java/com/exam/project/service/impl/ProjectServiceImpl.java`

**方法**（第 80-120 行）：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public ProjectVO createProject(ProjectCreateDTO dto, Long creatorId) {
    // 1. 获取创建者信息
    User creator = userMapper.selectById(creatorId);
    
    // 2. 创建项目实体
    Project project = new Project();
    project.setName(dto.getName());
    project.setDescription(dto.getDescription());
    project.setCreatorId(creatorId);
    project.setType(dto.getType() != null ? dto.getType() : 1);
    project.setStatus(0); // 草稿状态
    
    // 3. 校级项目：自动从创建者获取学校ID
    if (project.getType() == 2 && creator.getSchoolId() != null) {
        project.setSchoolId(creator.getSchoolId());
    }
    
    // 4. 插入数据库
    projectMapper.insert(project);
    
    // 5. 省级项目：关联学校
    if (project.getType() == 1 && dto.getSchoolIds() != null) {
        for (Long schoolId : dto.getSchoolIds()) {
            ProjectSchool ps = new ProjectSchool();
            ps.setProjectId(project.getId());
            ps.setSchoolId(schoolId);
            projectSchoolMapper.insert(ps);
        }
    }
    
    // 6. 校级项目：自动加入创建者
    if (project.getType() == 2) {
        addToProject(project.getId(), creatorId);
    }
    
    // 7. 初始化工具配置
    initProjectConfigs(project.getId());
    
    return toVO(project);
}
```

**核心逻辑**：
1. 创建项目记录
2. 根据项目类型关联学校
3. 初始化工具配置（为所有工具和角色创建默认配置）

---

### 1.8 Mapper 层

**文件**：`exam-project-service/src/main/java/com/exam/project/mapper/ProjectMapper.java`

```java
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
```

**说明**：继承 MyBatis-Plus 的 `BaseMapper`，自动获得 CRUD 方法，无需手写 SQL。

---

### 1.9 Entity 层

**文件**：`exam-common/src/main/java/com/exam/common/entity/Project.java`

```java
@Data
@TableName("project")
public class Project extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private Long creatorId;
    private String province;
    private String city;
    private Integer status;    // 0=未开始、1=进行中、2=已结束
    private Integer type;      // 1=省级、2=校级
    private Long schoolId;     // 校级项目所属学校
}
```

---

### 1.10 数据库表

**表名**：`project`

```sql
CREATE TABLE `project` (
  `id`          BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `name`        VARCHAR(200) NOT NULL COMMENT '项目名称',
  `description` VARCHAR(500) DEFAULT NULL,
  `creator_id`  BIGINT NOT NULL COMMENT '创建人',
  `province`    VARCHAR(50) DEFAULT NULL,
  `city`        VARCHAR(50) DEFAULT NULL,
  `status`      TINYINT NOT NULL DEFAULT 0,
  `type`        TINYINT NOT NULL DEFAULT 1,
  `school_id`   BIGINT DEFAULT NULL,
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 1.11 数据流转图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端                                  │
│  ProjectManage.vue                                           │
│    ↓ 点击"创建项目"                                           │
│  openCreateDialog() → 弹出表单对话框                          │
│    ↓ 填写信息，点击"创建"                                      │
│  handleSubmit() → createProject(form)                        │
│    ↓                                                         │
│  api/project.ts: post('/project', data)                      │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP POST /api/project
                          │ Headers: Authorization: Bearer xxx
                          │ Body: {name, description, type, schoolIds}
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                     后端 Controller                          │
│  ProjectController.java                                     │
│    @PostMapping                                              │
│    createProject(@RequestBody dto, @RequestAttribute userId) │
│    ↓                                                         │
│  projectService.createProject(dto, userId)                   │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                      Service 层                              │
│  ProjectServiceImpl.java                                    │
│    1. 查询创建者信息 (userMapper.selectById)                  │
│    2. 创建 Project 实体                                      │
│    3. projectMapper.insert(project)                          │
│    4. 关联学校 (projectSchoolMapper.insert)                   │
│    5. 初始化配置 (configMapper.insert × 24)                   │
│    6. 返回 ProjectVO                                         │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                      Mapper 层                               │
│  ProjectMapper extends BaseMapper<Project>                   │
│    ↓ 自动生成 SQL                                            │
│  INSERT INTO project (id, name, description, ...) VALUES (?) │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                      数据库                                  │
│  MySQL: exam_platform.project 表                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、功能清单（待分析）

| # | 功能 | 前端页面 | 后端接口 | 状态 |
|---|------|---------|---------|------|
| 1 | 创建项目 | ProjectManage.vue | POST /api/project | ✅ 已分析 |
| 2 | 用户登录 | Login.vue | POST /api/auth/login | 待分析 |
| 3 | 用户注册 | Register.vue | POST /api/auth/register | 待分析 |
| 4 | 项目列表（管理员） | ProjectManage.vue | GET /api/project/list | 待分析 |
| 5 | 项目列表（学员） | ProjectList.vue | GET /api/project/my | 待分析 |
| 6 | 项目工具页 | ProjectTools.vue | GET /api/project/{id}/config/tools | 待分析 |
| 7 | 删除项目 | ProjectManage.vue | DELETE /api/project/{id} | 待分析 |
| 8 | 成员管理 | ProjectManage.vue | POST /api/project/{id}/users | 待分析 |
| 9 | 用户管理 | UserManage.vue | POST /api/user | 待分析 |
| 10 | 工具配置 | ToolConfig.vue | GET/PUT /api/project/{id}/config | 待分析 |
| 11 | 创建试卷 | PaperCreate.vue | POST /api/paper | 待分析 |
| 12 | 试卷列表 | PaperManage.vue | GET /api/paper/list | 待分析 |
| 13 | 添加题目 | QuestionEdit.vue | POST /api/paper/{id}/question | 待分析 |
| 14 | 发布试卷 | PaperManage.vue | POST /api/paper/{id}/publish | 待分析 |

---

*本文档随开发持续更新*
