# campus-ai-assistant 面试问答

每题按“考点、回答、不要、代码位置”准备。

## 项目介绍类

### 1. 这个项目解决什么问题？
- 面试官想考什么：能否用业务语言说明项目。
- 推荐回答：这是个人学习、复现与二次开发项目，面向校园资料问答和学业查询，做了知识库录入、chunk 切分、关键词检索、Prompt 拼接、LLM 调用和成绩查询。
- 不要怎么回答：不要说成公司内部系统或大型平台。
- 对应代码位置：`README.md`，`ChatController.java`，`KnowledgeBaseController.java`。

### 2. 项目核心链路是什么？
- 面试官想考什么：是否理解主流程。
- 推荐回答：提问进入 `ChatServiceImpl`，先路由问题类型，RAG 问题检索 chunks，构建 Prompt，调用 `LlmClient`，保存 `chat_record`，再写 Redis 缓存。
- 不要怎么回答：不要只说“调用大模型回答”。
- 对应代码位置：`service/impl/ChatServiceImpl.java`。

### 3. 为什么要抽象 LlmClient？
- 面试官想考什么：扩展意识。
- 推荐回答：把模型调用从业务流程中隔离，mock、本地调试、DeepSeek、Spring AI ChatClient 可以替换，不影响 Controller 和 Service 主流程。
- 不要怎么回答：不要说只是为了多写接口。
- 对应代码位置：`service/LlmClient.java`，`MockLlmClient.java`，`DeepSeekLlmClient.java`，`SpringAiChatClientLlmClient.java`。

### 4. 你做了哪些数据表？
- 面试官想考什么：数据库设计能力。
- 推荐回答：知识库、文档、文档片段、聊天记录、学生、课程、成绩七张表，SQL 下划线字段映射到 Java 驼峰字段。
- 不要怎么回答：不要忽略聊天记录和成绩查询。
- 对应代码位置：`scripts/init.sql`，`entity` 包。

### 5. 前端做了什么？
- 面试官想考什么：是否能演示。
- 推荐回答：Vue 3 前端用于创建知识库、录入文档、查看 chunks、提问、查看 matchedChunks 和 promptPreview，以及查询成绩。
- 不要怎么回答：不要说做了完整产品级前端。
- 对应代码位置：`frontend/src/App.vue`。

## RAG 类

### 6. RAG 在项目里怎么实现？
- 面试官想考什么：是否理解 RAG。
- 推荐回答：文档先切成 chunks，提问时用关键词匹配取 TopK，再把片段和问题拼进 Prompt，让模型基于片段回答。
- 不要怎么回答：不要说用了向量数据库，本项目没有实现。
- 对应代码位置：`RagServiceImpl.java`，`KeywordMatcher.java`。

### 7. 为什么先用关键词检索？
- 面试官想考什么：技术取舍。
- 推荐回答：关键词检索简单可控、便于本地运行和解释，后续可以替换为向量检索，但接口层不需要大改。
- 不要怎么回答：不要说关键词检索等同于向量检索。
- 对应代码位置：`RagService.java`，`RagServiceImpl.java`。

### 8. 文本怎么切分？
- 面试官想考什么：chunk 设计。
- 推荐回答：`TextChunker` 按固定长度/段落思路拆分文本，生成 chunkIndex 和 content，保存到 `kb_document_chunk`。
- 不要怎么回答：不要说直接把整篇文档塞进 Prompt。
- 对应代码位置：`util/TextChunker.java`，`KnowledgeBaseServiceImpl.java`。

### 9. matchedChunks 有什么用？
- 面试官想考什么：可解释性。
- 推荐回答：返回命中的片段，方便调试模型依据，也方便前端展示回答来源。
- 不要怎么回答：不要只返回最终答案。
- 对应代码位置：`vo/MatchedChunkVO.java`，`ChatResponseVO.java`。

### 10. 异步文档切分怎么做？
- 面试官想考什么：消息队列理解。
- 推荐回答：`document/upload` 保存文档后发送 RabbitMQ 消息，消费者读取 documentId 后切分；发送失败时降级同步切分。
- 不要怎么回答：不要说 RabbitMQ 是必须依赖，当前实现可降级。
- 对应代码位置：`KnowledgeBaseServiceImpl.java`，`DocumentProcessConsumer.java`。

## Prompt / 大模型 API 类

### 11. PromptBuilder 做什么？
- 面试官想考什么：Prompt 组织能力。
- 推荐回答：把问题、知识库片段和回答约束拼成统一 Prompt，避免在业务代码里散落字符串。
- 不要怎么回答：不要把 Prompt 写死在 Controller。
- 对应代码位置：`util/PromptBuilder.java`。

### 12. mock 模式有什么意义？
- 面试官想考什么：本地调试能力。
- 推荐回答：默认 mock 保证没有 API Key 时也能启动和演示完整链路，回答里能看出是“根据知识库片段生成的模拟回答”。
- 不要怎么回答：不要依赖真实 Key 才能运行。
- 对应代码位置：`MockLlmClient.java`，`application.yml`。

### 13. real 模式没有 Key 怎么处理？
- 面试官想考什么：配置健壮性。
- 推荐回答：启动不报错，只有真正调用 `DeepSeekLlmClient` 时返回明确错误，避免本地启动被外部配置阻断。
- 不要怎么回答：不要让项目启动阶段直接失败。
- 对应代码位置：`DeepSeekLlmClient.java`。

### 14. Spring AI ChatClient 怎么接入？
- 面试官想考什么：新框架适配方式。
- 推荐回答：新增 `SpringAiChatClientLlmClient` 实现 `LlmClient`，用 `ChatClient.Builder` 构造调用，`llm.mode=spring-ai` 时切换。
- 不要怎么回答：不要把业务代码和 Spring AI API 强耦合。
- 对应代码位置：`SpringAiChatClientLlmClient.java`，`ChatServiceImpl.java`。

### 15. promptPreview 为什么返回给前端？
- 面试官想考什么：调试意识。
- 推荐回答：便于查看模型实际收到的上下文和问题，排查检索片段不准或 Prompt 写法问题。
- 不要怎么回答：不要在生产环境暴露敏感 Prompt。
- 对应代码位置：`ChatResponseVO.java`，`frontend/src/App.vue`。

## Spring Boot / MySQL 类

### 16. MyBatis-Plus 映射怎么保证？
- 面试官想考什么：ORM 基础。
- 推荐回答：配置 `map-underscore-to-camel-case=true`，表字段下划线，Entity 字段驼峰，Mapper 继承 BaseMapper。
- 不要怎么回答：不要混用字段命名。
- 对应代码位置：`application.yml`，`mapper` 包。

### 17. chat_record 保存哪些内容？
- 面试官想考什么：审计和复盘。
- 推荐回答：保存 userId、question、answer、sourceType、matchedChunkIds 和 createdAt，便于后续查看问答历史。
- 不要怎么回答：不要只把答案存在 Redis。
- 对应代码位置：`entity/ChatRecord.java`，`ChatServiceImpl.java`。

### 18. 学业查询是真查库吗？
- 面试官想考什么：是否写死数据。
- 推荐回答：通过 Student、Course、Score 的 Mapper 查询数据库，不是写死返回。
- 不要怎么回答：不要写假数据冒充业务查询。
- 对应代码位置：`AcademicServiceImpl.java`。

### 19. 统一异常怎么处理？
- 面试官想考什么：工程实践。
- 推荐回答：业务异常用 `BusinessException` 携带 `ErrorCode`，`GlobalExceptionHandler` 转统一返回结构。
- 不要怎么回答：不要在 Controller 到处 try-catch。
- 对应代码位置：`common` 包。

### 20. Docker Compose 包含什么？
- 面试官想考什么：本地运行能力。
- 推荐回答：提供 MySQL、Redis、RabbitMQ，便于本地启动依赖；后端仍通过环境变量读取连接信息。
- 不要怎么回答：不要说前后端都已经容器化部署，本项目只是依赖编排。
- 对应代码位置：`docker-compose.yml`。

## Redis / 异常处理 / 工程实践类

### 21. Redis 缓存了什么？
- 面试官想考什么：缓存边界。
- 推荐回答：知识库列表、高频问答和会话上下文；Redis 失败时不影响主流程。
- 不要怎么回答：不要把 Redis 当主数据库。
- 对应代码位置：`ChatServiceImpl.java`，`KnowledgeBaseServiceImpl.java`。

### 22. FAQ 缓存 key 怎么设计？
- 面试官想考什么：key 设计。
- 推荐回答：用知识库 id 和问题 hash 组成 `chat:faq:*`，避免不同知识库同问题冲突。
- 不要怎么回答：不要只用问题文本做 key。
- 对应代码位置：`ChatServiceImpl.java`。

### 23. 会话上下文怎么保存？
- 面试官想考什么：Redis 数据结构。
- 推荐回答：用 Redis List 保存最近几轮 Q/A，并设置过期时间，作为轻量上下文缓存。
- 不要怎么回答：不要无限增长。
- 对应代码位置：`ChatServiceImpl#appendContext`。

### 24. CI 做了什么？
- 面试官想考什么：基础工程化。
- 推荐回答：GitHub Actions 分别跑后端 Maven compile 和前端 npm build，保证提交后能做基础验证。
- 不要怎么回答：不要说做了完整发布流水线。
- 对应代码位置：`.github/workflows/ci.yml`。

### 25. 这个项目还能怎么扩展？
- 面试官想考什么：边界感。
- 推荐回答：可以把 `RagService` 替换为向量检索，完善登录和权限，补充文档上传解析，但当前项目保持简单可运行。
- 不要怎么回答：不要承诺已经实现向量数据库和复杂 Agent。
- 对应代码位置：`RagService.java`，`docs/rag-flow.md`。
