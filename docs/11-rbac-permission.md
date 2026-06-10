# 11 - RBAC 权限模型设计

> 本文档定义教师培训在线考试平台的权限模型，所有权限相关开发必须遵循。

---

## 1. 权限模型选型

采用 **RBAC0（基于角色的访问控制）+ 项目隔离** 模型。

- 角色固定，不支持继承或互斥约束
- 权限按 **项目 + 工具 + 角色** 三元组配置
- 用户通过 `project_user` 表归属项目，只能访问被分配的项目

---

## 2. 角色定义

| 角色编码 | 角色名称 | 职责 | 可访问端 |
|---------|---------|------|---------|
| 1 | 管理员 | 创建项目、配置项目工具和权限、管理用户 | 管理后台 `/admin` |
| 2 | 校长 | 发布试卷、批阅试卷、查看统计 | 管理端 `/principal` |
| 3 | 老师/学员 | 使用工具（考试、发帖等） | 学员端 `/teacher` |

### 角色职责详细说明

**管理员 (role=1)**
- 创建、编辑、删除项目
- 为项目配置工具开关（试卷、作业、帖子等）
- 为每个角色配置工具的具体权限（是否允许发布、删除、批阅等）
- 管理用户归属（将用户加入/移出项目）

**校长 (role=2)**
- 在项目内发布试卷
- 批阅老师提交的试卷
- 查看项目统计数据
- 只能操作管理员已启用的工具

**老师 (role=3)**
- 查看自己被分配的项目
- 使用项目内已启用的工具（答题、发帖等）
- 只能看到管理员配置为"可见"的工具

---

## 3. 权限控制流程

### 3.1 工具可见性判断

```
用户进入项目
    │
    ▼
查询 config 表：project_id + role + is_enabled
    │
    ├── is_enabled = 1 → 工具显示在项目工具页
    └── is_enabled = 0 → 工具不显示
```

### 3.2 操作权限判断

```
用户执行操作（如发布试卷）
    │
    ▼
查询 config 表：project_id + tool_id + role
    │
    ├── allow_publish = 1 → 允许发布
    └── allow_publish = 0 → 拒绝，提示无权限
```

---

## 4. 数据库表关系

```
user (用户表)
  │
  ├── role: 1/2/3 (全局角色)
  │
  └── N:M
        │
        ▼
project_user (项目归属表)
  │
  ├── project_id
  └── user_id
        │
        ▼
project (项目表)
  │
  └── 1:N
        │
        ▼
config (配置表)
  │
  ├── project_id
  ├── tool_id
  ├── role: 1/2/3
  ├── is_enabled: 是否启用
  ├── allow_publish: 是否允许发布
  ├── allow_delete: 是否允许删除
  └── allow_review: 是否允许批阅
```

---

## 5. 权限校验规则

### 5.1 项目访问校验

| 规则 | 说明 |
|------|------|
| 管理员 | 可访问所有项目（通过管理后台） |
| 校长/老师 | 只能访问 `project_user` 表中有记录的项目 |

### 5.2 工具可见性校验

| 规则 | 说明 |
|------|------|
| 工具是否显示 | 查询 `config.is_enabled`，按当前用户角色判断 |
| 未配置的工具 | 默认不可见（config 表无记录 = 未启用） |

### 5.3 操作权限校验

| 操作 | 校验字段 | 说明 |
|------|---------|------|
| 发布试卷 | `config.allow_publish` | 仅校长角色 |
| 删除试卷 | `config.allow_delete` | 仅校长角色 |
| 批阅试卷 | `config.allow_review` | 仅校长角色 |
| 答题 | 基础功能，`is_enabled=1` 即可 | 仅老师角色 |

---

## 6. 前端权限控制

### 6.1 菜单显示

```typescript
// 学员端：根据 config.is_enabled 过滤工具列表
const tools = await listProjectTools(projectId)
const visibleTools = tools.filter(t => t.isEnabled)
```

### 6.2 按钮显示

```vue
<!-- 校长端：根据 config.allow_publish 控制发布按钮 -->
<el-button v-if="config.allowPublish">发布试卷</el-button>

<!-- 校长端：根据 config.allow_review 控制批阅按钮 -->
<el-button v-if="config.allowReview">批阅</el-button>
```

### 6.3 接口权限

后端每个接口必须校验：
1. 用户是否已登录（Token 校验）
2. 用户是否属于该项目（project_user 校验）
3. 用户角色是否有该操作权限（config 校验）

---

## 7. 典型场景

### 场景 1：管理员配置项目

```
1. 管理员创建项目 A
2. 为项目 A 启用试卷工具，角色=老师，is_enabled=1
3. 为项目 A 启用试卷工具，角色=校长，is_enabled=1，allow_review=1
4. 为项目 A 启用帖子工具，角色=老师，is_enabled=1
```

### 场景 2：校长发布试卷

```
1. 校长进入项目 A
2. 系统查询 config：project=A, tool=paper, role=2
3. is_enabled=1 → 显示试卷工具
4. allow_publish=1 → 显示发布按钮
5. 校长创建并发布试卷
```

### 场景 3：老师答题

```
1. 老师进入项目 A
2. 系统查询 config：project=A, role=3
3. 试卷工具 is_enabled=1 → 显示
4. 帖子工具 is_enabled=1 → 显示
5. 老师点击试卷工具，看到试卷列表，开始答题
```

---

## 8. 扩展说明

当前采用 RBAC0，满足现有需求。未来如需扩展：

| 需求 | 扩展方案 |
|------|---------|
| 同一角色不同权限 | 在 `project_user` 增加 `project_role` 字段 |
| 细粒度权限（如仅看某类试卷） | 启用 `permission` 表，绑定用户+权限编码 |
| 角色继承（校长继承老师权限） | 升级为 RBAC1 |

---

*本文档随项目演进持续更新。*
