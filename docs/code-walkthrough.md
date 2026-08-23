# 项目流程与关键代码讲解

本文档按核心流程组织关键代码和设计问答，便于从入口定位到具体实现。
说明：以下代码块是对应文件中的关键逻辑摘录，为了便于讲解省略了部分导入、变量声明和异常处理，不应视为完整源码。

RAG 文档处理、chunk 切片、关键词提取和 pgvector 建索引的细节见 `docs/rag-deep-dive.md`；本文档只保留流程概览。

## 1. 登录与权限认证流程

### 怎么讲

用户登录时，前端提交用户名和密码。后端通过 `AuthServiceImpl` 加载用户，使用 BCrypt 校验密码，通过后用 `JwtUtil` 生成 Token。后续请求携带 `Authorization: Bearer <token>`，`JwtAuthenticationFilter` 解析 Token，把用户名和角色放进 Spring Security 上下文。接口层通过 `SecurityConfig` 控制不同角色可访问的路径。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/service/impl/AuthServiceImpl.java`

```java
CampusUserDetails userDetails = (CampusUserDetails) userDetailsService.loadUserByUsername(username);
String storedHash = userDetails.getPassword();

if (!passwordEncoder.matches(password, storedHash)) {
    throw new BadCredentialsException("密码错误");
}

String token = jwtUtil.generateToken(
        userDetails.getId(),
        userDetails.getUsername(),
        userDetails.getRole()
);
```

文件：`src/main/java/com/liminghan/campusai/security/JwtAuthenticationFilter.java`

```java
Claims claims = jwtUtil.parseClaims(token);
String username = claims.getSubject();
String role = claims.get("role", String.class);

UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
authentication.setDetails(claims);
SecurityContextHolder.getContext().setAuthentication(authentication);
```

文件：`src/main/java/com/liminghan/campusai/config/SecurityConfig.java`

```java
.requestMatchers("/api/admin/**").hasAuthority("ADMIN")
.requestMatchers("/api/teacher/**").hasAnyAuthority("TEACHER", "ADMIN")
.requestMatchers(HttpMethod.POST, "/api/kb/**").hasAnyAuthority("TEACHER", "ADMIN")
.anyRequest().authenticated()
```

### 设计问答

**为什么用 JWT？**
因为前后端分离项目不依赖服务端 Session，JWT 可以在无状态接口里携带用户身份和角色，方便网关或服务端鉴权。

**密码怎么存？**
使用 BCrypt，不存明文密码。演示账号启动时由 `DataInitializer` 使用 `BCryptPasswordEncoder` 刷新为 `123456` 对应 hash。

---

## 2. 学生 RAG 问答流程

### 怎么讲

学生在 AI 工作台提问，前端调用 `/api/chat/ask`。后端 `ChatController` 从 JWT 填充 userId，再调用 `ChatServiceImpl`。服务层先通过 `QuestionRouter` 判断问题类型。如果是学业查询，直接查数据库；如果是课程问题，则走 RAG：检索知识片段，构建 Prompt，调用 LLM，保存问答记录并返回引用来源。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/controller/ChatController.java`

```java
@PostMapping("/ask")
public Result<ChatResponseVO> ask(@Valid @RequestBody ChatAskRequest request) {
    if (request.getUserId() == null) {
        populateUserFromJwt(request);
    }
    return Result.success(chatService.ask(request));
}
```

文件：`src/main/java/com/liminghan/campusai/service/impl/ChatServiceImpl.java`

```java
QuestionType questionType = questionRouter.route(request.getQuestion());

if (cachedAnswer != null) {
    answer = cachedAnswer;
} else if (questionType == QuestionType.ACADEMIC_QUERY) {
    answer = academicService.answerAcademicQuestion(request.getQuestion());
} else {
    rawChunks = ragService.retrieveTopK(request.getKnowledgeBaseId(), request.getQuestion(), 5);
    matchedChunks = ragService.retrieveTopKWithScore(request.getKnowledgeBaseId(), request.getQuestion(), 5);
    ...
}
```

资料不足拒答：

```java
if (hasSelectedKnowledgeBase && rawChunks.isEmpty()) {
    prompt = "知识库命中不足，未调用 LLM。问题：" + request.getQuestion();
    answer = buildInsufficientKnowledgeAnswer(request.getQuestion());
}
```

保存问答记录：

```java
record.setQuestion(request.getQuestion());
record.setAnswer(answer);
record.setMatchedChunkIds(chunks.stream()
        .map(c -> String.valueOf(c.getId()))
        .collect(Collectors.joining(",")));
chatRecordService.save(record);
```

### 设计问答

**为什么资料不足时不让模型自由回答？**
因为这是课程知识库问答，用户期望的是可验证资料。无命中时拒答可以降低幻觉风险，也能提醒教师补充资料。

---

## 3. RAG 检索与 pgvector 回退流程

### 怎么讲

当前项目的 RAG 检索分两层。第一层是可选 pgvector 向量索引，第二层是关键词检索回退。系统主业务数据仍在 MySQL，pgvector 只保存 chunk 的向量索引副本。如果 pgvector 没启动或查询失败，系统不会崩，而是回退到 `KeywordMatcher`。

需要注意：当前 embedding 已抽象成可配置接口，默认本地 hashing；配置 `EMBEDDING_MODE=openai-compatible` 后可调用真实 embedding 服务。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/service/impl/RagServiceImpl.java`

```java
Map<Long, String> titleMap = loadTitleMap(chunks);
if (knowledgeBaseId != null) {
    vectorSearchService.indexChunks(chunks, titleMap);
    List<MatchedChunkVO> vectorResults = vectorSearchService.search(knowledgeBaseId, question, topK);
    if (!vectorResults.isEmpty()) {
        return vectorResults;
    }
}

return keywordMatcher.topKWithScore(question, chunks, topK, titleMap);
```

文件：`src/main/java/com/liminghan/campusai/service/vector/PgVectorSearchService.java`

```java
stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
stmt.execute("""
        CREATE TABLE IF NOT EXISTS kb_chunk_vector (
            chunk_id BIGINT PRIMARY KEY,
            knowledge_base_id BIGINT NOT NULL,
            document_id BIGINT NOT NULL,
            chunk_index INT NOT NULL,
            document_title VARCHAR(255),
            content TEXT NOT NULL,
            keywords TEXT,
            embedding vector(%d) NOT NULL,
            updated_at TIMESTAMP NOT NULL DEFAULT now()
        )
        """.formatted(dimension));
```

向量查询：

```java
SELECT chunk_id, chunk_index, document_title, content,
       GREATEST(0, ROUND(((1 - (embedding <=> ?::vector)) * 100)::numeric, 0)::int) AS score
FROM kb_chunk_vector
WHERE knowledge_base_id = ?
ORDER BY embedding <=> ?::vector
LIMIT ?
```

失败回退：

```java
private void disableForCurrentRun(String action, Exception e) {
    disabledByFailure = true;
    log.warn("pgvector {}. Falling back to keyword retrieval. Cause: {}", action, e.getMessage());
}
```

文件：`src/main/java/com/liminghan/campusai/service/vector/HashingEmbeddingService.java`

```java
public double[] embed(String text) {
    double[] vector = new double[dimension];
    for (String token : tokenize(text)) {
        int index = Math.floorMod(hash(token), dimension);
        vector[index] += 1.0;
    }
    normalize(vector);
    return vector;
}
```

### 设计问答

**为什么 pgvector 只做增强层，不替换 MySQL？**
MySQL 负责稳定保存业务数据，pgvector 只负责向量相似度检索。这样职责清晰，pgvector 挂了也不会影响主业务。

**真实 embedding 怎么接？**
当前版本通过 `EmbeddingClient` 抽象支持真实 embedding。默认 hashing 保证本地可运行；配置 `OpenAiCompatibleEmbeddingClient` 后，`PgVectorSearchService` 会写入真实向量，并保存 provider、model、dimension。

---

## 4. 文档上传、解析、切片与异步处理流程

### 怎么讲

教师上传资料后，后端先抽取文本并保存文档记录，然后发送 RabbitMQ 消息。消费者拿到 documentId 后重新读取文档内容，使用 `TextChunker` 切片，保存到 `kb_document_chunk`，再同步 pgvector 索引。这样上传接口不会被文档解析和索引构建阻塞。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/controller/KnowledgeBaseController.java`

```java
content = fileTextExtractor.extract(file.getBytes(), fileType);

Long documentId = knowledgeBaseService.uploadDocumentAsync(
        knowledgeBaseId,
        originalFilename,
        fileType,
        file.getSize(),
        content
);
```

文件：`src/main/java/com/liminghan/campusai/service/impl/KnowledgeBaseServiceImpl.java`

```java
DocumentProcessMessage message = new DocumentProcessMessage();
message.setDocumentId(documentId);
message.setKnowledgeBaseId(knowledgeBaseId);
rabbitTemplate.convertAndSend(documentExchange, documentRoutingKey, message);
```

RabbitMQ 不可用时降级：

```java
catch (Exception e) {
    try {
        processDocumentChunks(documentId);
    } catch (Exception pe) {
        markDocumentFailed(documentId, pe.getMessage());
    }
}
```

文件：`src/main/java/com/liminghan/campusai/mq/DocumentProcessConsumer.java`

```java
@RabbitListener(queues = "${app.mq.document-queue}")
public void consume(DocumentProcessMessage message) {
    knowledgeBaseService.processDocumentChunks(message.getDocumentId());
}
```

切片和索引：

```java
List<String> chunks = textChunker.split(document.getContent());
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

### 设计问答

**为什么用 MQ？**
文档解析、切片和索引构建可能耗时，异步化可以让上传接口快速返回，提升用户体验，也方便失败重试和任务监控。

---

## 5. Redis 缓存与会话记忆流程

### 怎么讲

Redis 在项目里不是主数据库，而是优化层。它缓存知识库列表、FAQ 答案和最近多轮会话上下文。Redis 失败时，代码会回退 MySQL 或直接继续主流程。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/service/impl/ChatServiceImpl.java`

FAQ 缓存 key：

```java
String faqKey = "chat:faq:" + Integer.toHexString(
        Objects.hash(request.getKnowledgeBaseId(), request.getQuestion()));
String cachedAnswer = readStringCache(faqKey);
```

会话上下文写入 Redis List：

```java
redisTemplate.opsForList().rightPush(key, "用户: " + question);
redisTemplate.opsForList().rightPush(key, "助手: " + answer);
redisTemplate.opsForList().trim(key, -20, -1);
redisTemplate.expire(key, Duration.ofHours(24));
```

文件：`src/main/java/com/liminghan/campusai/service/impl/KnowledgeBaseServiceImpl.java`

```java
String cacheKey = "kb:list";
Object cached = redisTemplate.opsForValue().get(cacheKey);
...
redisTemplate.opsForValue().set(cacheKey, list, Duration.ofMinutes(knowledgeBaseListTtlMinutes));
```

### 设计问答

**为什么不把聊天记录只放 Redis？**
Redis 是热数据缓存，可能过期或丢失。正式问答记录需要落 MySQL，便于历史查询、审计和教师分析。

---

## 6. 学业查询流程

### 怎么讲

成绩、学分这类结构化数据不应该交给大模型生成。系统用 `QuestionRouter` 识别学业查询后，直接调用 `AcademicService` 查 MySQL，避免模型编造分数。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/service/QuestionRouter.java`

```java
if (containsAny(text, List.of("成绩", "绩点", "学分", "挂科", "平均分"))) {
    return QuestionType.ACADEMIC_QUERY;
}
```

文件：`src/main/java/com/liminghan/campusai/service/impl/ChatServiceImpl.java`

```java
} else if (questionType == QuestionType.ACADEMIC_QUERY) {
    answer = academicService.answerAcademicQuestion(request.getQuestion());
}
```

文件：`src/main/java/com/liminghan/campusai/service/impl/AcademicServiceImpl.java`

这里通过学生、课程、成绩表查询结构化数据，不让大模型编造学业结果。

### 设计问答

**为什么不让 LLM 查成绩？**
因为成绩是确定性业务数据，必须以数据库为准。大模型适合解释和总结，不适合生成事实性分数。

---

## 7. 教师问题分析流程

### 怎么讲

教师端不只是管理资料，还能看到学生问了什么、高频问题是什么、哪些问题没有引用来源。这些信息可以反向指导教师补充讲义或复习资料。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/controller/TeacherController.java`

```java
@GetMapping("/question-analytics")
public Result<Map<String, Object>> questionAnalytics() {
    List<ChatRecord> records = chatRecordService.lambdaQuery()
            .orderByDesc(ChatRecord::getCreatedAt)
            .last("limit 100")
            .list();
```

统计无引用问题：

```java
long noCitationCount = records.stream()
        .filter(r -> r.getMatchedChunkIds() == null || r.getMatchedChunkIds().isBlank())
        .count();
```

统计高频问题：

```java
List<Map<String, Object>> topQuestions = records.stream()
        .collect(Collectors.groupingBy(ChatRecord::getQuestion, Collectors.counting()))
        .entrySet()
        .stream()
        .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
        .limit(8)
        .toList();
```

文件：`frontend/src/views/teacher/QuestionAnalytics.vue`

```js
const data = await http.get('/teacher/question-analytics')
records.value = data.records || []
topQuestions.value = data.topQuestions || []
summary.value = data
```

### 设计问答

**这个分析有什么业务价值？**
可以发现学生集中卡住的知识点，也可以把无引用问题转成“待补充资料”，形成知识库运营闭环。

---

## 8. 管理员文档任务监控流程

### 怎么讲

管理员端关注全局文档处理状态。页面会展示全部任务、已完成、处理中、失败数量，并支持按状态和关键词筛选。失败文档可以重试，文档也可以删除。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/controller/AdminController.java`

```java
@GetMapping("/documents")
public Result<List<KbDocument>> documents() {
    return Result.success(documentService.lambdaQuery()
            .orderByDesc(KbDocument::getCreatedAt).list());
}
```

重试：

```java
@PostMapping("/document/{documentId}/reprocess")
public Result<Map<String, String>> reprocessDocument(@PathVariable Long documentId) {
    knowledgeBaseService.reprocessDocument(documentId);
    return Result.success(Map.of("status", "PROCESSING"));
}
```

文件：`frontend/src/views/admin/DocumentTasks.vue`

```js
const statusCards = computed(() => [
  { label: '全部任务', value: documents.value.length },
  { label: '已完成', value: documents.value.filter(d => d.status === 'DONE').length },
  { label: '处理中', value: documents.value.filter(d => d.status === 'PROCESSING').length },
  { label: '失败', value: documents.value.filter(d => d.status === 'FAILED').length }
])
```

### 设计问答

**失败任务怎么处理？**
RabbitMQ 重试耗尽后进入失败状态，管理员可以在任务监控页查看失败原因并触发重新解析。

---

## 9. 自动初始化与演示数据流程

### 怎么讲

为了保证本地环境可以快速启动，项目启动后会自动执行 SQL 脚本，初始化表结构、演示账号、知识库、文档、chunk、对话和成绩数据。演示账号密码由 Java 端 BCrypt 重新生成，避免 SQL hash 与明文密码不一致。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/config/DataInitializer.java`

```java
runScript("db/init.sql", "schema");
migrateLegacyColumns();
runScript("db/sample-data.sql", "sample data");
fixPasswords();
```

修复演示账号密码：

```java
var encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("123456");
jdbcTemplate.update(
        "UPDATE sys_user SET password = ? WHERE username IN ('student', 'teacher', 'admin', 'student2')",
        hash);
```

### 设计问答

**为什么不用手动 SQL？**
自动初始化降低演示成本，也避免不同机器环境数据不一致。脚本幂等，重复启动不会重复插入主键数据。

---

## 10. 前端三角色页面流程

### 怎么讲

前端不是营销页，而是实际工作台。学生端突出 AI 问答和引用来源；教师端突出资料维护和问题分析；管理员端突出平台运营和任务治理。

### 关键代码

学生工作台：`frontend/src/views/student/ChatWorkspace.vue`

```js
const data = await askQuestion({
  conversationId: currentConversationId.value,
  knowledgeBaseId: selectedKb.value,
  question: q
})

last.answer = data.answer
last.matchedChunks = data.matchedChunks || []
```

教师文档中心：`frontend/src/views/teacher/DocumentCenter.vue`

```js
const formData = new FormData()
formData.append('file', file)
await uploadDocument(selectedKbId.value, formData)
```

管理员任务页：`frontend/src/views/admin/DocumentTasks.vue`

```js
documents.value = await http.get('/admin/documents') || []
```

### 设计问答

**为什么学生端三栏布局？**
左侧管理会话，中间问答，右侧显示引用来源和推荐追问。这样更符合知识库问答产品，而不是普通聊天框。
