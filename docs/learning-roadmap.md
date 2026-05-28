# 学习路线

## 第 1 步：跑通项目

导入 `scripts/init.sql` 和 `scripts/sample-data.sql`，启动项目，访问 `GET /api/health`。

## 第 2 步：看知识库接口

阅读 `KnowledgeBaseController` 和 `KnowledgeBaseServiceImpl`，理解知识库、文档、chunk 的写入流程。

## 第 3 步：看 RAG 工具类

阅读 `TextChunker`、`KeywordMatcher`、`PromptBuilder`，理解文档切分、检索和 Prompt 构造。

## 第 4 步：看聊天主流程

阅读 `ChatServiceImpl`，理解 QuestionRouter、RAG、普通聊天、聊天记录和 Redis 缓存。

## 第 5 步：看学业查询

阅读 `AcademicController` 和 `AcademicServiceImpl`，理解学生、课程、成绩表如何查询。

## 第 6 步：背面试题

按 `docs/interview-qa.md` 的 25 个问题准备，每个问题都要能指出代码位置。

## 第 7 步：整理简历话术

参考 `docs/resume-description.md`，只写当前真实实现，不写未实现功能。
