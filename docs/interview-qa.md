# 高频面试问答

## 1. 这个项目解决什么问题？

**推荐回答：**
它解决的是高校课程资料分散、学生问答缺少可靠来源、教师无法沉淀高频问题的问题。教师把课程资料维护成知识库，学生基于知识库提问，系统返回答案和引用来源，管理员负责用户、文档任务和平台状态治理。

**对应代码：**

- `KnowledgeBaseController`
- `ChatController`
- `TeacherController`
- `AdminController`

## 2. 你这个项目和普通 ChatGPT 套壳有什么区别？

**推荐回答：**
普通套壳只把用户问题转发给模型。这个项目有资料维护、文档解析、切片、检索、引用、拒答、权限、缓存、异步处理和后台治理。模型只是最后的生成环节，前面有完整业务链路。

**对应代码：**

- `KnowledgeBaseServiceImpl#processDocumentChunks`
- `RagServiceImpl#retrieveTopKWithScore`
- `PromptBuilder#buildRagPrompt`
- `ChatServiceImpl#ask`

## 3. RAG 怎么实现？

**推荐回答：**
教师上传文档后，系统抽取正文并切成 chunk。学生提问时，`RagServiceImpl` 先尝试 pgvector 向量索引，失败或无结果时回退关键词检索。检索到的 chunk 会进入 Prompt，模型基于资料回答，并把 matchedChunks 返回给前端展示来源。

**对应代码：**

- `TextChunker`
- `KeywordMatcher`
- `RagServiceImpl`
- `PgVectorSearchService`
- `PromptBuilder`

## 4. 为什么用 pgvector？

**推荐回答：**
pgvector 适合作为轻量向量检索层，部署比 Milvus 简单，又比纯关键词检索更接近真实 RAG 架构。项目中主业务库仍然是 MySQL，pgvector 只存 chunk 的向量索引副本，降低了迁移风险。

**注意边界：**
默认 embedding 是 hashing；配置 `EMBEDDING_MODE=openai-compatible` 后可调用真实 embedding。

**对应代码：**

- `PgVectorSearchService`
- `HashingEmbeddingService`
- `docker-compose.yml`

## 5. pgvector 挂了怎么办？

**推荐回答：**
系统会记录 warning，然后把向量检索关闭到当前运行周期，自动回退关键词检索，不影响主流程。

**对应代码：**

```java
private void disableForCurrentRun(String action, Exception e) {
    disabledByFailure = true;
    log.warn("pgvector {}. Falling back to keyword retrieval. Cause: {}", action, e.getMessage());
}
```

## 6. 为什么资料不足时拒答？

**推荐回答：**
课程知识库问答更看重可信来源。如果检索不到资料还让模型自由发挥，就容易产生幻觉。所以选定知识库但无命中时，系统直接提示“当前知识库资料不足”，并建议切换知识库或补充资料。

**对应代码：**

- `ChatServiceImpl#buildInsufficientKnowledgeAnswer`
- `ChatServiceImpl#ask`

## 7. RabbitMQ 用在哪里？

**推荐回答：**
用在文档异步处理。上传接口保存文档后投递消息，消费者异步切片和建索引。这样上传不会被耗时任务阻塞，也方便失败重试和死信队列处理。

**对应代码：**

- `RabbitMqConfig`
- `KnowledgeBaseServiceImpl#uploadDocumentAsync`
- `DocumentProcessConsumer`

## 8. Redis 缓存了什么？

**推荐回答：**
缓存三类内容：知识库列表、FAQ 答案、最近会话上下文。Redis 是优化层，不是主存储；失败时回退 MySQL 或继续主流程。

**对应代码：**

- `KnowledgeBaseServiceImpl#listKnowledgeBases`
- `ChatServiceImpl#readStringCache`
- `ChatServiceImpl#appendConversationHistory`

## 9. 成绩查询为什么不走大模型？

**推荐回答：**
成绩是确定性结构化数据，必须以数据库为准。如果交给大模型生成，可能编造分数。系统用 `QuestionRouter` 识别学业问题，然后由 `AcademicService` 查 MySQL。

**对应代码：**

- `QuestionRouter`
- `AcademicServiceImpl`
- `ChatServiceImpl#ask`

## 10. 权限怎么设计？

**推荐回答：**
认证层使用 Spring Security + JWT，Token 中有 userId 和 role。接口层控制学生、教师、管理员路径访问，业务层再检查知识库 owner 和 visibility，避免教师越权管理别人的知识库。

**对应代码：**

- `SecurityConfig`
- `JwtAuthenticationFilter`
- `SecurityUtils`
- `KnowledgeBaseServiceImpl`

## 11. 文档上传后如何变成可检索知识？

**推荐回答：**
上传后先用 `FileTextExtractor` 抽取正文，保存 `kb_document`。然后异步切片成 `kb_document_chunk`，提取关键词，并同步 pgvector 向量索引。学生提问时检索的就是这些 chunk。

**对应代码：**

- `FileTextExtractor`
- `TextChunker`
- `KnowledgeBaseServiceImpl#processDocumentChunks`
- `PgVectorSearchService#indexChunks`

## 12. 你项目最大的难点是什么？

**推荐回答：**
难点不是单个接口，而是把 AI 问答放进完整业务系统里：资料上传后要异步处理、检索要可解释、模型要可切换、无资料要拒答、缓存和向量检索都要可降级，同时还要保证学生、教师、管理员权限边界清楚。

## 13. 如果继续优化，你会怎么做？

**推荐回答：**
第一步把 hashing embedding 替换成真实中文 embedding 模型；第二步增加 rerank；第三步把引用编号写入回答正文；第四步补充 Markdown 渲染和反馈持久化。不会优先做微服务或 K8s，因为当前简历项目阶段投入产出比不高。

## 14. 这个项目有没有夸大的地方需要注意？

**推荐回答：**
有边界需要说清楚：pgvector 链路已经打通，但 embedding 目前是本地 hashing embedding，不是真实语义模型；系统是本地演示级平台，不是生产级大规模系统。

## 15. 面试官让你现场演示，怎么演示？

**推荐回答：**

1. 教师登录，查看课程知识库和文档中心。
2. 学生登录，选择 Java 知识库提问。
3. 展示回答、引用来源和资料不足拒答。
4. 教师查看学生问题分析。
5. 管理员查看文档任务监控和运营看板。

**对应文档：**

- `docs/demo-script.md`

## 16. chunk 的切片规则具体是什么？

**推荐回答：**
当前用的是 `TextChunker` 的字符级规则，不是 token splitter。先把全文 `trim()`，空文本返回空列表；默认 `CHUNK_SIZE = 400`，`MIN_CHUNK_SIZE = 300`。每一段先取最多 400 个字符，如果不是最后一段，就在这个窗口里从后往前找标点；如果标点位置在 300 字符之后，就在标点后切断，尽量保持句子完整。如果找不到合适标点，就按 400 字符硬切。当前没有 overlap，`chunkIndex` 从 0 递增。

**对应代码：**

- `TextChunker#split`
- `KnowledgeBaseServiceImpl#processDocumentChunks`
- `TextChunkerTest`

## 17. 关键词是怎么提取和打分的？

**推荐回答：**
关键词由 `KeywordMatcher` 提取。它用正则识别连续中文词元和英文/数字/技术符号词元，统一转小写，过滤停用词；中文 token 会额外拆成 bigram。每个 chunk 保存前 30 个关键词到 `kb_document_chunk.keywords`。检索时，问题也走同样的 tokenize；精确 token 命中加分更高，长 token 命中加 4 分，短 token 命中加 2 分，原文包含但不是 token 精确命中加 1 分，最后按分数倒序取 TopK。

**对应代码：**

- `KeywordMatcher#extractKeywords`
- `KeywordMatcher#tokenize`
- `KeywordMatcher#score`
- `KeywordMatcher#topKWithScore`

## 18. pgvector 索引是怎么建立的？

**推荐回答：**
MySQL 是主业务库，保存文档和 chunk；pgvector 是可选索引副本。文档处理完成后，`KnowledgeBaseServiceImpl#processDocumentChunks` 调用 `PgVectorSearchService#indexChunks`。第一次使用 pgvector 时会创建 `vector` 扩展、`kb_chunk_vector` 表和 `knowledge_base_id` 普通索引。每个 chunk 用 `chunk_id` 作为主键，保存知识库 ID、文档 ID、chunkIndex、标题、正文、关键词和 `embedding vector(128)`。写入时用 `ON CONFLICT (chunk_id) DO UPDATE`，所以重复处理会更新旧索引。

**对应代码：**

- `PgVectorSearchService#ensureSchema`
- `PgVectorSearchService#indexChunks`
- `HashingEmbeddingService#embed`

## 19. 当前向量检索是不是完整语义检索？

**推荐回答：**
默认不是，配置后可以是。当前项目默认使用本地 hashing embedding，保证没有 API key 时也能演示；如果设置 `EMBEDDING_MODE=openai-compatible`，就会通过 `OpenAiCompatibleEmbeddingClient` 调用真实 `/embeddings` 接口。写入 pgvector 时会保存 provider、model、dimension，检索时也按这些元数据过滤。

**对应代码：**

- `EmbeddingClient`
- `OpenAiCompatibleEmbeddingClient`
- `PgVectorSearchService#toPgVectorLiteral`
- `PgVectorSearchService#search`
- `docs/rag-deep-dive.md`

## 20. 现在 embedding 是否已经支持真实模型？

**推荐回答：**
支持可配置真实 embedding。项目新增了 `EmbeddingClient` 抽象，默认实现是本地 `HashingEmbeddingService`，保证没有 API key 时也能演示；如果配置 `EMBEDDING_MODE=openai-compatible`，系统会通过 `OpenAiCompatibleEmbeddingClient` 调用真实 `/embeddings` 接口；如果配置 `EMBEDDING_MODE=auto`，会优先真实 embedding，失败时回退 hashing。写入 pgvector 时还会保存 provider、model、dimension，检索时按这些字段过滤，避免模型切换后混用旧向量。

**对应代码：**

- `EmbeddingClient`
- `EmbeddingVector`
- `EmbeddingClientRouter`
- `OpenAiCompatibleEmbeddingClient`
- `PgVectorSearchService#indexChunks`
- `docs/phase-5-configurable-embedding.md`
