# Phase 5 可配置真实 Embedding 升级

本阶段目标：把原来的本地 hashing embedding 升级为可配置 embedding 接口。默认仍可本地运行；配置真实 embedding 服务后，pgvector 会写入真实模型向量。

## 设计结论

- `EmbeddingClient`：统一 embedding 抽象。
- `EmbeddingVector`：返回向量值，同时携带 `provider`、`model`、`dimension` 元数据。
- `HashingEmbeddingService`：本地 fallback，实现 `EmbeddingClient`。
- `OpenAiCompatibleEmbeddingClient`：调用 OpenAI-compatible `/embeddings` 接口。
- `EmbeddingClientRouter`：根据 `app.embedding.mode` 选择实现。
- `PgVectorSearchService`：不再依赖 hashing 服务，而是依赖 embedding 路由。

## 配置模式

默认本地模式不需要 API key：

```yaml
app:
  embedding:
    mode: hashing
    dimension: 128
```

真实 embedding 模式：

```powershell
$env:EMBEDDING_MODE="openai-compatible"
$env:EMBEDDING_BASE_URL="https://api.openai.com/v1"
$env:EMBEDDING_API_KEY="your_api_key"
$env:EMBEDDING_MODEL="text-embedding-3-small"
$env:EMBEDDING_DIMENSION="128"
$env:EMBEDDING_SEND_DIMENSIONS="true"
```

自动模式：

```powershell
$env:EMBEDDING_MODE="auto"
```

`auto` 会优先使用真实 embedding；未配置或调用失败时回退本地 hashing。

## pgvector 表升级

`kb_chunk_vector` 除了保存 `embedding vector(n)`，还保存：

- `embedding_provider`
- `embedding_model`
- `embedding_dimension`

检索时会按这三个字段过滤，避免切换 embedding 模型后混用旧索引。

## HNSW 索引

新增配置：

```yaml
app:
  vector:
    hnsw-enabled: true
```

启用后会尝试创建：

```sql
CREATE INDEX IF NOT EXISTS idx_kb_chunk_vector_embedding_hnsw
ON kb_chunk_vector
USING hnsw (embedding vector_cosine_ops);
```

如果 pgvector 版本不支持 HNSW，系统只记录 warn，不影响关键词回退。

## 面试讲法

> 项目最初用 hashing embedding 打通 pgvector 工程链路，后来我把 embedding 抽象成 `EmbeddingClient`，支持 OpenAI-compatible 真实 embedding 接口。写入 pgvector 时不只保存向量，还保存 provider、model、dimension，检索时按这些元数据过滤，避免模型切换后索引混用。默认模式仍然是 hashing，保证本地演示稳定；配置真实服务后就能写入真实 embedding。pgvector 还支持可选 HNSW 索引，用于后续数据量上来后的 ANN 检索优化。

## 任务边界

已完成：

- 可配置 embedding 接口。
- OpenAI-compatible embedding 调用。
- hashing fallback。
- pgvector embedding 元数据保存。
- 可选 HNSW 索引创建。

未完成：

- BM25 混合检索。
- 结构感知 chunk + overlap。
- reranker。
- 已有 pgvector 表从 128 维迁移到其他维度的自动迁移。
