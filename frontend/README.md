# campus-ai-assistant frontend

Vue 3 + Vite + Element Plus 前端，用于演示校园资料智能问答项目。

## 功能

- 知识库创建、列表、删除
- 文档同步切分与 RabbitMQ 异步入队
- chunk 查看、matchedChunks 展示、Prompt 预览
- AI 问答与学业成绩查询

## 启动

先启动后端 `campus-ai-assistant`，再执行：

```powershell
npm install
npm run dev
```

默认访问：

- 前端：http://localhost:5173
- 后端代理：http://localhost:8081

Vite 会把 `/api` 代理到后端。
