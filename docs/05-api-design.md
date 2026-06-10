# 05 - API 接口设计规范

> 本文档定义教师培训在线考试系统各微服务的 RESTful API 接口，包含路径、方法、参数、返回值。开发时 Controller 必须以本文档为准。

---

## 1. 通用规范

### 1.1 基础路径

| 环境 | 基础 URL |
|------|----------|
| 本地开发 | `http://localhost:8080` |
| API 网关 | `http://gateway:8080` |

### 1.2 统一响应体

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1715400000000
}
```

**状态码约定：**
- 200：成功
- 400：参数校验失败
- 401：未登录 / Token 过期
- 403：无权限
- 404：资源不存在
- 500：服务器内部错误

### 1.3 鉴权

所有接口（除登录）需在 Header 中携带：`Authorization: Bearer {token}`

### 1.4 分页请求/响应

请求参数：`page`（页码，从1开始）、`size`（每页条数，默认20）

响应体中分页数据包裹在 `PageResult<T>` 中：
```json
{
  "code": 200,
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

---

## 2. 用户服务 (exam-user-service)

**基础路径：** `/api/user`

### 2.1 登录

| 项目 | 内容 |
|------|------|
| 路径 | `POST /api/user/login` |
| 说明 | 用户登录，返回 JWT Token |
| 请求体 | `{ "username": "zhangsan", "password": "123456" }` |
| 响应 | `{ "token": "xxx", "userId": 1, "role": 2, "realName": "张三" }` |

### 2.2 获取当前用户信息

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/user/current` |
| 说明 | 获取当前登录用户完整信息（含角色、层级、所属项目列表） |

### 2.3 按层级查询老师列表

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/user/teachers` |
| 说明 | 校长发布试卷时查询可分发老师列表 |
| 参数 | `province`, `city`, `county`, `school`（均为可选，用于层级过滤） |
| 响应 | 分页返回 `List<UserDTO>` |

### 2.4 查询老师所属项目列表

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/user/projects` |
| 说明 | 当前登录老师查看可参加的项目列表 |

---

## 3. 试卷服务 (exam-paper-service)

**基础路径：** `/api/paper`

### 3.1 创建试卷

| 项目 | 内容 |
|------|------|
| 路径 | `POST /api/paper` |
| 说明 | 校长创建试卷（状态=草稿） |
| 权限 | 校长(2)、管理员(1) |
| 请求体 | `PaperCreateDTO`（title, description, totalScore, passScore, durationMinutes, projectId, 层级字段） |

### 3.2 编辑试卷基本信息

| 项目 | 内容 |
|------|------|
| 路径 | `PUT /api/paper/{paperId}` |
| 说明 | 仅草稿状态可编辑 |
| 请求体 | `PaperUpdateDTO` |

### 3.3 添加题目

| 项目 | 内容 |
|------|------|
| 路径 | `POST /api/paper/{paperId}/question` |
| 说明 | 向试卷中添加一道题目 |
| 请求体 | `QuestionCreateDTO`（title, questionType, score, isRequired, sortOrder, 含 options 列表） |

### 3.4 编辑题目

| 项目 | 内容 |
|------|------|
| 路径 | `PUT /api/paper/{paperId}/question/{questionId}` |
| 说明 | 修改题目内容和选项 |

### 3.5 删除题目

| 项目 | 内容 |
|------|------|
| 路径 | `DELETE /api/paper/{paperId}/question/{questionId}` |

### 3.6 查询试卷详情

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/paper/{paperId}` |
| 说明 | 返回试卷基本信息 + 题目列表 + 选项列表 |
| 响应 | `PaperDetailVO` |

### 3.7 分页查询试卷列表

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/paper/list` |
| 说明 | 校长查看自己发布的试卷列表 |
| 参数 | `page`, `size`, `status`（可选）, `keyword`（可选） |

### 3.8 发布试卷（核心接口）

| 项目 | 内容 |
|------|------|
| 路径 | `POST /api/paper/{paperId}/publish` |
| 说明 | 将试卷正式发布，发送 Kafka 异步分发消息 |
| 权限 | 校长(2) |
| 请求体 | `PublishDTO`（targetUserIds 或 层级筛选条件） |
| 响应 | `{ "message": "分发中", "paperId": 1 }` |
| 关键逻辑 | 校验 → 更新 paper.status=1 → 发 Kafka 消息 → 立即返回 |

### 3.9 截止试卷

| 项目 | 内容 |
|------|------|
| 路径 | `PUT /api/paper/{paperId}/close` |
| 说明 | 将试卷状态改为已截止，不再接受答题 |

### 3.10 删除试卷

| 项目 | 内容 |
|------|------|
| 路径 | `DELETE /api/paper/{paperId}` |
| 说明 | 逻辑删除，仅草稿状态可删 |

---

## 4. 答题服务 (exam-answer-service)

**基础路径：** `/api/answer`

### 4.1 查询老师试卷列表

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/answer/my-papers` |
| 说明 | 当前老师查看自己的试卷列表（从 response 表查） |
| 参数 | `status`（可选，0=未答题、1=正在答题、2=已提交、3=已批阅） |
| 分页 | 支持 |
| 响应 | `List<MyPaperVO>`（含试卷名、状态、得分、提交时间） |

### 4.2 开始答题（进入试卷）

| 项目 | 内容 |
|------|------|
| 路径 | `POST /api/answer/{responseId}/start` |
| 说明 | 老师点击"开始答题"，更新 response.status=1 |
| 前置条件 | response.user_id 为当前用户，status 为 0 或 1 |

### 4.3 获取试卷答题内容

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/answer/{responseId}/questions` |
| 说明 | 返回试卷题目和选项（不含正确答案），若已有答案则一并返回 |
| 响应 | `PaperAnswerVO`（含 questions 列表，每题含 options，若已作答含 answerContent） |

### 4.4 提交单题答案（核心接口）

| 项目 | 内容 |
|------|------|
| 路径 | `POST /api/answer/{responseId}/question/{questionId}` |
| 说明 | 老师提交一道题的答案，先写入 Redis，再异步落库 |
| 权限 | 当前用户为 response.user_id |
| 请求体 | `{ "answerContent": "A", "durationSeconds": 15 }` |
| 关键逻辑 | 写入 Redis → 更新 Redis 统计数据 → 返回成功 |

### 4.5 自动保存进度（定时调用）

| 项目 | 内容 |
|------|------|
| 路径 | `POST /api/answer/{responseId}/save-progress` |
| 说明 | 前端每 5 秒调用一次，确保 response.status 保持为 1（正在答题） |
| 关键逻辑 | 若用户主动提交(status=2)则不更新；否则 status → 1 |

### 4.6 提交整张试卷（核心接口）

| 项目 | 内容 |
|------|------|
| 路径 | `POST /api/answer/{responseId}/submit` |
| 说明 | 老师点击"提交试卷" |
| 权限 | 当前用户为 response.user_id |
| 关键逻辑 | 1. Redis 答案批量写入 answer 表 2. 客观题自动判分 3. response.status → 2, submit_time → NOW 4. 返回结果 |

### 4.7 查看答题结果

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/answer/{responseId}/result` |
| 说明 | 查看已批阅试卷的答题详情、每题得分、批阅评语 |

---

## 5. 批阅相关（复用 answer-service）

**基础路径：** `/api/review`

### 5.1 查询待批阅列表

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/review/pending` |
| 说明 | 校长查看某试卷下所有已提交待批阅的老师列表 |
| 参数 | `paperId`（必填） |
| 权限 | 校长(2)，且为 paper.publisher_id 或同层级权限 |

### 5.2 查看某老师答题详情（批阅用）

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/review/{responseId}/detail` |
| 说明 | 校长查看某位老师的完整答题内容，含客观题自动判分结果和正确答案 |
| 权限 | 校长(2) |

### 5.3 逐题批阅

| 项目 | 内容 |
|------|------|
| 路径 | `PUT /api/review/{responseId}/question/{questionId}/score` |
| 说明 | 校长给某道题打分和评语 |
| 请求体 | `{ "score": 5.0, "reviewComment": "回答不够完整" }` |

### 5.4 确认批阅完成

| 项目 | 内容 |
|------|------|
| 路径 | `PUT /api/review/{responseId}/complete` |
| 说明 | 校长确认批阅完成，汇总分数，更新 response 状态和判定 |
| 关键逻辑 | SUM(answer.score) → response.score → 判定 is_pass → status → 3 |

---

## 6. 统计服务 (exam-statistics-service)

**基础路径：** `/api/statistics`

### 6.1 试卷统计

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/statistics/paper/{paperId}` |
| 说明 | 某试卷的整体统计（平均分、合格率、各分数段分布） |

### 6.2 区域统计

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/statistics/region` |
| 说明 | 按省/市/县/校统计合格率 |
| 参数 | `paperId`, `level`（province/city/county/school） |

### 6.3 老师个人统计

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/statistics/teacher/{userId}` |
| 说明 | 某老师的所有试卷成绩和考核总分 |

---

## 7. 检索服务 (exam-search-service)

**基础路径：** `/api/search`

### 7.1 试题全文检索

| 项目 | 内容 |
|------|------|
| 路径 | `GET /api/search/question` |
| 说明 | 按关键词 + 筛选条件检索试题 |
| 参数 | `keyword`（全文检索）, `questionType`, `paperId` |

### 7.2 索引重建（管理用）

| 项目 | 内容 |
|------|------|
| 路径 | `POST /api/search/rebuild` |
| 说明 | 从 MySQL 全量重建 ES 索引 |
| 权限 | 管理员(1) |

---

## 8. 项目服务 (exam-project-service)

**基础路径：** `/api/project`

### 8.1 CRUD

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/project/list` | 项目列表（分页） |
| GET | `/api/project/{id}` | 项目详情 |
| POST | `/api/project` | 创建项目（管理员） |
| PUT | `/api/project/{id}` | 编辑项目 |

### 8.2 加入/退出项目

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/project/{id}/join` | 老师加入项目 |
| DELETE | `/api/project/{id}/leave` | 老师退出项目 |

### 8.3 项目配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/project/{id}/config` | 获取项目下某角色的工具配置列表 |
| PUT | `/api/project/{id}/config` | 更新配置（管理员） |

---

## 9. 接口开发优先级

| 优先级 | 接口组 | 说明 |
|--------|--------|------|
| P0 | 用户登录 + 试卷 CRUD + 题目管理 | 核心骨架，必须先完成 |
| P1 | 答题提交 + 自动批阅 + 试卷列表 | 核心业务闭环 |
| P2 | 试卷发布 + Kafka 分发 | 异步高并发场景 |
| P3 | 校长批阅 + 统计 | 管理端核心功能 |
| P4 | 检索 + 配置管理 + 项目服务 | 扩展功能 |
