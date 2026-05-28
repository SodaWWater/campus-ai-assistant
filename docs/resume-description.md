# 简历描述

## 简历项目标题

校园资料智能问答与学业助手系统

## 技术栈

Java 21、Spring Boot 3、MyBatis-Plus、MySQL、Redis、springdoc-openapi、Prompt、简化 RAG、DeepSeek API 兼容调用

## 项目描述

基于 Spring Boot 实现的校园资料智能问答项目，支持知识库创建、文档切分、关键词检索、Prompt 构建、Mock/DeepSeek 大模型调用和学业成绩查询。项目使用 MySQL 保存知识库、文档片段和聊天记录，使用 Redis 缓存会话上下文和高频问答结果。

## 主要工作

1. 设计知识库、文档、文档片段、聊天记录、学生、课程、成绩等数据表。
2. 基于 MyBatis-Plus 完成 Entity、Mapper、Service 和 Controller 分层开发。
3. 实现文本切分、关键词 TopK 检索和 Prompt 构建。
4. 设计 QuestionRouter，将问题路由到 RAG、学业查询或普通问答。
5. 抽象 LlmClient，提供 mock 模式和 DeepSeek 真实调用预留。
6. 使用 Redis 缓存最近会话上下文和高频问答结果。

## 项目亮点

1. 用简化 RAG 实现了可运行、可讲清楚的 AI 问答闭环。
2. mock 模式默认可用，没有 API Key 也能演示主要流程。
3. 成绩类问题走数据库查询，避免模型编造确定性业务数据。

## 1 分钟介绍话术

这是一个校园资料智能问答项目。我用 Spring Boot 和 MyBatis-Plus 搭建后端，MySQL 保存知识库、文档片段和聊天记录，Redis 缓存会话上下文和高频问答。项目实现了简化 RAG：文档录入后切分成 chunk，用户提问时用关键词检索 TopK 片段，再拼接 Prompt 调用 LLM Client。默认提供 MockLlmClient，也预留 DeepSeek 调用。成绩类问题走数据库查询，不让模型编造。

## 不建议在简历中夸大的内容

- 不写向量数据库。
- 不写复杂 Agent 框架。
- 不写生产级知识库系统。
- 不写精通大模型底层原理。
