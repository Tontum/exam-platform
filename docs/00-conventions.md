# 开发规范文档

> 本文档是教师培训在线考试平台的开发规范，所有代码编写必须遵循。
> 最后更新：2026-06-04

---

## 一、测试规范（HARD RULE — 不可跳过）

### 1.1 核心原则

**每个接口写完后必须立即编写集成测试，测试通过后才能写下一个接口。**

原因：AI 无法读取 IDEA 控制台输出，测试是唯一可验证代码正确性的手段。

### 1.2 测试要求

| 项目 | 规范 |
|------|------|
| 测试范围 | Controller 的每个接口至少一条集成测试 |
| 数据库 | 必须连接真实 Docker MySQL（端口 13306，数据库 exam_platform），不 mock 数据库层 |
| 测试类命名 | `{ControllerName}Test.java`，放在 `src/test/java/com/exam/{module}/controller/` |
| 测试方法命名 | `test{接口描述}_{预期结果}`，如 `testListProjects_ShouldReturnPageResult` |
| 运行命令 | `mvn test -pl exam-{service}-service` |
| 通过标准 | 全部测试通过后才能进入下一个接口开发 |

### 1.3 工作流程

```
写完接口 → 写测试 → mvn test → 全部通过 ✓ → 登记到 CLAUDE.md → 继续下一个
                               → 失败 ✗ → 修复 → 重新运行 → 通过后再登记
```

### 1.4 测试结果登记

每完成一个服务的所有测试，在 `CLAUDE.md` 的「测试通过记录」表格中追加：

```
| 日期 | 服务 | 测试类 | 接口数 | 测试数 | 结果 |
```

---

## 二、代码注释规范

### 2.1 注释语言

- 所有代码注释使用**中文**
- 注释内容说明**用途和意图**，不翻译代码本身

### 2.2 类注释

每个类必须有文档注释，说明职责：

```java
/**
 * 项目控制器 — 管理项目的 CRUD 和用户加入/退出
 */
@RestController
@RequestMapping("/api/project")
public class ProjectController {
```

### 2.3 方法注释

每个 public 方法必须有注释，说明功能、参数、返回值：

```java
/**
 * 分页查询项目列表
 * @param query 查询条件
 * @return 分页结果
 */
@GetMapping("/list")
public Result<IPage<ProjectVO>> listProjects(ProjectQueryDTO query) {
```

### 2.4 前端 API 调用标注

前端所有 API 调用位置统一用 `// TODO: 调用 API` 标注，方便后续对接：

```typescript
// TODO: 调用 API — GET /api/project/list
const res = await listProjects({ page: page.value, size: pageSize.value })
```

---

## 三、命名规范

### 3.1 Java 命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | 大驼峰 | `ProjectController` |
| 方法名 | 小驼峰 | `listProjects()` |
| 常量 | 全大写下划线 | `MAX_PAGE_SIZE` |
| 包名 | 全小写 | `com.exam.project` |

### 3.2 数据对象命名

| 对象 | 后缀 | 用途 | 示例 |
|------|------|------|------|
| Entity | 无 | 数据库实体，对应表结构 | `Project` |
| DTO | `DTO` | 请求参数，前端传入 | `ProjectCreateDTO` |
| VO | `VO` | 响应结果，返回前端 | `ProjectVO` |

### 3.3 数据库命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | 小写下划线，复数 | `projects`、`project_users` |
| 字段名 | 小写下划线 | `created_at`、`creator_id` |
| 主键 | `id` | BIGINT 自增 |
| 外键 | `{关联表}_id` | `project_id`、`user_id` |

### 3.4 前端命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 组件文件 | 大驼峰 `.vue` | `ProjectManage.vue` |
| API 文件 | 小写 `.ts` | `project.ts` |
| 变量/函数 | 小驼峰 | `fetchProjects()` |
| CSS 类名 | 短横线 | `page-header` |

---

## 四、接口规范

### 4.1 RESTful 风格

| 操作 | HTTP 方法 | 路径 | 示例 |
|------|-----------|------|------|
| 查询列表 | GET | `/api/{resource}/list` | `GET /api/project/list` |
| 查询详情 | GET | `/api/{resource}/{id}` | `GET /api/project/1` |
| 创建 | POST | `/api/{resource}` | `POST /api/project` |
| 更新 | PUT | `/api/{resource}/{id}` | `PUT /api/project/1` |
| 删除 | DELETE | `/api/{resource}/{id}` | `DELETE /api/project/1` |

### 4.2 路径规范

- 全小写，用短横线分隔：`/api/project-user`
- 名词用复数：`/api/projects`
- 嵌套资源：`/api/projects/{id}/configs`

### 4.3 分页参数

统一使用 `page` 和 `size`：

```
GET /api/project/list?page=1&size=10
```

### 4.4 响应格式

统一响应格式：

```json
{
  "code": 200,
  "data": { ... },
  "message": "success",
  "timestamp": 1717500000000
}
```

分页响应：

```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  },
  "message": "success"
}
```

### 4.5 HTTP 状态码

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 正常响应 |
| 400 | 请求错误 | 参数校验失败 |
| 401 | 未登录 | Token 过期或无效 |
| 403 | 无权限 | 角色权限不足 |
| 404 | 不存在 | 资源未找到 |
| 500 | 服务器错误 | 未捕获异常 |

---

## 五、异常处理规范

### 5.1 后端异常处理

- 使用统一异常处理器 `GlobalExceptionHandler`
- 业务异常使用 `BusinessException`，携带错误码和消息
- 不吞异常，不打印无关堆栈

```java
throw BusinessException.notFound("项目不存在");
throw BusinessException.badRequest("项目名称不能为空");
```

### 5.2 前端异常处理

- Axios 拦截器统一处理 HTTP 错误
- 业务错误弹出 `ElMessage.error()`
- 401 跳转登录页，清除 Token

---

## 六、数据库规范

### 6.1 编码规范（重要）

**问题**：Docker MySQL 客户端默认编码是 latin1，通过命令行执行中文 SQL 会导致乱码。

**规则**：
- 所有 SQL 文件**必须**以 `SET NAMES utf8mb4;` 开头
- **禁止**用 `docker exec mysql -e "INSERT ..."` 执行包含中文的 SQL
- 正确方式：先写入 `.sql` 文件，再用 `docker exec mysql -e "source /tmp/xxx.sql"` 执行
- 只读查询时使用 `docker exec mysql --default-character-set=utf8mb4`

**示例**：
```bash
# ❌ 错误 — 中文会乱码
docker exec exam-mysql mysql -uroot -proot123456 -e "INSERT INTO tool VALUES (1, 'paper', '试题工具')"

# ✅ 正确 — 写入文件后执行
echo "SET NAMES utf8mb4; INSERT INTO tool VALUES (1, 'paper', '试题工具');" > /tmp/fix.sql
docker cp fix.sql exam-mysql:/tmp/
docker exec exam-mysql mysql -uroot -proot123456 exam_platform -e "source /tmp/fix.sql"

# ✅ 正确 — 只读查询加编码参数
docker exec exam-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -e "SELECT * FROM tool"
```

### 6.2 表结构变更

- 表结构以 `docs/02-database-design.md` 为准
- **不可随意变更表结构**
- 如需变更，必须同步更新设计文档

### 6.2 字段规范

- 所有表必须有 `id`（主键）、`created_at`、`updated_at` 字段
- 使用逻辑删除：`deleted` 字段（0=正常，1=已删除）
- 时间字段使用 `DATETIME` 类型

### 6.3 索引规范

- 主键索引：`id`
- 外键字段必须建索引
- 高频查询字段建索引
- 命名：`idx_{表名}_{字段名}`

---

## 七、Git 规范

### 7.1 提交信息格式

```
<type>(<scope>): <description>

[可选正文]

[可选脚注]
```

### 7.2 Type 类型

| Type | 说明 |
|------|------|
| feat | 新功能 |
| fix | 修复 bug |
| docs | 文档变更 |
| style | 代码格式（不影响功能） |
| refactor | 重构（非新功能非修复） |
| test | 测试相关 |
| chore | 构建/工具变更 |

### 7.3 示例

```
feat(project): 添加项目列表查询接口

- 实现 ProjectController.listProjects()
- 编写集成测试并通过
```

---

## 八、文档规范

### 8.1 文档更新

- 设计变更必须同步更新 `docs/` 下对应文档
- 新增接口必须更新 `docs/05-api-design.md`
- 表结构变更必须更新 `docs/02-database-design.md`

### 8.2 CLAUDE.md 更新

- 每完成一个服务的测试，在「测试通过记录」登记
- 更新「当前开发状态」章节

---

## 九、前端开发规范

### 9.1 组件结构

```vue
<template>
  <!-- 模板 -->
</template>

<script setup lang="ts">
  // 逻辑
</script>

<style scoped lang="scss">
  /* 样式 */
</style>
```

### 9.2 状态管理

- 全局状态使用 Pinia（`stores/`）
- 页面状态使用 `ref` / `reactive`

### 9.3 样式规范

- 使用 CSS 变量（`var(--color-primary)`）
- 组件样式使用 `scoped`
- 响应式断点：768px（移动端）、1200px（桌面端）

---

## 十、安全规范

### 10.1 敏感信息

- 密码、Token 不打印到日志
- 配置文件中的密码使用环境变量
- 前端不存储明文密码

### 10.2 接口安全

- 需要登录的接口必须校验 Token
- 需要权限的接口必须校验角色
- 删除操作必须二次确认

---

*本文档随项目演进持续更新。*
