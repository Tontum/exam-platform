# 技术设计：项目增删改查

## 架构概览

```
┌─────────────────┐     HTTP      ┌─────────────────────┐     MyBatis-Plus     ┌─────────┐
│  Vue 3 前端     │ ──────────── → │  Spring Boot 后端    │ ────────────────── → │  MySQL  │
│  ProjectManage  │ ← ────────── │  ProjectController   │ ← ───────────────── │  project│
│  api/project.ts │     JSON      │  ProjectService      │      Result Set     │  表     │
└─────────────────┘               └─────────────────────┘                      └─────────┘
```

## 后端设计

### 1. 实体类 (Project.java)

```java
@Data
@TableName("project")
public class Project {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private Long creatorId;
    private String province;
    private String city;
    private Integer status;  // 0=未开始、1=进行中、2=已结束
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2. Mapper 接口 (ProjectMapper.java)

```java
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
```

### 3. Service 接口 (ProjectService.java)

```java
public interface ProjectService extends IService<Project> {
    IPage<Project> listProjects(int page, int size);
    Project createProject(ProjectCreateDTO dto, Long creatorId);
    Project updateProject(Long id, ProjectUpdateDTO dto);
    boolean deleteProject(Long id);
}
```

### 4. Service 实现 (ProjectServiceImpl.java)

```java
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> 
    implements ProjectService {
    
    @Override
    public IPage<Project> listProjects(int page, int size) {
        Page<Project> pageParam = new Page<>(page, size);
        return page(pageParam, new QueryWrapper<Project>()
            .orderByDesc("created_at"));
    }
    
    @Override
    public Project createProject(ProjectCreateDTO dto, Long creatorId) {
        Project project = new Project();
        BeanUtils.copyProperties(dto, project);
        project.setCreatorId(creatorId);
        project.setStatus(0); // 默认未开始
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        save(project);
        return project;
    }
    
    @Override
    public Project updateProject(Long id, ProjectUpdateDTO dto) {
        Project project = getById(id);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        BeanUtils.copyProperties(dto, project);
        project.setUpdatedAt(LocalDateTime.now());
        updateById(project);
        return project;
    }
    
    @Override
    public boolean deleteProject(Long id) {
        return removeById(id);
    }
}
```

### 5. Controller (ProjectController.java)

```java
@RestController
@RequestMapping("/api/project")
public class ProjectController {
    
    @Autowired
    private ProjectService projectService;
    
    @GetMapping("/list")
    public Result<IPage<Project>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        return Result.success(projectService.listProjects(page, size));
    }
    
    @PostMapping("/create")
    public Result<Project> create(
            @RequestBody @Valid ProjectCreateDTO dto,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(projectService.createProject(dto, userId));
    }
    
    @PutMapping("/update/{id}")
    public Result<Project> update(
            @PathVariable Long id,
            @RequestBody @Valid ProjectUpdateDTO dto) {
        return Result.success(projectService.updateProject(id, dto));
    }
    
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.success();
    }
}
```

### 6. DTO 类

```java
@Data
public class ProjectCreateDTO {
    @NotBlank(message = "项目名称不能为空")
    private String name;
    private String description;
    private String province;
    private String city;
}

@Data
public class ProjectUpdateDTO {
    @NotBlank(message = "项目名称不能为空")
    private String name;
    private String description;
    private String province;
    private String city;
}
```

## 前端设计

### 1. API 接口 (api/project.ts)

```typescript
import { get, post, put, del } from './index'

// 项目 VO 类型
export interface ProjectVO {
  id: number
  name: string
  description: string
  creatorId: number
  province: string
  city: string
  status: number
  createdAt: string
  updatedAt: string
}

// 分页响应类型
interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 获取项目列表
export function listProjects(params: { page: number; size: number }) {
  return get<PageResult<ProjectVO>>('/project/list', params)
}

// 创建项目
export function createProject(data: {
  name: string
  description?: string
  province?: string
  city?: string
}) {
  return post<ProjectVO>('/project/create', data)
}

// 更新项目
export function updateProject(id: number, data: {
  name: string
  description?: string
  province?: string
  city?: string
}) {
  return put<ProjectVO>(`/project/update/${id}`, data)
}

// 删除项目
export function deleteProject(id: number) {
  return del<void>(`/project/delete/${id}`)
}
```

### 2. 页面组件 (ProjectManage.vue)

已在之前的美化工作中完成，现在需要确保 API 调用正确。

## 数据库设计

### project 表结构（已有）

```sql
CREATE TABLE `project` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `description` VARCHAR(500) NULL COMMENT '项目描述',
  `creator_id` BIGINT NOT NULL COMMENT '创建人（管理员）',
  `province` VARCHAR(50) NULL COMMENT '所属省',
  `city` VARCHAR(50) NULL COMMENT '所属市',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=未开始、1=进行中、2=已结束',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';
```

## 依赖关系

```
前端 (exam-frontend)
    ↓ HTTP 请求
API 网关 (exam-gateway) :8080
    ↓ 路由转发
项目服务 (exam-project-service) :8087
    ↓ MyBatis-Plus
MySQL 数据库 (13306)
```

## 错误处理

| 错误场景 | HTTP 状态码 | 错误码 | 错误信息 |
|----------|-------------|--------|----------|
| 项目名称为空 | 400 | 40001 | 项目名称不能为空 |
| 项目不存在 | 404 | 40401 | 项目不存在 |
| 服务器内部错误 | 500 | 50000 | 服务器内部错误 |
