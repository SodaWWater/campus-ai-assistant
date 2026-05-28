# API 文档

本项目当前没有登录模块，所有接口暂不需要 Authorization Header。

统一返回格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 健康检查

- 接口名称：健康检查
- 请求路径：`/api/health`
- 请求方法：`GET`
- 请求参数 JSON：无

返回示例：

```json
{"code":0,"message":"success","data":"ok"}
```

## 创建知识库

- 接口名称：创建知识库
- 请求路径：`/api/kb`
- 请求方法：`POST`

请求参数 JSON：

```json
{"name":"Java 复习资料库","description":"Java 基础知识资料"}
```

返回示例：

```json
{"code":0,"message":"success","data":{"id":1,"name":"Java 复习资料库","description":"Java 基础知识资料"}}
```

## 查询知识库列表

- 接口名称：查询知识库列表
- 请求路径：`/api/kb/list`
- 请求方法：`GET`
- 请求参数 JSON：无

返回示例：

```json
{"code":0,"message":"success","data":[]}
```

## 查询知识库详情

- 接口名称：查询知识库详情
- 请求路径：`/api/kb/{id}`
- 请求方法：`GET`
- 请求参数 JSON：无

返回示例：

```json
{"code":0,"message":"success","data":{"id":1,"name":"Java 复习资料库"}}
```

## 删除知识库

- 接口名称：删除知识库
- 请求路径：`/api/kb/{id}`
- 请求方法：`DELETE`
- 请求参数 JSON：无

返回示例：

```json
{"code":0,"message":"success","data":"ok"}
```

## 新增文档

- 接口名称：新增文档并切分 chunk
- 请求路径：`/api/kb/{knowledgeBaseId}/document`
- 请求方法：`POST`

请求参数 JSON：

```json
{"title":"Java 集合基础","content":"ArrayList 底层基于动态数组，HashMap 使用哈希表保存键值对。"}
```

返回示例：

```json
{"code":0,"message":"success","data":{"documentId":1}}
```

## 查询文档片段

- 接口名称：查询文档片段
- 请求路径：`/api/kb/{knowledgeBaseId}/chunks`
- 请求方法：`GET`
- 请求参数 JSON：无

返回示例：

```json
{"code":0,"message":"success","data":[{"id":1,"documentId":1,"knowledgeBaseId":1,"chunkIndex":0,"content":"..."}]}
```

## 删除文档

- 接口名称：删除文档
- 请求路径：`/api/kb/document/{documentId}`
- 请求方法：`DELETE`
- 请求参数 JSON：无

返回示例：

```json
{"code":0,"message":"success","data":"ok"}
```

## AI 问答

- 接口名称：AI 问答
- 请求路径：`/api/chat/ask`
- 请求方法：`POST`

请求参数 JSON：

```json
{"userId":1,"question":"帮我解释 Java 集合怎么复习","knowledgeBaseId":1}
```

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "answer": "根据知识库片段生成的模拟回答...",
    "sourceType": "RAG",
    "conversationId": 1,
    "matchedChunks": [{"id":1,"chunkIndex":0,"content":"..."}]
  }
}
```

## 查询学生成绩

- 接口名称：查询学生成绩
- 请求路径：`/api/academic/student/{studentNo}/scores`
- 请求方法：`GET`
- 请求参数 JSON：无

返回示例：

```json
{"code":0,"message":"success","data":[{"studentNo":"20230001","studentName":"李明","courseName":"Java 程序设计","score":88.5}]}
```

## 查询课程平均分

- 接口名称：查询课程平均分
- 请求路径：`/api/academic/course/{courseId}/average`
- 请求方法：`GET`
- 请求参数 JSON：无

返回示例：

```json
{"code":0,"message":"success","data":{"courseId":1,"courseName":"Java 程序设计","averageScore":85.25}}
```

## 推荐测试顺序

1. `GET /api/health`
2. `POST /api/kb` 创建知识库
3. `POST /api/kb/{knowledgeBaseId}/document` 新增文档
4. `GET /api/kb/{knowledgeBaseId}/chunks` 查看切分结果
5. `POST /api/chat/ask` 提问
6. `GET /api/academic/student/{studentNo}/scores` 查询成绩
