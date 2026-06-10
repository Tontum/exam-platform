# 当前阶段可测功能指南

> 2026-06-01 | exam-project-service 已上线，其他服务待开发

## 一、导入测试数据

在 IDEA 中打开 `sql/init/02-test-data.sql`，右键 → Run（或选择 exam_platform 数据库后执行）。如果之前已初始化过，用 INSERT IGNORE 不会冲突。

## 二、当前可用的后端 API（exam-project-service，端口 8087）

### 2.1 项目管理

```bash
# 查看项目列表（分页）
curl http://localhost:8087/api/project/list?page=1&size=10

# 按状态筛选
curl http://localhost:8087/api/project/list?status=1

# 按关键词搜索
curl "http://localhost:8087/api/project/list?keyword=河南"

# 查看项目详情
curl http://localhost:8087/api/project/1

# 创建项目（需要 creatorId，通过 Header 传入）
curl -X POST http://localhost:8087/api/project/1 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"name":"测试项目","description":"通过API创建的项目","province":"河南省","city":"郑州市"}'

# 编辑项目
curl -X PUT http://localhost:8087/api/project/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"修改后的项目名"}'

# 老师加入项目
curl -X POST http://localhost:8087/api/project/1/join \
  -H "X-User-Id: 12"

# 老师退出项目
curl -X DELETE http://localhost:8087/api/project/1/leave \
  -H "X-User-Id: 12"
```

### 2.2 配置管理

```bash
# 查看项目1中校长(role=2)的配置
curl "http://localhost:8087/api/project/1/config?role=2"

# 查看项目1中老师(role=3)的配置
curl "http://localhost:8087/api/project/1/config?role=3"

# 更新单条配置
curl -X PUT http://localhost:8087/api/project/1/config \
  -H "Content-Type: application/json" \
  -d '{"id":2,"isEnabled":1,"allowPublish":1,"allowDelete":1,"allowReview":1}'

# 批量更新配置
curl -X PUT http://localhost:8087/api/project/1/config/batch \
  -H "Content-Type: application/json" \
  -d '[{"id":2,"scorePerSubmit":10},{"id":3,"scorePerSubmit":8}]'
```

## 三、测试数据概览

导入后数据库中的数据：

| 表 | 已有条数 | 说明 |
|------|:---:|------|
| user | 14 | 1管理员 + 3校长 + 10老师，密码都是 123456 |
| project | 3 | 不同状态的项目 |
| project_user | 16 | 老师与项目的关联 |
| tool | 8 | 7个功能工具 + 统计工具 |
| paper | 9 | 2草稿 + 5已发布 + 2已截止 |
| question | 20 | 分布在 paper 1/2/3/7 中 |
| option | 56 | 所有选择题和判断题的选项 |
| response | 17 | 各种答题状态 |
| answer | 35 | 部分已作答/已批阅 |
| config | 20 | 项目1/2 各角色配置 |
| permission | 11 | 基础权限定义 |

## 四、测试角色说明

| 用户名 | 角色 | 可测试场景 |
|--------|------|-----------|
| admin / 123456 | 管理员(1) | 作为 creatorId 创建项目 |
| principal1 / 123456 | 校长(2) | teacher1-4 的批阅人 |
| teacher1 / 123456 | 老师(3) | 参加项目1+2，有多个答题记录 |
| teacher2 / 123456 | 老师(3) | 参加项目1+2，有待批阅记录 |

## 五、当前限制

1. **无登录认证** — Gateway 和 user-service 未开发，所有 API 直接访问（port 8087），userId 通过 X-User-Id Header 传入
2. **前端仍用 mock 数据** — 前端页面未对接真实 API，需手动 curl 测试
3. **exam-user/exam-paper/exam-answer 服务未开发** — 登录、试卷、答题等功能暂不可用

## 六、下一步开发

Phase 3: exam-user-service（登录 + 用户查询 + 老师列表），完成后即可打通登录→项目→答题的完整链路。
