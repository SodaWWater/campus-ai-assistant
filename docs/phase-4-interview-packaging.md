# Phase 4 面试与交付包装

## 目标

把 Campus Knowledge Hub 整理成能在简历、面试和现场演示中讲清楚的完整项目。重点不是继续堆功能，而是让面试官快速理解：

- 项目解决什么业务问题。
- 系统架构如何分层。
- RAG、pgvector、RabbitMQ、Redis、权限这些技术分别解决了什么问题。
- 每个核心流程对应哪段关键代码。
- 哪些能力已经实现，哪些是后续可扩展方向。

## 本阶段交付物

| 文件 | 用途 |
| --- | --- |
| `docs/architecture.md` | 总体架构、分层职责与关键代码入口 |
| `docs/phase-5-configurable-embedding.md` | 可配置真实 embedding 接口升级说明 |
| `docs/rag-deep-dive.md` | RAG 文档处理、chunk 规则、关键词提取、pgvector 索引深度讲解 |
| `docs/interview-playbook.md` | 30 秒、2 分钟、5 分钟项目讲解模板 |
| `docs/interview-code-walkthrough.md` | 按流程讲项目，并带对应关键代码 |
| `docs/interview-qa.md` | 高频面试问答 |
| `docs/resume-description.md` | 简历项目描述和可选版本 |
| `docs/demo-script.md` | 现场演示脚本 |

## 讲解边界

可以说：

- 已实现多角色校园知识库平台。
- 已实现文档解析、切片、异步处理、RAG 问答、引用来源、资料不足拒答。
- 已实现 PostgreSQL + pgvector 可选向量索引增强层。
- 当前 embedding 已抽象为可配置接口，默认 hashing，配置后可走 OpenAI-compatible 真实 embedding。
- pgvector 不可用时会自动回退关键词检索。
- 已做 Redis 会话上下文缓存和 FAQ 缓存。
- 已做 RabbitMQ 异步文档处理、重试和死信队列设计。

不要说：

- 不要说项目默认会调用真实 embedding；默认仍是 hashing，真实服务需要配置 API key。
- 不要说已实现生产级微服务或 Kubernetes。
- 不要说这个系统已经线上大规模使用。
- 不要把 hashing embedding 说成大模型语义 embedding。

## 背诵顺序

1. 先背 `interview-playbook.md` 的 30 秒和 2 分钟版本。
2. 再背 `rag-deep-dive.md`，重点掌握 chunk、关键词、pgvector 三个深挖点。
3. 再背 `interview-code-walkthrough.md` 的 10 条核心流程。
4. 最后背 `interview-qa.md` 高频追问。
