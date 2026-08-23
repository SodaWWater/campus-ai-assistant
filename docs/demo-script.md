# 5 分钟演示脚本

本文档给出一条可复现的项目演示路径。目标不是遍历所有功能，而是集中展示“资料可维护、答案可溯源、角色有边界、后台可治理”的校园课程知识库流程。

## 演示准备

- 后端：`http://localhost:8081`
- 前端：`http://localhost:5173`
- 演示账号：`student / 123456`、`teacher / 123456`、`admin / 123456`
- 资料边界：样例资料位于 `docs/sample-materials/`，包含 Java、数据结构、数据库、校园学习事务指南等主题。

## 0. 开场 20 秒

话术：

> 这个项目不是简单把问题转发给大模型，而是围绕校园课程资料做了一个完整的 RAG 闭环。教师维护知识库，学生基于资料提问并看到引用来源，管理员监控用户、资料和任务状态。后端用 Spring Boot、MyBatis-Plus、Redis、RabbitMQ、Spring Security/JWT 和可选 pgvector 检索层实现。

对应代码：

- `src/main/java/com/liminghan/campusai/controller/ChatController.java`
- `src/main/java/com/liminghan/campusai/controller/KnowledgeBaseController.java`
- `src/main/java/com/liminghan/campusai/controller/TeacherController.java`
- `src/main/java/com/liminghan/campusai/controller/AdminController.java`

## 1. 学生端：可溯源问答 90 秒

操作：

1. 使用 `student / 123456` 登录。
2. 进入智能答疑工作台。
3. 选择 Java 或数据库知识库。
4. 提问：`ArrayList 和 LinkedList 怎么选？`
5. 展示回答正文、引用来源、命中的知识片段、检索/生成耗时。

话术：

> 学生提问后，后端不会直接让模型自由发挥。`ChatServiceImpl` 会先做问题路由，课程资料类问题进入 RAG；`RagServiceImpl` 检索知识片段；`PromptBuilder` 把问题和片段组织成 Prompt；最后把答案、引用片段和耗时一起返回前端。

对应代码：

```java
// ChatController.java
@PostMapping("/ask")
public Result<ChatResponse> ask(@RequestBody ChatRequest request) {
    return Result.success(chatService.ask(request));
}
```

```java
// ChatServiceImpl.java
QuestionType questionType = questionRouter.route(request.getQuestion());
List<KbDocumentChunk> matchedChunks = ragService.retrieveTopK(knowledgeBaseId, request.getQuestion(), 5);
String prompt = promptBuilder.buildRagPrompt(request.getQuestion(), matchedChunks);
String answer = llmClient.chat(prompt);
```

可追问回答：

> 引用不是前端写死的，而是后端返回的 `matchedChunks`。前端只负责把 chunk 的文档标题、来源类型、匹配分数和片段内容展示出来。

## 2. 教师端：资料维护与异步处理 90 秒

操作：

1. 使用 `teacher / 123456` 登录。
2. 进入知识库管理。
3. 打开某个知识库的文档中心。
4. 展示文档状态、片段数、解析耗时、来源说明。

话术：

> 教师端不是只上传一个文件，而是维护课程资料资产。上传后先保存文档记录，再通过 RabbitMQ 发送处理消息，由消费者异步抽取文本、切片、入库、建立可选向量索引，并更新状态。这样上传接口不会被长时间阻塞，失败任务也能在后台被看到。

对应代码：

```java
// KnowledgeBaseServiceImpl.java
document.setStatus("PROCESSING");
documentService.save(document);
DocumentProcessMessage message = new DocumentProcessMessage();
message.setDocumentId(documentId);
message.setKnowledgeBaseId(knowledgeBaseId);
rabbitTemplate.convertAndSend(documentExchange, documentRoutingKey, message);
```

```java
// DocumentProcessConsumer.java
@RabbitListener(queues = "${app.mq.document-queue}")
public void consume(DocumentProcessMessage message) {
    knowledgeBaseService.processDocumentChunks(message.getDocumentId());
}
```

```java
// KnowledgeBaseServiceImpl.java
List<String> chunks = textChunker.split(document.getContent());
List<KbDocumentChunk> savedChunks = new ArrayList<>();
for (int i = 0; i < chunks.size(); i++) {
    KbDocumentChunk chunk = new KbDocumentChunk();
    chunk.setDocumentId(document.getId());
    chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
    chunk.setChunkIndex(i);
    chunk.setContent(chunks.get(i));
    chunk.setKeywords(keywordMatcher.extractKeywords(chunks.get(i)));
    chunkService.save(chunk);
    savedChunks.add(chunk);
}
vectorSearchService.indexChunks(savedChunks, Map.of(document.getId(), document.getTitle()));
```

可追问回答：

> 当前资料来自项目内置样例和公开课程资料链接的整理说明，是可复现的样例资料库，不是线上真实校内生产数据。

## 3. RAG 检索：pgvector 可选增强 70 秒

操作：

1. 在学生端切换不同知识库提问。
2. 展示不同资料命中的来源不同。
3. 说明无命中时会拒答或提示资料不足。

话术：

> 检索层做了两级设计：优先尝试 pgvector 相似度检索，如果数据库扩展或向量数据不可用，就回退到关键词匹配。这个设计让项目可以在普通本地环境跑起来，也可以在有 PostgreSQL + pgvector 的环境里增强检索效果。

对应代码：

```java
// RagServiceImpl.java
vectorSearchService.indexChunks(chunks, titleMap);
List<MatchedChunkVO> vectorResults = vectorSearchService.search(knowledgeBaseId, question, topK);
if (!vectorResults.isEmpty()) {
    return vectorResults;
}
return keywordMatcher.topKWithScore(question, chunks, topK, titleMap);
```

```java
// HashingEmbeddingService.java
double[] vector = new double[dimension];
for (String token : tokenize(text)) {
    int index = Math.floorMod(hash(token), dimension);
    vector[index] += 1.0;
}
normalize(vector);
```

边界说明：

> 当前项目使用本地 Hashing Embedding 作为可运行的轻量方案，不包装成 OpenAI 或 DeepSeek embedding。真实生产可以替换为模型 embedding 服务，`PgVectorSearchService` 的调用边界已经预留出来。

## 4. 管理员端：平台治理 70 秒

操作：

1. 使用 `admin / 123456` 登录。
2. 进入平台运营看板。
3. 展示用户数、知识库数、文档数、问答数、文档任务状态、服务健康信息。

话术：

> 管理员端体现的是工程化闭环。除了能管理用户和知识库，还能看到文档处理任务状态、服务健康状态和问答统计，这样系统不是“能问答就结束”，而是可运维、可治理。

对应代码：

```java
// AdminController.java
data.put("userCount", userService.count());
data.put("knowledgeBaseCount", knowledgeBaseService.count());
data.put("documentCount", documentService.count());
data.put("chatCount", chatRecordService.count());
```

```vue
<!-- DocumentTasks.vue -->
<el-table :data="tasks">
  <el-table-column prop="documentName" label="文档" />
  <el-table-column prop="status" label="状态" />
  <el-table-column prop="chunkCount" label="片段数" />
</el-table>
```

## 5. 收尾 30 秒

话术：

> 这个项目我最想强调三点：第一，RAG 不是只调模型，而是资料治理、检索、Prompt、引用和拒答策略的组合；第二，权限和角色是真实接入 Spring Security/JWT 的；第三，文档处理、缓存、任务监控和测试让它从演示功能变成一个可以讲工程边界的项目。

收尾时不要夸大：

- 不说“已经生产可用”，说“具备生产化改造的骨架”。
- 不说“真实 embedding 模型”，说“当前是本地 hashing embedding，可替换真实 embedding 服务”。
- 不说“真实学校数据”，说“可复现样例资料库 + 公开资料来源说明”。
