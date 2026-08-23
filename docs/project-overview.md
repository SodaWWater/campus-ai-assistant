# Campus Knowledge Hub 项目概览

## 30 秒项目简介

Campus Knowledge Hub 是一个面向高校课程资料管理与智能答疑场景的多角色 RAG 知识库平台。系统支持教师维护课程资料，学生基于知识库进行可溯源 AI 问答，管理员查看文档任务、服务状态和平台运营数据。后端基于 Spring Boot、MyBatis-Plus、MySQL、Redis、RabbitMQ，前端基于 Vue 3 和 Element Plus。RAG 检索支持关键词回退和 PostgreSQL + pgvector 可选向量增强层，文档上传后通过 RabbitMQ 异步切片并建立索引。

## 2 分钟项目介绍

这个项目解决的是“校园资料分散、学生提问缺少可靠来源、教师无法沉淀高频问题”的问题。

系统分成三类角色：

- 学生端：课程知识库问答、引用来源展示、学业查询。
- 教师端：知识库管理、文档上传、文档解析状态、学生问题分析。
- 管理员端：用户管理、知识库审核、文档任务监控、运行状态看板。

核心链路是 RAG 问答。教师先上传课程资料，系统抽取文本并切成知识片段。学生提问时，后端先判断问题类型，如果是成绩这类结构化问题，就直接查 MySQL；如果是课程知识问题，就从知识库中检索 TopK 片段，构建 Prompt，再调用 LLM Client。回答会带上命中的片段和来源，方便学生知道答案依据。

工程上我做了几个设计：

- LLM 调用用 `LlmClient` 抽象，支持 mock、DeepSeek 和 Spring AI 模式。
- 文档处理用 RabbitMQ 异步化，并配置重试和死信队列，避免上传接口被解析任务阻塞。
- Redis 用来缓存会话上下文、FAQ 答案和知识库列表，失败时回退数据库。
- pgvector 作为可选向量索引层，不影响 MySQL 主库；不可用时自动回退关键词检索。
- 权限用 Spring Security + JWT，按学生、教师、管理员区分访问范围。

这个项目的亮点不是单纯接大模型，而是把 AI 问答放进一个真实业务系统里，补齐了资料维护、异步处理、权限、缓存、溯源和运营治理。

## 5 分钟技术讲解结构

### 1. 业务背景

高校课程资料通常分布在课件、实验文档、复习资料和通知里。学生直接问大模型容易出现幻觉，教师也很难知道学生集中卡在哪些知识点。这个系统把课程资料沉淀为知识库，让学生基于可信资料提问。

### 2. 总体架构

前端是 Vue 3 单页应用，后端是 Spring Boot 单体服务。MySQL 存主业务数据，Redis 做缓存，RabbitMQ 做文档异步处理，PostgreSQL + pgvector 做可选向量索引。

### 3. 核心 RAG 流程

学生提问进入 `ChatController`，再到 `ChatServiceImpl`。服务层会先用 `QuestionRouter` 区分学业查询和课程问答。课程问答进入 `RagServiceImpl` 检索知识片段，优先 pgvector，失败回退关键词。检索到的片段由 `PromptBuilder` 拼成 Prompt，再交给 `LlmClient`。

### 4. 文档处理流程

教师上传文件后，`KnowledgeBaseController` 调用 `FileTextExtractor` 抽取文本，`KnowledgeBaseServiceImpl` 保存文档记录并投递 RabbitMQ。消费者 `DocumentProcessConsumer` 拿到 documentId 后切片，保存到 `kb_document_chunk`，并同步 pgvector 索引。

### 5. 工程兜底

如果 RabbitMQ 不可用，上传流程会降级同步处理；如果 pgvector 不可用，RAG 会回退关键词检索；如果 Redis 不可用，会回退 MySQL 查询。这样可以保持本地运行和演示流程稳定。

### 6. 权限与治理

JWT 中保存 userId 和 role。Spring Security 控制接口权限，业务层再校验知识库 owner 和 visibility。管理员可以看全局用户、文档任务和服务状态。

## 一句话亮点

这个项目不是“调用大模型回答问题”，而是围绕校园课程资料构建了一个可维护、可溯源、可降级、可治理的 RAG 应用闭环。
