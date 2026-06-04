# Phase 2 RAG 能力升级

## 目标

把当前关键词检索升级为更真实的 RAG 检索与回答链路。

## 计划范围

1. 检索升级
   - BM25 或增强关键词检索。
   - 向量检索候选方案：pgvector、Elasticsearch/OpenSearch、Milvus。
   - 混合检索：关键词召回 + 向量召回 + 分数融合。

2. 文档切片升级
   - 按标题、段落、页码切片。
   - chunk overlap。
   - 保存来源 URL、来源平台、授权信息、章节标题。

3. Prompt 与拒答策略
   - 有资料命中时进行溯源回答。
   - 无可靠资料时明确提示资料不足。
   - 区分课程解释、实验指导、考试复习、校园事务、学业查询。

4. 引用编号
   - 回答正文中使用 `[1] [2]`。
   - 右侧引用卡片按编号对应。

## 非范围

- 本阶段不做微服务拆分。
- 不引入商业闭源 rerank 服务作为默认依赖。

## 验收标准

- RAG 检索结果可解释。
- 回答能展示引用编号和来源片段。
- 无命中资料时不会硬编答案。
- 检索耗时和生成耗时被记录。

## 当前已完成子阶段

本阶段第一步已完成，范围限定为“不引入新基础设施”的 RAG 策略增强：

- `KeywordMatcher` 从字符级匹配升级为词元、中文 bigram 和关键词字段综合评分。
- `retrieveTopK` 不再返回零分片段，避免无关资料进入 Prompt。
- `PromptBuilder` 改为正常中文提示词，并要求资料不足时明确拒答。
- `ChatServiceImpl` 在用户选择知识库但无可靠命中时不调用 LLM，直接返回“当前知识库资料不足”。
- `MockLlmClient` 文案修复为正常中文，便于默认 mock 模式演示。

仍未实现：

- 真实模型 embedding 生成。
- rerank 模型。
- 引用编号写入回答正文。

## Phase 2-B 实施说明

PostgreSQL + pgvector 方案见 `docs/phase-2-pgvector-design.md`。当前实现目标是打通可选向量索引链路，embedding 采用本地 hashing embedding，后续可替换为真实 embedding 模型。
