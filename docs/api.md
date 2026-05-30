# Campus Knowledge Hub — API 接口文档

**统一返回格式**：

```json
{"code": 0, "message": "success", "data": {}}
```

**认证说明**：除 `/api/auth/login`、`/api/health`、Swagger 页面外，所有接口需携带 `Authorization: Bearer <token>` Header。Token 通过登录接口获取，有效期 24 小时。

**角色说明**：
- `STUDENT` — 学生，可浏览知识库、对话式提问、查看学业成绩
- `TEACHER` — 教师，可管理知识库、上传文档、查看处理状态
- `ADMIN` — 管理员，可管理用户、全量知识库审计、系统监控（ADMIN 可访问所有接口）

---

## 1. 认证模块

### 登录

- `POST /api/auth/login`
- 无需认证
- 请求体：

```json
{
  "username": "student",
  "password": "123456"
}
```

- 返回：

```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "student",
    "nickname": "李同学",
    "role": "STUDENT"
  }
}
```

### 获取当前用户

- `GET /api/auth/me`
- 返回 JWT 中携带的用户信息：

```json
{
  "code": 0,
  "data": {
    "userId": 1,
    "username": "student",
    "role": "STUDENT"
  }
}
```

---

## 2. 学生端

### 学生首页

- `GET /api/student/dashboard`
- 角色：STUDENT

返回：知识库总数、问答次数、文档总数、最近 5 个知识库

```json
{
  "code": 0,
  "data": {
    "kbCount": 3,
    "chatCount": 15,
    "docCount": 8,
    "recentKnowledgeBases": [
      { "id": 1, "name": "Java 复习资料库", "documentCount": 5, "chunkCount": 32 }
    ]
  }
}
```

### 学生可见知识库

- `GET /api/student/knowledge-bases`
- 角色：STUDENT

返回所有公开和课程可见的知识库列表。

### 学业成绩查询

- `GET /api/student/academic`
- 角色：STUDENT

**JWT 自动识别当前登录学生**，无需手动传入学号。通过 `sys_user.id → student.user_id` 关联查找。

返回：

```json
{
  "code": 0,
  "data": {
    "studentNo": "20240001",
    "name": "李同学",
    "major": "计算机科学与技术",
    "grade": "2024",
    "averageScore": 88.5,
    "totalCredit": 16.0,
    "courseCount": 5,
    "scores": [
      {
        "courseName": "Java 程序设计",
        "courseCode": "CS201",
        "credit": 4.0,
        "score": 92,
        "semester": "2024-2025-1"
      },
      {
        "courseName": "数据结构",
        "courseCode": "CS202",
        "credit": 3.0,
        "score": 85,
        "semester": "2024-2025-1"
      }
    ]
  }
}
```

若当前用户未关联学生记录，返回：
```json
{"code": 0, "data": {"message": "当前账号未关联学生信息"}}
```

---

## 3. 教师端

### 教师工作台

- `GET /api/teacher/dashboard`
- 角色：TEACHER

返回：知识库总数、文档总数、处理中文档数、失败文档数、知识库列表

```json
{
  "code": 0,
  "data": {
    "kbCount": 2,
    "docCount": 8,
    "processingCount": 1,
    "failedCount": 0,
    "knowledgeBases": [...]
  }
}
```

### 教师知识库列表

- `GET /api/teacher/knowledge-bases`
- 角色：TEACHER

返回教师所有知识库。

---

## 4. 知识库管理

### 创建知识库

- `POST /api/kb`
- 角色：TEACHER / ADMIN
- 请求体：

```json
{
  "name": "Java 复习资料库",
  "description": "面向 Java 课程的期末复习资料库",
  "visibility": "PUBLIC"
}
```

`visibility` 可选值：`PUBLIC` / `PRIVATE` / `COURSE_ONLY`

### 知识库列表

- `GET /api/kb`
- 角色：所有登录用户

返回全部知识库。

### 知识库详情

- `GET /api/kb/{id}`
- 角色：所有登录用户

### 更新知识库

- `PUT /api/kb/{id}`
- 角色：TEACHER / ADMIN
- 请求体：

```json
{
  "name": "Java 进阶复习",
  "description": "更新后的描述",
  "visibility": "PUBLIC"
}
```

### 删除知识库

- `DELETE /api/kb/{id}`
- 角色：TEACHER / ADMIN

同时删除该知识库下的所有文档和片段。

---

## 5. 文档管理

### 录入文档正文（同步处理）

- `POST /api/kb/{knowledgeBaseId}/document`
- 角色：TEACHER / ADMIN
- 请求体：

```json
{
  "title": "Java 集合基础知识",
  "content": "ArrayList 底层基于动态数组实现，LinkedList 基于双向链表..."
}
```

### 上传文件（异步处理）

- `POST /api/kb/{knowledgeBaseId}/document/upload`
- 角色：TEACHER / ADMIN
- Content-Type: `multipart/form-data`
- 表单字段：`file`
- 支持格式：`.txt` `.md` `.pdf` `.docx` `.doc`

上传后自动提取文本（PDFBox 处理 PDF，POI 处理 Word），保存文档元信息，立即同步切分并投递 RabbitMQ 异步处理。

返回：

```json
{
  "code": 0,
  "data": {
    "documentId": 15,
    "status": "PROCESSING"
  }
}
```

### 文档列表（按知识库）

- `GET /api/kb/{knowledgeBaseId}/documents`
- 角色：TEACHER / ADMIN

返回字段：id, title, fileName, fileType, fileSize, status, errorMessage, chunkCount, uploadedBy, processedAt, createdAt

### 查看文档片段

- `GET /api/document/{documentId}/chunks`
- 角色：所有登录用户

返回该文档的所有 chunk（chunkIndex, content, keywords）。

### 重新解析文档

- `POST /api/document/{documentId}/reprocess`
- 角色：TEACHER / ADMIN

将文档重置为 PROCESSING 并重新投递到 RabbitMQ。适用于处理失败的文档。

### 删除文档

- `DELETE /api/kb/document/{documentId}`
- 角色：TEACHER / ADMIN

同时删除文档的 chunk 和源文件，更新知识库计数。

---

## 6. 对话管理

### 获取对话列表

- `GET /api/conversations`
- 角色：所有登录用户

返回当前用户的所有对话，按 `updatedAt` 降序排列。

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "title": "Java 集合怎么复习",
      "knowledgeBaseId": 1,
      "createdAt": "2026-05-30T10:00:00",
      "updatedAt": "2026-05-30T10:05:00"
    }
  ]
}
```

### 创建新对话

- `POST /api/conversations`
- 角色：所有登录用户
- 请求体：

```json
{
  "title": "期末复习",
  "knowledgeBaseId": 1
}
```

### 删除对话

- `DELETE /api/conversations/{id}`
- 角色：所有登录用户

仅限删除自己创建的对话，会验证 `userId` 所有权。

---

## 7. AI 问答

### 发送消息（对话式 RAG）

- `POST /api/chat/ask`
- 角色：所有登录用户

userId 和 username 从 JWT 自动提取，无需手动传递。

- 请求体：

```json
{
  "conversationId": 1,
  "knowledgeBaseId": 1,
  "question": "Java 集合框架有哪些核心接口？"
}
```

- 首次对话不传 `conversationId`，系统自动创建：
```json
{
  "knowledgeBaseId": 1,
  "question": "Java 是什么？"
}
```

- 返回：

```json
{
  "code": 0,
  "data": {
    "answer": "根据知识库资料，Java 集合框架主要包括以下核心接口：\n1. **Collection** — 单列集合的根接口...",
    "sourceType": "RAG",
    "messageId": 42,
    "conversationId": 1,
    "promptPreview": "你是校园学习助手...（前500字）",
    "llmMode": "real",
    "retrievalTimeMs": 15,
    "generationTimeMs": 1203,
    "matchedChunks": [
      {
        "id": 10,
        "chunkIndex": 0,
        "content": "Java 集合框架包括 Collection、List、Set、Map...",
        "documentTitle": "Java 集合基础",
        "score": 5
      },
      {
        "id": 12,
        "chunkIndex": 2,
        "content": "ArrayList 底层基于动态数组...",
        "documentTitle": "集合源码分析",
        "score": 3
      }
    ]
  }
}
```

**问答流程**：
1. 用户发送问题 → 自动获取/创建对话
2. 加载对话历史（Redis → MySQL 回退，最近 10 轮）
3. QuestionRouter 判断问题类型
4. RAG 检索 TopK 片段（含文档标题 + 得分）
5. PromptBuilder 构造完整 Prompt（角色 + 历史 + 参考资料 + 问题）
6. 始终调用 LLM 生成回答（不因无命中跳过）
7. 保存 `chat_record` → 写入 Redis 对话历史 → 返回

**RAG 角色定位**：检索结果为"参考资料"而非"唯一答案来源"，LLM 会综合上下文和自身知识给出更完整的回答。

### 获取对话消息

- `GET /api/chat/messages?conversationId=1`
- 角色：所有登录用户（仅限自己的对话）

返回该对话下所有问答记录（按时间正序）。

### 查询个人问答历史（跨对话）

- `GET /api/chat/history`
- 角色：所有登录用户

返回当前用户最近 50 条问答记录（按时间倒序，跨所有对话）。

---

## 8. 管理员端

### 管理员看板

- `GET /api/admin/dashboard`
- 角色：ADMIN

返回：

```json
{
  "code": 0,
  "data": {
    "userCount": 5,
    "kbCount": 3,
    "docCount": 12,
    "chunkCount": 85,
    "chatCount": 42,
    "processingCount": 0,
    "doneCount": 11,
    "failedCount": 1
  }
}
```

### 用户管理

#### 用户列表

- `GET /api/admin/users`
- 角色：ADMIN

返回所有用户（含 role、status、createdAt）。

#### 创建用户

- `POST /api/admin/users`
- 角色：ADMIN
- 请求体：

```json
{
  "username": "newstudent",
  "password": "123456",
  "nickname": "新同学",
  "role": "STUDENT",
  "status": "ENABLED"
}
```

创建 STUDENT 角色用户时，自动在 `student` 表中同步创建记录（自动生成学号）。

#### 编辑用户

- `PUT /api/admin/users/{id}`
- 角色：ADMIN

可修改 nickname、role、status、password（留空不修改）。改为 STUDENT 时自动同步学生表。

#### 删除用户

- `DELETE /api/admin/users/{id}`
- 角色：ADMIN

不能删除自己。删除 STUDENT 用户时同步删除 `student` 表关联记录。

#### 切换用户状态

- `PUT /api/admin/users/{id}/status`
- 角色：ADMIN

切换启用/禁用（ENABLED ↔ DISABLED）。

### 知识库审计

#### 全部知识库

- `GET /api/admin/knowledge-bases`
- 角色：ADMIN

返回所有知识库（含 ownerName、visibility、documentCount、chunkCount）。

#### 编辑知识库

- `PUT /api/admin/knowledge-bases/{id}`
- 角色：ADMIN

可修改 name、description、visibility。

#### 删除知识库

- `DELETE /api/admin/knowledge-bases/{id}`
- 角色：ADMIN

### 文档任务管理

#### 全部文档列表

- `GET /api/admin/documents`
- 角色：ADMIN

返回所有文档（按创建时间降序），含 fileName、fileType、fileSize、status、chunkCount、uploadedBy 等。

#### 重新解析

- `POST /api/admin/document/{documentId}/reprocess`
- 角色：ADMIN

#### 删除文档

- `DELETE /api/admin/document/{documentId}`
- 角色：ADMIN

---

## 9. 系统

### 系统配置与状态

- `GET /api/system/config`
- 角色：所有登录用户

返回：

```json
{
  "code": 0,
  "data": {
    "llmMode": "real",
    "provider": "deepseek",
    "mysql": true,
    "redis": true,
    "rabbitmq": true
  }
}
```

`llmMode` 取值：`mock`（模拟） / `real`（DeepSeek API） / `spring-ai`（Spring AI）

### 系统概览

- `GET /api/system/overview`
- 角色：所有登录用户

同 `/api/system/config`。

---

## 10. 健康检查

- `GET /api/health`
- 无需认证

返回：`{"status": "UP"}`

---

## API 角色权限速查表

| 接口 | STUDENT | TEACHER | ADMIN |
|------|:-------:|:-------:|:-----:|
| `POST /api/auth/login` | ✅ | ✅ | ✅ |
| `GET /api/auth/me` | ✅ | ✅ | ✅ |
| `GET /api/student/dashboard` | ✅ | ❌ | ✅ |
| `GET /api/student/knowledge-bases` | ✅ | ❌ | ✅ |
| `GET /api/student/academic` | ✅ | ❌ | ✅ |
| `GET /api/teacher/dashboard` | ❌ | ✅ | ✅ |
| `GET /api/teacher/knowledge-bases` | ❌ | ✅ | ✅ |
| `GET /api/kb` | ✅ | ✅ | ✅ |
| `GET /api/kb/{id}` | ✅ | ✅ | ✅ |
| `POST /api/kb` | ❌ | ✅ | ✅ |
| `PUT /api/kb/{id}` | ❌ | ✅ | ✅ |
| `DELETE /api/kb/{id}` | ❌ | ✅ | ✅ |
| `POST /api/kb/{id}/document` | ❌ | ✅ | ✅ |
| `POST /api/kb/{id}/document/upload` | ❌ | ✅ | ✅ |
| `GET /api/kb/{id}/documents` | ❌ | ✅ | ✅ |
| `GET /api/document/{id}/chunks` | ✅ | ✅ | ✅ |
| `POST /api/document/{id}/reprocess` | ❌ | ✅ | ✅ |
| `DELETE /api/kb/document/{id}` | ❌ | ✅ | ✅ |
| `GET /api/conversations` | ✅ | ✅ | ✅ |
| `POST /api/conversations` | ✅ | ✅ | ✅ |
| `DELETE /api/conversations/{id}` | ✅ | ✅ | ✅ |
| `POST /api/chat/ask` | ✅ | ✅ | ✅ |
| `GET /api/chat/messages` | ✅ | ✅ | ✅ |
| `GET /api/chat/history` | ✅ | ✅ | ✅ |
| `GET /api/admin/dashboard` | ❌ | ❌ | ✅ |
| `GET /api/admin/users` | ❌ | ❌ | ✅ |
| `POST /api/admin/users` | ❌ | ❌ | ✅ |
| `PUT /api/admin/users/{id}` | ❌ | ❌ | ✅ |
| `DELETE /api/admin/users/{id}` | ❌ | ❌ | ✅ |
| `PUT /api/admin/users/{id}/status` | ❌ | ❌ | ✅ |
| `GET /api/admin/knowledge-bases` | ❌ | ❌ | ✅ |
| `PUT /api/admin/knowledge-bases/{id}` | ❌ | ❌ | ✅ |
| `DELETE /api/admin/knowledge-bases/{id}` | ❌ | ❌ | ✅ |
| `GET /api/admin/documents` | ❌ | ❌ | ✅ |
| `POST /api/admin/document/{id}/reprocess` | ❌ | ❌ | ✅ |
| `DELETE /api/admin/document/{id}` | ❌ | ❌ | ✅ |
| `GET /api/system/config` | ✅ | ✅ | ✅ |
| `GET /api/system/overview` | ✅ | ✅ | ✅ |
