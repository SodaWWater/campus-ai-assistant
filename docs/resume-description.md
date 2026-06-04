# 简历项目描述

## 项目名称

Campus Knowledge Hub：校园课程知识库与智能答疑平台

## 简历版描述

基于 Spring Boot + Vue 3 设计并实现面向高校课程资料管理与智能答疑场景的多角色 RAG 知识库平台，支持教师维护课程资料、学生可溯源问答、管理员文档任务监控与平台治理。系统集成 MySQL、Redis、RabbitMQ、PostgreSQL + pgvector，实现文档异步解析、知识片段检索、会话上下文缓存、资料不足拒答和多角色权限控制。

## 技术栈

Spring Boot 3、Java 21、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、RabbitMQ、PostgreSQL、pgvector、Spring AI、DeepSeek compatible API、Vue 3、Element Plus、Vite

## 主要职责

- 设计学生、教师、管理员三类角色的信息架构和权限边界。
- 设计知识库、文档、文档片段、会话记录、学生成绩等核心数据模型。
- 实现文档上传、文本抽取、切片、关键词提取、异步处理和失败重试。
- 实现 RAG 问答链路：问题路由、TopK 检索、Prompt 构建、LLM 抽象调用、引用来源返回。
- 引入 PostgreSQL + pgvector 作为可选向量索引增强层，并保留关键词检索回退。
- 使用 Redis 缓存会话上下文、FAQ 答案和知识库列表。
- 使用 RabbitMQ 解耦文档上传和切片处理，并设计重试与死信队列。
- 完成 Vue 3 前端工作台，包括学生 AI 问答、教师文档中心、教师问题分析、管理员任务监控。

## 技术亮点

### RAG 可溯源问答

学生提问后，系统先检索课程资料片段，再把片段和问题组装成 Prompt。回答返回 matchedChunks，前端展示资料来源、片段内容和匹配分数，降低模型幻觉风险。

### pgvector 可选增强层

主业务数据保存在 MySQL，PostgreSQL + pgvector 只保存 chunk 向量索引副本。向量库不可用时系统自动回退关键词检索，保证演示和本地启动稳定。

注意：当前版本使用本地 hashing embedding 打通工程链路，后续可替换真实 embedding 模型。

### RabbitMQ 异步文档处理

上传接口保存文档后投递消息，消费者异步执行切片、关键词提取和索引构建。消息队列不可用时降级同步处理，失败任务可在后台查看和重试。

### 结构化数据不走大模型

成绩、学分等确定性问题由 `QuestionRouter` 分流到 `AcademicService` 查询 MySQL，避免模型编造业务数据。

## 30 秒口述版

我做了一个校园课程知识库与智能答疑平台，教师可以上传课程资料，系统异步解析成知识片段；学生基于课程知识库提问，系统用 RAG 检索资料并返回可溯源回答；管理员可以查看文档任务和平台运行状态。技术上用了 Spring Boot、Vue、MySQL、Redis、RabbitMQ 和 pgvector，重点做了文档异步处理、检索回退、权限控制和资料不足拒答。

## 面试谨慎表述

不要写：

- 高并发生产系统。
- 已接入真实 embedding 大模型。
- 微服务架构。
- K8s 生产部署。

可以写：

- 可选 pgvector 向量索引增强层。
- 本地 hashing embedding 验证向量检索链路。
- 可替换真实 embedding 模型。
- 面向面试演示和学习复现的完整 RAG 应用。
