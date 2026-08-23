# RAG 完整流程工程导读

> 读完这一篇，你能把整个 RAG 链路从头讲到尾。

---

## 先搞懂几个基本概念

### 什么是 RAG？

**RAG = 检索增强生成**。用人话说：

> 学生提问题 → 先去知识库里找相关资料 → 把资料和问题一起喂给 AI → AI 基于资料回答

和直接问 ChatGPT 的区别：ChatGPT 靠脑子里的"记忆"回答，可能瞎编。RAG 先查资料再回答，有据可查。

### 什么是向量（Vector）？

> 向量就是把一段文字变成一串数字。比如 "Java 集合框架" 可能变成 `[0.3, 0.1, 0.8, ...]`。

为什么要把文字变成数字？因为**计算机只会算数字**。两段文字意思越接近，它们的数字串就越相似。通过比较数字，就能找到最相关的资料。

### 什么是 Embedding？

> Embedding 就是"把文字转成向量"这个动作的名称。你可以理解成"文字 → 数字串的翻译过程"。

### 什么是 Chunk（切片）？

> 一篇长文档不能整篇塞给 AI（太长塞不下），需要切成小段。每一段就叫一个 chunk。

### 什么是 pgvector？

> pgvector 是 PostgreSQL 数据库的一个扩展插件。普通的数据库只能精确查询（比如"找出姓名=张三"），pgvector 能做模糊相似查询（"找出和这段文字最相似的 5 个片段"）。

把它理解成：**PostgreSQL 加了一个"相似度计算器"**。

---

## 整个流程，一步一步走

### 流程全景图

```
教师上传文档
    │
    ▼
① 文档入库（MySQL）  →  状态标记为"处理中"
    │
    ▼
② 投递消息（RabbitMQ）→  异步处理，不阻塞上传
    │
    ▼
③ 文档切片            →  长文档切成小段（chunk）
    │
    ▼
④ 提取关键词          →  每段挑出关键术语
    │
    ▼
⑤ 生成向量            →  每段变成一串数字（embedding）
    │
    ▼
⑥ 存入 pgvector       →  数字串写入向量数据库
    │
    ▼
⑦ 文档状态改为"完成"


学生提问
    │
    ▼
⑧ 问题转成向量        →  问题也变成一串数字
    │
    ▼
⑨ 向量相似度搜索      →  在 pgvector 里找最接近的片段
    │
    ▼
⑩ 向量失败？回退关键词  →  pgvector 挂了也不怕
    │
    ▼
⑪ 拼 Prompt            →  把资料 + 问题 + 历史拼成提示词
    │
    ▼
⑫ LLM 生成回答         →  AI 基于资料生成答案
    │
    ▼
⑬ 返回回答 + 引用来源  →  学生看到答案和来源卡片
```

---

## 每一步的详细解释

### 第①步：文档入库

教师上传一份 PDF 或 Markdown 文件。

系统做的事：
- 用 `FileTextExtractor` 把文件内容抽取成纯文本（PDF → Apache PDFBox 提取，Word → Apache POI 提取）
- 在 MySQL 的 `kb_document` 表里创建一条记录，状态标记为 `PROCESSING`（处理中）
- 教师端和管理员端可以看到这条记录的状态

**对应代码**：`KnowledgeBaseServiceImpl.createDocumentRecord()`

---

### 第②步：异步处理（RabbitMQ 消息队列）

**为什么不直接同步处理？**

因为 PDF 解析、切片、建索引可能需要几秒到几十秒。如果同步处理，上传接口要等很久才能返回，用户体验差。

**解决方案**：使用 RabbitMQ 消息队列。

```
上传接口 → 保存文档 → 往 RabbitMQ 丢一条消息 → 立即返回"上传成功"
                                              │
                                              ▼
                              后台消费者拿到消息 → 慢慢处理切片和索引
```

**什么是 RabbitMQ？**
你可以把它理解成**快递分拣中心**：
- 上传接口是"寄件人"（生产者），丢包裹就走
- RabbitMQ 是"快递中心"（队列），暂存包裹
- `DocumentProcessConsumer` 是"收件人"（消费者），逐个处理

**对应代码**：
- 生产者：`KnowledgeBaseServiceImpl.uploadDocumentAsync()`
- 消费者：`DocumentProcessConsumer.consume()`
- 如果 RabbitMQ 没启动，代码会自动降级为同步处理

---

### 第③步：文档切片（TextChunker）

**为什么切？** 一篇文档可能有几千字，直接塞给 AI 会超出限制。切成小块后每次只取最相关的小块。

**怎么切？** 用 `TextChunker.split()` 方法，规则很简单：

```
默认每段 400 个字符（差不多 200 个中文字）
        │
        ▼
找标点切割：从 400 字符位置往前找。！？等标点
        │
        ▼
  找到标点在 300 字符之后？→ 在标点处切，句子完整
  找不到合适的标点？      → 直接在 400 字符处硬切
        │
        ▼
继续下一段，直到全文切完
```

**为什么要找标点？** 避免把一句话从中间切断，保持语义完整。

**举个例子**：
```
原文（600字）：
"...ArrayList 基于动态数组，适合随机访问。LinkedList 基于链表，适合频繁插入删除。
在实际开发中，我们需要根据具体场景选择合适的数据结构..."
                      ↑ 这里有句号，正好在 400 字附近
                      → 在这里断开会比较自然
```

**对应代码**：`TextChunker.split()`

**当前切片 vs 专业升级方案**

现在是规则化方案。但架构已经预留了升级路径：

| | 当前方案 | 专业升级 |
|---|---|---|
| 切片方式 | 固定 400 字符 + 标点回退 | 按标题/段落层级切，每段 500 token |
| Overlap | 无 | 相邻 chunk 重叠 50~100 token，跨片段不丢信息 |
| 关键词 | 正则 + bigram | TF-IDF / BM25 自动加权 |
| 检索 | 向量 OR 关键词 | 混合检索：向量 × 权重 + BM25 × 权重 |
| Rerank | 无 | 粗召回 → 精排模型挑 TopK |

**实现说明**：

> 切片目前用规则化方案，简单可靠。但我设计时预留了升级路径——后面可以加 overlap 防止跨片段丢信息、用 BM25 做关键词加权、引入 rerank 提升精度。Embedding 也抽象成了接口，默认本地跑，配置后切真实模型。

---### 第④步：提取关键词（KeywordMatcher）

**为什么要提取关键词？** 两个用途：
1. 关键词检索时用于匹配打分
2. 生成向量时混入正文，提升检索效果

**怎么提取？** `KeywordMatcher.extractKeywords()` 的规则：

```
1. 用正则识别词元：
   - 中文：连续 2 个汉字以上 → "数据结构"、"链表"
   - 英文/数字/符号：连续 2 个以上 → "ArrayList", "O(1)"
   
2. 全部转小写

3. 过滤掉废话词（停用词）：
   - 中文："的"、"是"、"什么"、"这个"...
   - 英文："the"、"and"、"for"...

4. 中文词额外拆成二字词（bigram）：
   "数据结构" → ["数据", "据结", "结构"]
   这样即使学生问"数据结构"中的"结构"，也能匹配到

5. 取前 30 个，用逗号连接，存入数据库的 keywords 字段
```

**对应代码**：`KeywordMatcher.extractKeywords()`, `KeywordMatcher.tokenize()`

---

### 第⑤步：生成向量（Embedding）— 最核心的升级

**这是 RAG 最关键的一步**：把一段文字变成一串数字。

**架构设计：可配置 Embedding 接口**

我先设计了一个 `EmbeddingClient` 接口（相当于"向量生成器"的抽象标准），然后做了三种实现：

```
                   ┌─ EmbeddingClient 接口 ─┐
                   │   embed(文字) → 向量    │
                   └────────┬───────────────┘
                            │
          ┌─────────────────┼─────────────────┐
          ▼                 ▼                  ▼
   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
   │ Hashing模式   │  │ OpenAI模式    │  │ Auto模式      │
   │ 默认/本地     │  │ 真实API      │  │ 智能切换      │
   └──────────────┘  └──────────────┘  └──────────────┘
```

**为什么这样设计？** 一句话：**默认能跑，配置后能升级**。本地运行不依赖 API Key；配置相应环境变量后可以切换到真实模型。

#### 模式一：本地 Hashing（默认，零配置）

用 `HashingEmbeddingService`，原理：

```
1. 提取文本中的词元（和关键词类似的方法）
2. 对每个词元做 SHA-256 哈希 → 得到一个数字
3. 用这个数字对维度数取模 → 确定落在哪个位置
4. 在那个位置上 +1（词频累计）
5. 全部词元处理完后，做 L2 归一化
```

**举个极简例子（假设只有 5 维）**：
```
文本："ArrayList 动态数组 链表"
词汇：["arraylist", "动态数组", "链表"]

arraylist → hash → 模5 = 3 → 向量[3] += 1
动态数组 → hash → 模5 = 0 → 向量[0] += 1  
链表     → hash → 模5 = 1 → 向量[1] += 1

结果：[1, 1, 0, 1, 0]
```

**实现重点**：这不是语义模型，而是轻量级哈希映射。目的不是追求最强语义，而是**打通工程链路**——让 chunk → embedding → pgvector → 相似度检索这条线先跑通。后续替换真实模型只需实现一个 `EmbeddingClient`。

#### 模式二：真实 OpenAI-compatible Embedding（需要 API Key）

用 `OpenAiCompatibleEmbeddingClient`，调用 OpenAI 的 `/embeddings` 接口。

```powershell
# 配置环境变量后自动切换
$env:EMBEDDING_MODE="openai-compatible"
$env:EMBEDDING_BASE_URL="https://api.openai.com/v1"
$env:EMBEDDING_API_KEY="your_api_key"
$env:EMBEDDING_MODEL="text-embedding-3-small"
$env:EMBEDDING_DIMENSION="1536"   # text-embedding-3-small 默认 1536 维
```

**和 hashing 有什么区别？**

| | Hashing（本地） | OpenAI Embedding（真实） |
|---|---|---|
| 原理 | 哈希函数映射 | 大模型生成的语义向量 |
| 维度 | 128（可配） | 1536 / 3072（可配） |
| 语义理解 | 弱，只看词频率 | 强，理解上下文意思 |
| 需要 API Key？ | 不需要 | 需要 |
| "苹果"和"水果" | 两个不相关的词 | 能理解它们是相关的 |

说白了：**hashing 看的是"用了哪些词"，真实 embedding 看的是"说的是什么意思"**。

#### 模式三：Auto 自动模式

```powershell
$env:EMBEDDING_MODE="auto"
```

逻辑：
```
真实 API 配置了吗？→ 是 → 先试真实 API
                      │
                      ├─ 成功 → 返回真实向量
                      └─ 失败 → 自动回退 hashing
                      
                   → 否 → 直接用 hashing
```

**这个设计的意义**：开发环境不设 API Key 也能跑；生产环境设了就用更好的效果；API 临时挂了也不影响系统。

#### 路由决策：EmbeddingClientRouter

`EmbeddingClientRouter` 是总调度器，根据配置的 `app.embedding.mode` 决定走哪条路：

```java
// 三种模式一句话对应
mode = "hashing"              → 走本地哈希
mode = "openai-compatible"    → 走真实 API
mode = "auto"                 → 优先真实，失败回退哈希
```

#### pgvector 表里还保存了 Embedding 元数据

写入 pgvector 的不只是向量值，还有三个额外字段：

| 元数据字段 | 存什么 | 为什么存 |
|-----------|--------|---------|
| `embedding_provider` | "local" 或 "openai" | 知道是哪个服务生成的 |
| `embedding_model` | "hashing-embedding" 或 "text-embedding-3-small" | 知道用的哪个模型 |
| `embedding_dimension` | 128 或 1536 | 知道向量是几维的 |

**为什么存这些？** 当检索时，系统会加上过滤条件：

```sql
WHERE embedding_provider = 'openai'
  AND embedding_model = 'text-embedding-3-small'
  AND embedding_dimension = 1536
```

这样**不同模型生成的向量不会混在一起**。因为 OpenAI 1536 维向量和 hashing 128 维向量的"空间"完全不同，混在一起比较出来的相似度没有意义。就像不能用中国地图上的距离去比较月球地图上的距离。

**对应代码**：`EmbeddingClient.java`, `EmbeddingVector.java`, `EmbeddingClientRouter.java`, `HashingEmbeddingService.java`, `OpenAiCompatibleEmbeddingClient.java`, `PgVectorSearchService.indexChunks()`

---

### 第⑥步：存入 pgvector

每个 chunk 的向量写入 `kb_chunk_vector` 表：

| 字段 | 含义 |
|------|------|
| `chunk_id` | 对应 MySQL 中的 chunk ID |
| `knowledge_base_id` | 属于哪个知识库 |
| `content` | chunk 原文 |
| `embedding` | 128 维向量数组 |
| `embedding_provider` | 用的哪个 embedding 服务 |
| `embedding_model` | 用的哪个模型 |

写入时用 `ON CONFLICT DO UPDATE`：如果同一个 chunk 重新处理，直接更新旧的向量，不会重复。

**对应代码**：`PgVectorSearchService.indexChunks()`

---

### 第⑦步：文档状态更新

处理完成后，文档状态从 `PROCESSING` → `DONE`，教师端就能看到文档已就绪。

如果处理失败：状态变为 `FAILED`，管理员可以在后台重试。

---

### 第⑧步：学生提问，问题转向量

学生输入问题后，系统把问题用**同样的方法**转成向量。

**为什么要用同样的方法？** 因为只有用同样的"翻译规则"，数字才能比较。如果文档用 A 方法、问题用 B 方法，数字就对不上了。

**对应代码**：`RagServiceImpl.retrieveTopKWithScore()` → `vectorSearchService.search()`

---

### 第⑨步：向量相似度搜索

在 pgvector 里执行：

```sql
-- 找和问题向量最相似的 5 个 chunk
SELECT chunk_id, content, (1 - (embedding <=> 问题向量)) * 100 AS score
FROM kb_chunk_vector
WHERE knowledge_base_id = 知识库ID
  AND embedding_provider = 当前provider  -- 只匹配同一批 embedding
  AND embedding_model = 当前model        -- 避免混用不同模型的结果
ORDER BY embedding <=> 问题向量
LIMIT 5
```

**`<=>` 是什么？** 这是 pgvector 的"余弦距离"运算符。两个向量越接近，距离越小。然后用 `1 - 距离` 转成百分制分数（越接近 100 越相似）。

**为什么要按 provider/model/dimension 过滤？** 防止你用 OpenAI embedding 生成的数据，被 hashing embedding 的查询误匹配。不同模型生成的向量不在同一个"空间"里，混在一起结果没意义。

---

### 第⑩步：回退机制（关键词检索）

**如果 pgvector 不可用**（没启动、连不上、搜索出错），系统不会崩溃，而是自动回退到关键词检索：

```
pgvector 可用？→ 是 → 向量检索
              → 否 → KeywordMatcher 关键词评分 → 按分数排序取 TopK
```

**关键词打分规则**：

```
问题 token 在 chunk 中精确命中  →  加 2 分（长词加 4 分）
问题 token 在 chunk 中出现但不精确 →  加 1 分
分数为 0 的 chunk  →  过滤掉（不相关）
按分数从高到低 → 取前 5 个
```

**对应代码**：`RagServiceImpl.retrieveTopKWithScore()`, `KeywordMatcher.score()`

---

### 第⑪步：拼装 Prompt（提示词）

拿到最相关的 chunk 后，把资料、问题、历史对话拼成一段完整的提示词发给 AI。

```
你是校园课程知识库智能助教。
只能基于以下参考资料回答问题。

【参考资料】
[资料1] 来源：《Java 集合复习讲义》
内容：ArrayList 基于动态数组，随机访问效率高...

[资料2] 来源：《OpenDSA 数据结构》
内容：LinkedList 基于链表，表达节点关系更直接...

【对话历史】
用户：Java 中有什么常用集合？
助手：主要有 ArrayList、LinkedList、HashMap...

【当前问题】
ArrayList 和 LinkedList 怎么选？

要求：
1. 优先使用参考资料
2. 资料不足时明确说明，不要编造
3. 适合本科学生理解
```

**对应代码**：`PromptBuilder.buildRagPrompt()`

---

### 第⑫步：LLM 生成回答

把拼好的 Prompt 发给 LLM（大语言模型）。

**当前模式**：默认用 Mock 模式（不需要 API Key），返回模拟回答。配置 `LLM_MODE=real` 后通过 DeepSeek API 真正调用。

**三种模式**：
| 模式 | 怎么配置 | 干什么 |
|------|----------|--------|
| mock | 默认 | 返回预设的回答模板，用于本地演示 |
| real | `LLM_MODE=real` | 调用 DeepSeek API 真正生成 |
| spring-ai | `LLM_MODE=spring-ai` | 通过 Spring AI 框架调用 |

**对应代码**：`ChatServiceImpl.chooseClient()`

---

### 第⑬步：返回回答 + 引用来源

学生看到的不只是一个回答，还有：

```
┌─ 对话区 ───────────────────────────┐
│ Q: ArrayList 和 LinkedList 怎么选？  │
│ A: 如果主要按下标读取和遍历，优先     │
│    选 ArrayList；如果需要频繁在中间   │
│    插入删除，考虑 LinkedList...      │
└────────────────────────────────────┘

┌─ 引用来源 ─────────────────────────┐
│ [来源1] OpenDSA 数据结构资料        │
│ "...数组适合连续存储和快速随机访问.." │
│ 匹配度：92%                         │
│                                     │
│ [来源2] Java 集合复习讲义           │
│ "...LinkedList 基于链表..."         │
│ 匹配度：78%                         │
└────────────────────────────────────┘
```

**包含的信息**：资料标题、来源平台、匹配片段、匹配分数。

**对应代码**：`ChatResponseVO`, `MatchedChunkVO`

---

## 额外安全机制：问题分类路由

系统在检索之前，会先判断问题类型。

用 `QuestionRouter` 做关键词匹配：

```
问题包含 "成绩"、"分数"、"绩点" → 学业查询 → 直接查 MySQL，不走 AI
问题包含 "复习"、"解释"、"资料" → RAG 问答 → 走完整检索链路
其他                              → 一般对话 → 不走知识库，直接问 AI
```

**为什么成绩查询不走 AI？** 成绩是精确数据，必须查数据库，不能让 AI 编造。

**对应代码**：`QuestionRouter.route()`, `ChatServiceImpl.ask()`

---

## 实现速览

### RAG 链路概述

> 教师上传文档 → RabbitMQ 异步切片成 chunk → 先本地 hashing 生成 128 维向量存入 pgvector → 学生提问转同维度向量 → pgvector 余弦相似度找 Top5 最相关片段 → 拼 Prompt 发给 LLM → 返回答案 + 引用来源。Embedding 抽象成了接口，默认本地跑保证演示稳定，配置环境变量后切真实 OpenAI embedding。pgvector 不可用时自动回退关键词检索。

### 关键技术术语速查表

| 术语 | 一句话解释 |
|------|-----------|
| RAG | 先查资料再回答的 AI 模式，不靠记忆瞎编 |
| Chunk | 长文档切成的小段，每段约 400 字符 |
| Embedding | 把文字变成一串数字的过程 |
| 向量（Vector） | 文字对应的数字串，用来做相似度比较 |
| pgvector | PostgreSQL 的扩展，能做向量相似度搜索 |
| RabbitMQ | 消息队列，让耗时任务异步处理，不阻塞用户 |
| Hashing Embedding | 用哈希算法把文字映射成向量，不需要 API Key |
| Token | 文本切出来的最小单元，可以是词或字符 |
| Bigram | 把词拆成相邻两个字的组合 |
| Prompt | 发给 AI 的指令，包含资料 + 问题 + 规则 |
| Stop Word | 没用的常见词，如"的"、"是"、"the"，检索时过滤掉 |
| <=> | pgvector 的余弦距离运算符，越小越相似 |
| HNSW | 一种加速向量搜索的索引结构 |
| DLQ | 死信队列，消息处理失败后的归宿 |

### 六个实现要点

1. **架构**：MySQL 做主存储，pgvector 做向量索引副本，Redis 做缓存，RabbitMQ 做异步。

2. **检索**：向量优先（pgvector），关键词兜底（KeywordMatcher），保证高可用。

3. **Embedding 设计**：`EmbeddingClient` 抽象接口，支持 hashing/openai-compatible/auto 三种模式。写入 pgvector 时保存 provider/model/dimension 元数据，检索时按元数据过滤，避免不同模型混用。

4. **拒答**：知识库无命中时不调用 LLM，直接返回"当前知识库资料不足"，防止幻觉。

5. **权限**：Spring Security + JWT，三层控制——路径拦截 + SecurityUtils + visibility 过滤 + checkOwnership。

6. **可观测**：Actuator 健康检查 + Micrometer 指标（检索耗时/生成耗时/LLM 成功率）。

### 如果被追问"你是怎么实现的"

**问文档怎么切？** → 说的就是 TextChunker 的标点回退规则，400 字符窗口 + 300 最小阈值。

**问关键词怎么提取？** → 说的就是 KeywordMatcher 的正则词元 + 停用词过滤 + 中文 bigram。

**问向量怎么生成？** → 说的就是默认 Hashing Embedding（SHA-256 + 取模 + 归一化），已预留真实 Embedding 接口。

**问 pgvector 怎么查？** → 说的就是 `<=>` 余弦距离排序 + provider/model/dimension 过滤。

**问 pgvector 挂了怎么办？** → 说的就是 `disabledByFailure` 标记 + 自动回退关键词。

**问权限怎么控制？** → 说的就是 SecurityUtils 统一提取用户 + checkOwnership 检查 owner + visibility 过滤。

---

## 该说的和不该说的

### 可以说的（加分项）
- 异步处理用 RabbitMQ，失败有死信队列（DLQ）重试 3 次
- embedding 抽象成了接口，支持本地 hashing 和真实 API 双模式
- pgvector 只是可选增强层，挂了不影响主流程
- 拒答策略防止 LLM 幻觉

### 不要说的（避免扣分）
- 不要说"就是调个 API"——强调自己写了完整的检索链路
- 不要说"Hashing Embedding 是语义向量"——明确说这是本地轻量方案
- 不要主动提"还没做完"——说已经打通了链路，预留了升级空间
- 不要说"我也不太懂这个"——上面的速记卡片背熟就够了
