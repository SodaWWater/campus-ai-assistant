# RAG 文档处理与检索深度讲解

本文档说明 RAG 核心链路：文档处理、chunk 切分、关键词提取、pgvector 索引和检索回退。

说明：以下代码块是关键逻辑摘录，省略了部分导入、构造器和异常处理。讲解时以源码文件为准，不把摘录说成完整源码。

## 0. 当前 Embedding 版本

当前实现已经从“只能本地 hashing embedding”升级为“可配置 embedding 接口”：

- 默认 `EMBEDDING_MODE=hashing`，不需要 API key，便于本地演示。
- `EMBEDDING_MODE=openai-compatible` 时，调用真实 `/embeddings` 接口。
- `EMBEDDING_MODE=auto` 时，优先真实 embedding，未配置或失败时回退 hashing。
- pgvector 表保存 `embedding_provider`、`embedding_model`、`embedding_dimension`，检索时按这些字段过滤，避免不同模型索引混用。
- pgvector 可选创建 HNSW 索引，配置项为 `VECTOR_HNSW_ENABLED=true`。

对应代码：

- `EmbeddingClient`
- `EmbeddingVector`
- `EmbeddingClientRouter`
- `OpenAiCompatibleEmbeddingClient`
- `HashingEmbeddingService`
- `PgVectorSearchService`

## 1. 文档处理总流程

### 怎么讲

教师上传文档后，系统先在 MySQL 里创建一条 `kb_document` 记录，状态设为 `PROCESSING`。随后投递 RabbitMQ 消息，让 `DocumentProcessConsumer` 异步处理。消费者拿到 `documentId` 后，会删除旧 chunk 和旧向量索引，重新切片、提取关键词、保存 `kb_document_chunk`，最后同步写入 pgvector 索引并把文档状态改为 `DONE`。

如果 RabbitMQ 不可用，代码会降级为同步处理，保证本地演示时不因为 MQ 启动失败导致上传完全不可用。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/service/impl/KnowledgeBaseServiceImpl.java`

```java
public Long uploadDocumentAsync(Long knowledgeBaseId, String fileName, String fileType,
                                Long fileSize, String content) {
    Long documentId = createDocumentRecord(knowledgeBaseId, title, content, fileName, fileType, fileSize);

    DocumentProcessMessage message = new DocumentProcessMessage();
    message.setDocumentId(documentId);
    message.setKnowledgeBaseId(knowledgeBaseId);
    try {
        rabbitTemplate.convertAndSend(documentExchange, documentRoutingKey, message);
    } catch (Exception e) {
        processDocumentChunks(documentId);
    }
    return documentId;
}
```

文件：`src/main/java/com/liminghan/campusai/mq/DocumentProcessConsumer.java`

```java
@RabbitListener(queues = "${app.mq.document-queue}")
public void consume(DocumentProcessMessage message) {
    knowledgeBaseService.processDocumentChunks(message.getDocumentId());
}
```

文件：`src/main/java/com/liminghan/campusai/service/impl/KnowledgeBaseServiceImpl.java`

```java
public void processDocumentChunks(Long documentId) {
    KbDocument document = documentService.getById(documentId);

    vectorSearchService.deleteByDocumentId(documentId);
    chunkService.lambdaUpdate().eq(KbDocumentChunk::getDocumentId, documentId).remove();

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

    document.setStatus("DONE");
    document.setChunkCount(chunks.size());
    documentService.updateById(document);

    updateKbCounts(document.getKnowledgeBaseId());
    vectorSearchService.indexChunks(savedChunks, Map.of(document.getId(), document.getTitle()));
}
```

### 设计问答

**为什么上传后不直接同步处理？**

文档解析、切片、建索引都可能比较慢，同步处理会让上传接口长时间阻塞。异步处理可以让接口快速返回，后台任务状态通过教师端和管理员端展示。

**为什么处理前要先删旧 chunk 和旧向量？**

重试或重新处理文档时，旧索引如果不删除，会出现同一文档的新旧片段混在一起，导致检索结果污染。

## 2. chunk 切片规则

### 怎么讲

当前切片不是基于模型 token 的复杂切分，而是一个可解释、可本地稳定运行的字符级规则：

- 先对全文 `trim()`。
- 空文本返回空列表。
- 默认窗口大小 `CHUNK_SIZE = 400`。
- 最小切片阈值 `MIN_CHUNK_SIZE = 300`。
- 每次先尝试取 400 个字符。
- 如果这不是最后一段，并且当前窗口长度至少 300，就在窗口内从后往前找标点。
- 如果找到的标点位置在 `start + 300` 之后，就在标点后切断，避免把句子硬切开。
- 如果找不到合适标点，就按 400 字符硬切。
- 当前实现没有 overlap，`chunkIndex` 按切片顺序从 0 递增。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/util/TextChunker.java`

```java
private static final int CHUNK_SIZE = 400;
private static final int MIN_CHUNK_SIZE = 300;

public List<String> split(String text) {
    String normalized = text == null ? "" : text.trim();
    List<String> chunks = new ArrayList<>();
    if (normalized.isEmpty()) {
        return chunks;
    }

    int start = 0;
    while (start < normalized.length()) {
        int end = Math.min(start + CHUNK_SIZE, normalized.length());
        if (end < normalized.length() && end - start >= MIN_CHUNK_SIZE) {
            int punctuation = findLastPunctuation(normalized, start, end);
            if (punctuation > start + MIN_CHUNK_SIZE) {
                end = punctuation + 1;
            }
        }
        chunks.add(normalized.substring(start, end).trim());
        start = end;
    }
    return chunks;
}
```

```java
private int findLastPunctuation(String text, int start, int end) {
    for (int i = end - 1; i >= start; i--) {
        char c = text.charAt(i);
        if (c == '.' || c == '!' || c == '?' /* plus Chinese punctuation constants in source */) {
            return i;
        }
    }
    return -1;
}
```

注：源码中 `findLastPunctuation` 直接判断若干中英文标点字符，包括英文 `. ! ?`。准确边界是“优先按句末或分隔标点切分”，不是复杂 NLP 分句器。

### 为什么这样设计

当前实现优先保证规则简单、可解释、可测试和运行稳定。400 字符能让 chunk 足够短，便于 Prompt 引用；300 最小阈值避免为了一个很早的标点切出过短片段；标点回退让内容更接近完整语义单元。

### 局限与可升级点

- 当前没有 overlap，跨 chunk 的上下文可能丢失。
- 当前按 Java 字符长度切，不按模型 token 切。
- 后续可以升级为 `chunkSize + overlap`，例如 500 token + 80 token overlap。
- 也可以按 Markdown 标题、段落、表格结构做层级切分。

## 3. 关键词怎么提取

### 怎么讲

关键词提取在 `KeywordMatcher` 里完成。它不是 TF-IDF，也没有接入中文分词库，而是使用轻量规则：

- 用正则提取词元：
  - 连续 2 个以上中文字符：`[\p{IsHan}]{2,}`
  - 连续 2 个以上英文、数字或常见技术符号：`[a-zA-Z0-9_+#.-]{2,}`
- 全部转小写。
- 过滤停用词，比如英文 `the / and / for / with`，以及源码中维护的常见中文疑问词。
- 如果 token 包含中文，就额外拆成中文 bigram。
- `extractKeywords` 取前 30 个 token，用逗号拼接存入 `kb_document_chunk.keywords`。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/util/KeywordMatcher.java`

```java
private static final Pattern WORD_PATTERN =
        Pattern.compile("[\\p{IsHan}]{2,}|[a-zA-Z0-9_+#.-]{2,}");

public String extractKeywords(String text) {
    return tokenize(text).stream()
            .limit(30)
            .collect(Collectors.joining(","));
}
```

```java
private Set<String> tokenize(String text) {
    String safeText = nullToEmpty(text).toLowerCase(Locale.ROOT);
    LinkedHashSet<String> tokens = new LinkedHashSet<>();
    Matcher matcher = WORD_PATTERN.matcher(safeText);

    while (matcher.find()) {
        String token = matcher.group().trim();
        if (!STOP_WORDS.contains(token)) {
            tokens.add(token);
            if (containsChinese(token)) {
                tokens.addAll(chineseBigrams(token));
            }
        }
    }
    return tokens;
}
```

```java
private List<String> chineseBigrams(String token) {
    List<String> result = new ArrayList<>();
    for (int i = 0; i < token.length() - 1; i++) {
        result.add(token.substring(i, i + 2));
    }
    return result;
}
```

### 举例

假设 chunk 内容是：

```text
ArrayList 基于动态数组实现，适合随机访问；LinkedList 基于链表实现，适合频繁插入删除。
```

可能提取出的关键词包括：

```text
arraylist,动态数组,动态,态数,数组,linkedlist,链表,插入删除,插入,入删,删除
```

注意：具体顺序由 `LinkedHashSet` 保留正则扫描顺序决定。

## 4. 关键词检索怎么打分

### 怎么讲

用户提问时，系统会先把问题 tokenize，然后对每个 chunk 计算匹配分数。评分规则很直观：

- 如果问题 token 在 chunk token 集合中精确命中：
  - token 长度大于等于 4，加 4 分。
  - token 长度小于 4，加 2 分。
- 如果不是 token 精确命中，但 chunk 原文包含该 token，加 1 分。
- 分数为 0 的 chunk 被过滤。
- 按分数倒序取 TopK。

`topKWithScore` 会把 chunk 内容、文档标题、chunkIndex、score 封装成 `MatchedChunkVO` 返回给前端展示引用来源。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/util/KeywordMatcher.java`

```java
public int score(Set<String> questionTokens, String content) {
    String normalizedContent = content.toLowerCase(Locale.ROOT);
    Set<String> contentTokens = tokenize(content);
    int score = 0;

    for (String token : questionTokens) {
        if (contentTokens.contains(token)) {
            score += token.length() >= 4 ? 4 : 2;
        } else if (normalizedContent.contains(token)) {
            score += 1;
        }
    }

    return score;
}
```

```java
public List<MatchedChunkVO> topKWithScore(String question, List<KbDocumentChunk> chunks,
                                          int topK, Map<Long, String> titleMap) {
    Set<String> questionTokens = tokenize(question);
    return chunks.stream()
            .map(chunk -> {
                int s = score(questionTokens, chunk.getContent() + " " + nullToEmpty(chunk.getKeywords()));
                MatchedChunkVO vo = new MatchedChunkVO();
                vo.setId(chunk.getId());
                vo.setChunkIndex(chunk.getChunkIndex());
                vo.setContent(chunk.getContent());
                vo.setDocumentTitle(titleMap.getOrDefault(chunk.getDocumentId(), "未知文档"));
                vo.setScore(s);
                return vo;
            })
            .filter(vo -> vo.getScore() > 0)
            .sorted(Comparator.comparingInt(MatchedChunkVO::getScore).reversed())
            .limit(topK)
            .toList();
}
```

### 实现边界

这套关键词评分是轻量检索，不是 BM25，也不是 Elasticsearch。它的价值是本地可运行、逻辑可解释，并且作为 pgvector 不可用时的稳定回退。

## 5. hashing embedding 怎么生成

### 怎么讲

当前项目没有接入真实 embedding 模型，而是用 `HashingEmbeddingService` 把文本映射成固定维度向量，默认维度是 128。这个设计的目的不是追求最强语义效果，而是打通“chunk -> embedding -> pgvector -> 相似度检索”的工程链路。

生成过程：

- 用和关键词类似的正则提取中文、英文、数字、技术符号 token。
- 中文 token 额外拆 bigram。
- 对每个 token 做 SHA-256 hash。
- 用 hash 结果对向量维度取模，确定落在哪个维度。
- 对该维度加 1。
- 最后做 L2 normalize，让向量长度归一化。
- 写入 pgvector 前转成 `[0.000000,0.123456,...]` 字符串。

### 关键代码

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

```java
private int hash(String token) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
    return ((bytes[0] & 0xff) << 24)
            | ((bytes[1] & 0xff) << 16)
            | ((bytes[2] & 0xff) << 8)
            | (bytes[3] & 0xff);
}
```

```java
private void normalize(double[] vector) {
    double sum = 0;
    for (double value : vector) {
        sum += value * value;
    }
    if (sum == 0) {
        return;
    }
    double norm = Math.sqrt(sum);
    for (int i = 0; i < vector.length; i++) {
        vector[i] = vector[i] / norm;
    }
}
```

配置文件：`src/main/resources/application.yml`

```yaml
app:
  vector:
    enabled: ${VECTOR_SEARCH_ENABLED:true}
    dimension: ${VECTOR_DIMENSION:128}
    jdbc-url: ${VECTOR_DB_URL:jdbc:postgresql://localhost:5433/campus_ai_vector}
```

### 实现边界

一定要明确说：这不是大模型 embedding，不要包装成语义向量模型。它是一个轻量 hashing embedding，用于验证向量索引链路，后续可替换为真实中文 embedding。

## 6. pgvector 索引怎么建立

### 怎么讲

pgvector 是可选增强层，MySQL 仍然是主业务库。MySQL 保存文档和 chunk 原始数据，PostgreSQL + pgvector 保存 chunk 的向量索引副本。

索引建立发生在两个时机：

- 文档处理完成后：`KnowledgeBaseServiceImpl#processDocumentChunks` 调用 `vectorSearchService.indexChunks(savedChunks, titleMap)`。
- 检索前兜底补索引：`RagServiceImpl` 加载 MySQL chunk 后，也会调用一次 `indexChunks`，保证 pgvector 新启动或索引缺失时能补写。

`PgVectorSearchService` 第一次使用时会执行 `ensureSchema`：

- `CREATE EXTENSION IF NOT EXISTS vector`
- 创建 `kb_chunk_vector` 表
- `chunk_id` 作为主键
- `embedding vector(128)` 保存向量
- 创建 `knowledge_base_id` 普通索引，加速按知识库过滤

### 关键代码

文件：`src/main/java/com/liminghan/campusai/service/vector/PgVectorSearchService.java`

```java
private void ensureSchema(Connection conn) throws SQLException {
    if (schemaReady) {
        return;
    }
    synchronized (this) {
        if (schemaReady) {
            return;
        }
        try (Statement stmt = conn.createStatement()) {
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
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_kb_chunk_vector_kb ON kb_chunk_vector (knowledge_base_id)");
        }
        schemaReady = true;
    }
}
```

```java
public void indexChunks(List<KbDocumentChunk> chunks, Map<Long, String> titleMap) {
    try (Connection conn = openConnection()) {
        ensureSchema(conn);
        String sql = """
                INSERT INTO kb_chunk_vector
                (chunk_id, knowledge_base_id, document_id, chunk_index,
                 document_title, content, keywords, embedding, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, now())
                ON CONFLICT (chunk_id) DO UPDATE SET
                    content = EXCLUDED.content,
                    keywords = EXCLUDED.keywords,
                    embedding = EXCLUDED.embedding,
                    updated_at = now()
                """;

        for (KbDocumentChunk chunk : chunks) {
            ps.setLong(1, chunk.getId());
            ps.setLong(2, chunk.getKnowledgeBaseId());
            ps.setString(6, chunk.getContent());
            ps.setString(7, chunk.getKeywords());
            EmbeddingVector embedding = embeddingClient.embed(chunk.getContent() + " " + nullToEmpty(chunk.getKeywords()));
            ps.setString(8, toPgVectorLiteral(embedding.values()));
            ps.setString(9, embedding.provider());
            ps.setString(10, embedding.model());
            ps.setInt(11, embedding.dimension());
            ps.addBatch();
        }
        ps.executeBatch();
    }
}
```

### 为什么 embedding 输入拼了 content + keywords

```java
embeddingClient.embed(chunk.getContent() + " " + nullToEmpty(chunk.getKeywords()))
```

这样做是为了让向量索引既包含原始片段正文，也包含关键词字段中提炼出的词元。对于 hashing embedding 来说，这能提高技术词、中文 bigram 和片段主题词对检索的影响。

### 关于索引类型的边界

当前代码会创建 `knowledge_base_id` 普通索引，并在 `VECTOR_HNSW_ENABLED=true` 时尝试创建 HNSW 向量近似索引。检索排序使用的是 pgvector 的距离操作符 `<=>`。准确边界是：

> 项目已经打通 pgvector 向量表和相似度排序链路，并提供可选 HNSW 索引开关。如果本地 pgvector 版本不支持 HNSW，会记录 warn，不影响关键词回退。

## 7. 向量检索怎么查

### 怎么讲

用户提问时，系统把问题也转成同维度 hashing embedding，然后在 `kb_chunk_vector` 中按 `knowledge_base_id` 过滤，用 `<=>` 计算向量距离，距离越小越相似。代码用 `1 - distance` 转成百分制 score，并过滤掉 score 小于等于 0 的结果。

### 关键代码

文件：`src/main/java/com/liminghan/campusai/service/vector/PgVectorSearchService.java`

```java
public List<MatchedChunkVO> search(Long knowledgeBaseId, String question, int topK) {
    EmbeddingVector queryEmbedding = embeddingClient.embed(question);
    String vectorLiteral = toPgVectorLiteral(queryEmbedding.values());
    String sql = """
            SELECT chunk_id, chunk_index, document_title, content,
                   GREATEST(0, ROUND(((1 - (embedding <=> ?::vector)) * 100)::numeric, 0)::int) AS score
            FROM kb_chunk_vector
            WHERE knowledge_base_id = ?
              AND embedding_provider = ?
              AND embedding_model = ?
              AND embedding_dimension = ?
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """;
}
```

### 检索回退

文件：`src/main/java/com/liminghan/campusai/service/impl/RagServiceImpl.java`

```java
public List<MatchedChunkVO> retrieveTopKWithScore(Long knowledgeBaseId, String question, int topK) {
    List<KbDocumentChunk> chunks = loadChunks(knowledgeBaseId);
    Map<Long, String> titleMap = loadTitleMap(chunks);

    if (knowledgeBaseId != null) {
        vectorSearchService.indexChunks(chunks, titleMap);
        List<MatchedChunkVO> vectorResults = vectorSearchService.search(knowledgeBaseId, question, topK);
        if (!vectorResults.isEmpty()) {
            return vectorResults;
        }
    }

    return keywordMatcher.topKWithScore(question, chunks, topK, titleMap);
}
```

文件：`src/main/java/com/liminghan/campusai/service/vector/PgVectorSearchService.java`

```java
private void disableForCurrentRun(String action, Exception e) {
    disabledByFailure = true;
    log.warn("pgvector {}. Falling back to keyword retrieval. Cause: {}", action, e.getMessage());
}
```

### 实现说明

> 检索不是单点依赖 pgvector。pgvector 启用时优先走向量检索；如果未配置、连接失败、建表失败或搜索异常，`PgVectorSearchService` 会把当前运行周期标记为不可用，RAG 回退到关键词检索，保证系统仍能回答。

## 8. 这条链路怎么背

可以按这段 60 秒话术背：

> 文档上传后，系统先在 MySQL 保存文档记录并标记为 PROCESSING，然后投递 RabbitMQ。消费者根据 documentId 重新处理文档：先删除旧 chunk 和旧向量索引，再用 `TextChunker` 按 400 字符窗口切片，切片时会尽量在 300 字符之后的标点处断开，避免破坏句子。每个 chunk 保存到 `kb_document_chunk`，同时用 `KeywordMatcher` 提取前 30 个关键词，规则是正则词元、停用词过滤和中文 bigram。处理完成后把 chunk 写入 pgvector，embedding 使用本地 hashing embedding，默认 128 维，把正文和关键词一起映射成向量。查询时优先按 pgvector 的 `<=>` 距离排序取 TopK，如果 pgvector 不可用或没有结果，就回退关键词评分。这一实现保证本地链路可复现，并保留替换真实 embedding 和 ANN 索引的扩展位置。

## 9. 高频追问

**为什么 chunk 没有 overlap？**

当前为了规则简单、索引体积小、演示可解释，没有加 overlap。缺点是跨片段问题可能召回不足，后续可以增加 50 到 100 token overlap。

**为什么不用 TF-IDF 或 BM25？**

当前关键词检索是轻量回退方案，重点是可解释和少依赖。生产增强可以引入 BM25、Elasticsearch/OpenSearch 或 reranker。

**为什么不直接用 pgvector 做主库？**

业务数据仍然适合放 MySQL，例如用户、权限、文档状态、聊天记录、学业数据。pgvector 只做向量索引副本，职责更清楚，挂掉也不影响主业务数据。

**HNSW 索引现在怎么处理？**

当前实现会在 `VECTOR_HNSW_ENABLED=true` 时尝试创建：

```sql
CREATE INDEX idx_kb_chunk_vector_embedding
ON kb_chunk_vector
USING hnsw (embedding vector_cosine_ops);
```

如果 pgvector 版本不支持 HNSW，系统只记录 warn，仍然可以用 `<=>` 排序或回退关键词检索。

**如何接入真实 embedding？**

回答：

> 当前已经有 `EmbeddingClient` 抽象。默认是 hashing；配置 `EMBEDDING_MODE=openai-compatible` 后，会通过 `OpenAiCompatibleEmbeddingClient` 调用真实 embedding API，返回同维度向量后继续走 `PgVectorSearchService#indexChunks` 和 `search`。
