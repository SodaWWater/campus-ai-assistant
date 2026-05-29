# RAG 流程

```mermaid
sequenceDiagram
  participant U as 用户
  participant C as ChatController
  participant S as ChatServiceImpl
  participant R as RagServiceImpl
  participant P as PromptBuilder
  participant L as LlmClient
  participant DB as MySQL
  participant Redis as Redis
  U->>C: POST /api/chat/ask
  C->>S: ask(request)
  S->>Redis: 读取 FAQ 缓存
  S->>R: retrieveTopK(kbId, question, 3)
  R->>DB: 查询 kb_document_chunk
  R-->>S: matchedChunks
  S->>P: buildRagPrompt(question, chunks)
  S->>L: generate(prompt)
  S->>DB: 保存 chat_record
  S->>Redis: 写入 FAQ 缓存和上下文
  S-->>U: answer + matchedChunks + promptPreview
```

当前实现是关键词 TopK 检索，不接向量数据库。这样便于本地运行和面试讲解，后续可以把 `RagService` 中的检索实现替换为向量检索。

## 文档切分

- 同步接口：`POST /api/kb/{knowledgeBaseId}/document`
- 异步接口：`POST /api/kb/{knowledgeBaseId}/document/upload`
- 切分工具：`TextChunker`
- 消息消费者：`DocumentProcessConsumer`
