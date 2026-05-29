# 学习路线

1. 看 `docs/api.md`，用 Swagger 或 curl 跑通健康检查、知识库、文档、问答、成绩查询。
2. 看 `scripts/init.sql` 和 Entity，理解表结构和字段映射。
3. 看 `KnowledgeBaseServiceImpl`，理解文档保存、切分、RabbitMQ 异步入口。
4. 看 `RagServiceImpl`、`KeywordMatcher`、`PromptBuilder`，理解 RAG 最小闭环。
5. 看 `ChatServiceImpl`，画出路由、检索、Prompt、LLM、保存聊天记录、Redis 缓存的顺序。
6. 切换 `llm.mode=mock`、`real`、`spring-ai`，理解三种调用方式差异。
7. 启动 `frontend`，通过页面演示知识库和问答流程。

## 7 天计划

- Day 1：跑通后端、导入 SQL。
- Day 2：理解知识库和文档切分。
- Day 3：理解 RAG 检索与 Prompt。
- Day 4：理解 LLM 客户端抽象和 Spring AI ChatClient。
- Day 5：理解 Redis 缓存和降级。
- Day 6：理解 RabbitMQ 异步切分。
- Day 7：用自己的话复述项目，并准备接口演示。
