# 功能规格：项目增删改查

## AC-001：项目列表查询（分页）

### 验收条件

- GIVEN 系统中存在多个项目
- WHEN 管理员访问项目管理页面
- THEN 显示项目列表，包含 ID、名称、描述、状态、省份、城市、创建时间
- AND 支持分页，默认每页 15 条
- AND 显示总条数

### API 规格

```
GET /api/project/list?page=1&size=15

Response:
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "name": "2025年度河南省教师素质提升培训",
        "description": "全省中小学教师在线考核",
        "creatorId": 1,
        "province": "河南省",
        "city": "郑州市",
        "status": 1,
        "createdAt": "2025-01-15 10:00:00",
        "updatedAt": "2025-01-15 10:00:00"
      }
    ],
    "total": 10,
    "size": 15,
    "current": 1,
    "pages": 1
  },
  "message": "success"
}
```

## AC-002：项目创建

### 验收条件

- GIVEN 管理员在项目管理页面
- WHEN 点击"创建项目"按钮
- THEN 弹出创建项目对话框
- AND 管理员填写项目名称（必填）、描述、省份、城市
- WHEN 点击"创建"按钮
- THEN 项目保存到数据库
- AND 对话框关闭
- AND 项目列表刷新，显示新创建的项目
- AND 显示成功提示

### API 规格

```
POST /api/project/create

Request:
{
  "name": "新项目名称",
  "description": "项目描述",
  "province": "河南省",
  "city": "郑州市"
}

Response:
{
  "code": 200,
  "data": {
    "id": 11,
    "name": "新项目名称",
    "description": "项目描述",
    "creatorId": 1,
    "province": "河南省",
    "city": "郑州市",
    "status": 0,
    "createdAt": "2025-03-20 14:30:00",
    "updatedAt": "2025-03-20 14:30:00"
  },
  "message": "项目创建成功"
}
```

## AC-003：项目编辑

### 验收条件

- GIVEN 管理员在项目管理页面
- WHEN 点击某项目的"编辑"按钮
- THEN 弹出编辑项目对话框
- AND 对话框中显示该项目的当前信息
- AND 管理员修改项目信息
- WHEN 点击"保存"按钮
- THEN 项目信息更新到数据库
- AND 对话框关闭
- AND 项目列表刷新，显示更新后的信息
- AND 显示成功提示

### API 规格

```
PUT /api/project/update/{id}

Request:
{
  "name": "更新后的项目名称",
  "description": "更新后的描述",
  "province": "河南省",
  "city": "洛阳市"
}

Response:
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "更新后的项目名称",
    "description": "更新后的描述",
    "creatorId": 1,
    "province": "河南省",
    "city": "洛阳市",
    "status": 1,
    "createdAt": "2025-01-15 10:00:00",
    "updatedAt": "2025-03-20 15:00:00"
  },
  "message": "项目更新成功"
}
```

## AC-004：项目删除

### 验收条件

- GIVEN 管理员在项目管理页面
- WHEN 点击某项目的"删除"按钮
- THEN 弹出确认删除对话框
- AND 显示警告信息"确认删除该项目？此操作不可撤销。"
- WHEN 管理员点击"确认"按钮
- THEN 项目从数据库中删除（逻辑删除）
- AND 项目列表刷新，该项目不再显示
- AND 显示成功提示
- WHEN 管理员点击"取消"按钮
- THEN 对话框关闭，不执行删除操作

### API 规格

```
DELETE /api/project/delete/{id}

Response:
{
  "code": 200,
  "data": null,
  "message": "项目删除成功"
}
```

## AC-005：项目状态管理

### 验收条件

- GIVEN 项目状态定义：0=未开始、1=进行中、2=已结束
- WHEN 管理员创建项目
- THEN 项目默认状态为"未开始"（0）
- AND 前端正确显示状态标签

### 状态显示规则

| 状态值 | 显示文本 | 标签类型 |
|--------|----------|----------|
| 0 | 未开始 | info |
| 1 | 进行中 | success |
| 2 | 已结束 | warning |
