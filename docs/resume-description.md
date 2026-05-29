# 简历描述

## 校园资料智能问答与学业助手系统

技术栈：Spring Boot 3、Java 21、MyBatis-Plus、MySQL、Redis、RabbitMQ、Spring AI ChatClient、DeepSeek API、Vue 3、Element Plus。

项目描述：个人学习、复现与二次开发项目，面向校园资料问答和学业查询场景，实现知识库录入、文本切分、关键词检索、Prompt 拼接、Mock/真实大模型接口切换、聊天记录保存和成绩查询，并提供 Vue 3 前端演示。

主要工作：

- 设计 `kb_knowledge_base`、`kb_document`、`kb_document_chunk`、`chat_record`、`student`、`course`、`score` 表。
- 使用 MyBatis-Plus 完成 Entity、Mapper、Service 分层。
- 实现 `QuestionRouter`、`RagService`、`PromptBuilder` 和 `LlmClient` 抽象。
- 支持 mock、DeepSeek、Spring AI ChatClient 三种调用路径。
- 使用 Redis 缓存知识库列表、高频问答和会话上下文。
- 使用 RabbitMQ 演示文档异步切分，RabbitMQ 不可用时降级同步切分。
- 使用 Vue 3 + Element Plus 做知识库、问答、成绩查询调试页面。

不要夸大：不要写精通、高并发、分布式、微服务、复杂 Agent 框架。
