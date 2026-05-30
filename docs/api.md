# Campus Knowledge Hub API 文档

**统一返回格式**：

```json
{"code":0,"message":"success","data":{}}
```

**认证说明**：所有接口（除 `/api/auth/login`、`/api/health`、Swagger）需要 `Authorization: Bearer <token>` Header。Token 通过登录接口获取。

---

## 1. 认证模块

### 登录

- `POST /api/auth/login`
- 请求体：

```json
{"username":"student","password":"123456"}
```

- 返回：

```json
{
  "code":0,
  "data":{
    "token":"eyJhbGciOi...",
    "userId":1,
    "username":"student",
    "nickname":"李同学",
    "role":"STUDENT"
  }
}
```

### 当前用户

- `GET /api/auth/me`
- Header: `Authorization: Bearer <token>`
- 返回：

```json
{"code":0,"data":{"userId":1,"username":"student","role":"STUDENT"}}
```

---

## 2. 学生端

### 学生首页

- `GET /api/student/dashboard`
- 返回：可用知识库数、问答次数、文档总数、最近知识库

### 学生可见知识库

- `GET /api/student/knowledge-bases`
- 返回：所有公开和课程知识库列表

---

## 3. 教师端

### 教师工作台

- `GET /api/teacher/dashboard`
- 返回：我的知识库列表、总文档数、处理中文档数、失败文档数

### 教师知识库

- `GET /api/teacher/knowledge-bases`
- 返回：教师自己的知识库列表

---

## 4. 知识库管理

### 创建知识库

- `POST /api/kb`
- 角色：TEACHER / ADMIN
- 请求体：

```json
{"name":"Java 复习资料库","description":"Java 基础知识","visibility":"PUBLIC"}
```

- visibility: PUBLIC / PRIVATE / COURSE_ONLY

### 知识库列表

- `GET /api/kb`
- 返回所有知识库

### 知识库详情

- `GET /api/kb/{id}`

### 删除知识库

- `DELETE /api/kb/{id}`
- 角色：TEACHER / ADMIN

---

## 5. 文档管理

### 录入文档（同步处理）

- `POST /api/kb/{knowledgeBaseId}/document`
- 角色：TEACHER / ADMIN
- 请求体：

```json
{"title":"Java 集合基础","content":"ArrayList 底层基于动态数组..."}
```

### 上传文件（异步处理）

- `POST /api/kb/{knowledgeBaseId}/document/upload`
- Content-Type: `multipart/form-data`
- 表单字段：`file`（仅支持 .txt 和 .md）
- 返回：

```json
{"code":0,"data":{"documentId":1,"status":"PROCESSING"}}
```

### 文档列表

- `GET /api/kb/{knowledgeBaseId}/documents`
- 返回包含 fileName、fileType、fileSize、status、chunkCount、processedAt 等信息

### 查看文档片段

- `GET /api/document/{documentId}/chunks`
- 返回该文档的所有 chunk

### 重新解析

- `POST /api/document/{documentId}/reprocess`
- 角色：TEACHER / ADMIN
- 将失败文档重新投递到 RabbitMQ 处理

### 删除文档

- `DELETE /api/kb/document/{documentId}`
- 角色：TEACHER / ADMIN

---

## 6. AI 问答

### 提问

- `POST /api/chat/ask`
- 请求体：

```json
{
  "userId": 1,
  "knowledgeBaseId": 1,
  "question": "Java 集合怎么复习"
}
```

- 返回：

```json
{
  "code": 0,
  "data": {
    "answer": "根据资料，复习 Java 集合应关注...",
    "sourceType": "RAG",
    "conversationId": 1,
    "promptPreview": "你是校园学习助手...（前500字）",
    "llmMode": "mock",
    "retrievalTimeMs": 15,
    "generationTimeMs": 3,
    "matchedChunks": [
      {
        "id": 1,
        "chunkIndex": 0,
        "content": "ArrayList 底层基于动态数组...",
        "documentTitle": "Java 集合基础",
        "score": 3
      }
    ]
  }
}
```

- **无命中保护**：当所有 chunk score = 0 时，answer = "当前知识库未找到相关内容"，不调用模型。

### 问答历史

- `GET /api/chat/history?userId={userId}`
- 返回全部问答记录（含 promptPreview、llmMode、耗时等信息）

---

## 7. 管理员端

### 系统看板

- `GET /api/admin/dashboard`
- 返回：用户数、知识库数、文档数、chunk 数、问答次数

### 用户列表

- `GET /api/admin/users`
- 返回所有用户（含 role 和 status）

### 知识库审计

- `GET /api/admin/knowledge-bases`
- 返回所有知识库

---

## 8. 系统

### 系统配置与状态

- `GET /api/system/config`
- 返回：LLM 模式、MySQL/Redis/RabbitMQ 连接状态

### 系统概览

- `GET /api/system/overview`
- 同 /config

---

## 9. 学业查询

### 学生成绩

- `GET /api/academic/student/{studentNo}/scores`

### 课程平均分

- `GET /api/academic/course/{courseId}/average`

---

## 10. 健康检查

- `GET /api/health`
- 无需认证
