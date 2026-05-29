# campus-ai-assistant API

统一返回：

```json
{"code":0,"message":"success","data":{}}
```

当前项目没有登录模块，接口暂不需要 Authorization Header。

## 推荐测试顺序

1. `GET /api/health`
2. `POST /api/kb` 创建知识库
3. `POST /api/kb/{knowledgeBaseId}/document` 新增文档并同步切分
4. `GET /api/kb/{knowledgeBaseId}/chunks` 查看切分结果
5. `POST /api/chat/ask` 提问
6. `GET /api/academic/student/{studentNo}/scores` 查询成绩

## 接口列表

### 健康检查

- 方法：`GET`
- 路径：`/api/health`
- 请求参数 JSON：无
- 返回示例：

```json
{"code":0,"message":"success","data":"ok"}
```

### 创建知识库

- 方法：`POST`
- 路径：`/api/kb`
- 请求参数 JSON：

```json
{"name":"Java 复习资料库","description":"Java 基础知识资料"}
```

- 返回示例：

```json
{"code":0,"message":"success","data":{"id":1,"name":"Java 复习资料库","description":"Java 基础知识资料"}}
```

### 知识库列表

- 方法：`GET`
- 路径：`/api/kb/list`
- 请求参数 JSON：无
- 返回示例：

```json
{"code":0,"message":"success","data":[{"id":1,"name":"Java 复习资料库","description":"Java 基础知识资料"}]}
```

### 知识库详情

- 方法：`GET`
- 路径：`/api/kb/{id}`
- 请求参数 JSON：无
- 返回示例：

```json
{"code":0,"message":"success","data":{"id":1,"name":"Java 复习资料库"}}
```

### 删除知识库

- 方法：`DELETE`
- 路径：`/api/kb/{id}`
- 请求参数 JSON：无
- 返回示例：

```json
{"code":0,"message":"success","data":"ok"}
```

### 新增文档并同步切分

- 方法：`POST`
- 路径：`/api/kb/{knowledgeBaseId}/document`
- 请求参数 JSON：

```json
{"title":"Java 集合基础","content":"ArrayList 底层基于动态数组，HashMap 使用哈希表保存键值对。"}
```

- 返回示例：

```json
{"code":0,"message":"success","data":{"documentId":1}}
```

### 新增文档并异步切分

- 方法：`POST`
- 路径：`/api/kb/{knowledgeBaseId}/document/upload`
- 请求参数 JSON：

```json
{"title":"Redis 复习","content":"Redis 常用于缓存、计数器、排行榜等场景。"}
```

- 返回示例：

```json
{"code":0,"message":"success","data":{"documentId":2,"status":"PROCESSING"}}
```

该接口会先保存 `kb_document`，再向 RabbitMQ 发送文档切分消息；RabbitMQ 不可用时降级为同步切分。

### 查询文档片段

- 方法：`GET`
- 路径：`/api/kb/{knowledgeBaseId}/chunks`
- 请求参数 JSON：无
- 返回示例：

```json
{"code":0,"message":"success","data":[{"id":1,"documentId":1,"knowledgeBaseId":1,"chunkIndex":0,"content":"ArrayList 底层基于动态数组","keywords":"arraylist,hashmap"}]}
```

### 删除文档

- 方法：`DELETE`
- 路径：`/api/kb/document/{documentId}`
- 请求参数 JSON：无
- 返回示例：

```json
{"code":0,"message":"success","data":"ok"}
```

### AI 问答

- 方法：`POST`
- 路径：`/api/chat/ask`
- 请求参数 JSON：

```json
{"userId":1,"question":"帮我解释 Java 集合怎么复习","knowledgeBaseId":1}
```

- 返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "answer": "根据知识库片段生成的模拟回答...",
    "sourceType": "RAG",
    "conversationId": 1,
    "promptPreview": "你是校园学习助手...",
    "matchedChunks": [{"id":1,"chunkIndex":0,"content":"ArrayList 底层基于动态数组"}]
  }
}
```

### 查询学生成绩

- 方法：`GET`
- 路径：`/api/academic/student/{studentNo}/scores`
- 请求参数 JSON：无
- 返回示例：

```json
{"code":0,"message":"success","data":[{"studentNo":"20230001","studentName":"李明","courseName":"Java 程序设计","score":88.5,"semester":"2024-2025-1"}]}
```

### 查询课程平均分

- 方法：`GET`
- 路径：`/api/academic/course/{courseId}/average`
- 请求参数 JSON：无
- 返回示例：

```json
{"code":0,"message":"success","data":{"courseId":1,"courseName":"Java 程序设计","averageScore":85.25}}
```
