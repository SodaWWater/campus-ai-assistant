# 面试问答

## 项目介绍类

1. 这个项目是做什么的？
   - 面试官想考什么：项目定位是否清楚。
   - 推荐回答：这是一个校园资料智能问答与学业助手项目，支持知识库、文档切分、简化 RAG、Mock/DeepSeek LLM 调用和学业成绩查询。
   - 不要怎么回答：不要说是完整智能体平台。
   - 对应代码位置：`ChatController`、`KnowledgeBaseController`、`AcademicController`

2. 项目为什么适合实习简历？
   - 面试官想考什么：项目是否真实可讲。
   - 推荐回答：它覆盖 Spring Boot 接口、MyBatis-Plus、MySQL、Redis、Prompt、RAG 和大模型 API 抽象，复杂度适中。
   - 不要怎么回答：不要说项目已经达到生产级。
   - 对应代码位置：`service`、`entity`、`util`

3. 项目核心链路是什么？
   - 面试官想考什么：是否理解整体流程。
   - 推荐回答：文档录入后切分 chunk，提问时路由问题，RAG 问题检索片段，构造 Prompt，调用 LLM，保存聊天记录。
   - 不要怎么回答：不要只背技术名词。
   - 对应代码位置：`KnowledgeBaseServiceImpl`、`ChatServiceImpl`

4. 这个项目没有做什么？
   - 面试官想考什么：边界意识。
   - 推荐回答：没有做复杂前端、向量数据库、复杂 Agent 框架，当前是最小可运行闭环。
   - 不要怎么回答：不要把未实现功能写成已实现。
   - 对应代码位置：`README.md`

5. 如何本地演示？
   - 面试官想考什么：是否真能运行。
   - 推荐回答：导入 `init.sql` 和 `sample-data.sql`，启动项目，按 `docs/api.md` 的顺序测试知识库、文档、问答和成绩查询。
   - 不要怎么回答：不要说只看文档无法运行。
   - 对应代码位置：`scripts`、`docs/api.md`

## RAG 类

6. 什么是本项目的 RAG？
   - 面试官想考什么：是否理解检索增强生成。
   - 推荐回答：先检索相关资料片段，再把片段放进 Prompt，让模型基于资料回答。
   - 不要怎么回答：不要说模型自己知道资料内容。
   - 对应代码位置：`RagServiceImpl`、`PromptBuilder`

7. 文档如何切分？
   - 面试官想考什么：chunk 设计。
   - 推荐回答：`TextChunker` 按约 300 到 500 字思路切分，并保留 `chunkIndex`。
   - 不要怎么回答：不要把整篇文档直接塞给模型。
   - 对应代码位置：`TextChunker`

8. 如何检索 TopK？
   - 面试官想考什么：检索实现。
   - 推荐回答：`KeywordMatcher` 对问题和 chunk 做简单字符级关键词匹配，根据重合度排序后取 TopK。
   - 不要怎么回答：不要说当前实现是向量检索。
   - 对应代码位置：`KeywordMatcher`

9. 为什么使用关键词检索？
   - 面试官想考什么：技术取舍。
   - 推荐回答：它实现简单、可运行、容易讲清楚，适合作为可替换向量检索的最小版本。
   - 不要怎么回答：不要说关键词检索一定比向量检索好。
   - 对应代码位置：`RagServiceImpl`

10. RAG 回答如何避免编造？
    - 面试官想考什么：Prompt 约束。
    - 推荐回答：Prompt 明确要求只基于资料回答，资料不足时回答“当前知识库未找到相关内容”。
    - 不要怎么回答：不要说能完全杜绝所有错误。
    - 对应代码位置：`PromptBuilder`

## Prompt / 大模型 API 类

11. PromptBuilder 做了什么？
    - 面试官想考什么：Prompt 工程基础。
    - 推荐回答：它组织角色设定、回答要求、资料片段和用户问题，让模型输入结构更清晰。
    - 不要怎么回答：不要说只是简单拼接。
    - 对应代码位置：`PromptBuilder`

12. LlmClient 为什么要抽象？
    - 面试官想考什么：接口设计。
    - 推荐回答：业务只依赖 `LlmClient`，可以在 mock 和真实 DeepSeek 调用之间切换。
    - 不要怎么回答：不要说是为了显得复杂。
    - 对应代码位置：`LlmClient`

13. MockLlmClient 的作用是什么？
    - 面试官想考什么：本地可运行性。
    - 推荐回答：没有 API Key 时仍能演示完整链路，返回包含“根据知识库片段生成的模拟回答”的结果。
    - 不要怎么回答：不要把 mock 回答当真实模型能力。
    - 对应代码位置：`MockLlmClient`

14. DeepSeekLlmClient 如何处理 API Key？
    - 面试官想考什么：配置健壮性。
    - 推荐回答：启动时不强制要求 Key，real 模式调用时才检查，缺失时返回明确错误。
    - 不要怎么回答：不要说没有 Key 项目就不能启动。
    - 对应代码位置：`DeepSeekLlmClient`

15. 普通聊天和 RAG 问答有什么区别？
    - 面试官想考什么：业务分支。
    - 推荐回答：普通聊天只构造通用 Prompt；RAG 会先检索知识库 chunk，再构造带资料的 Prompt。
    - 不要怎么回答：不要说所有问题都走 RAG。
    - 对应代码位置：`ChatServiceImpl`

## Spring Boot / MySQL 类

16. 项目分层是什么？
    - 面试官想考什么：工程结构。
    - 推荐回答：Controller 接收请求，Service 处理业务，Mapper 访问数据库，Entity 映射表，DTO/VO 区分请求和返回。
    - 不要怎么回答：不要把所有逻辑放 Controller。
    - 对应代码位置：`controller`、`service`、`mapper`

17. MyBatis-Plus 用在哪里？
    - 面试官想考什么：ORM 使用。
    - 推荐回答：每张表有 Entity、Mapper、Service，通过 BaseMapper 和 IService 完成增删查改。
    - 不要怎么回答：不要说完全手写 SQL。
    - 对应代码位置：`entity`、`mapper`、`service`

18. 数据库有哪些表？
    - 面试官想考什么：数据建模。
    - 推荐回答：知识库、文档、chunk、聊天记录、学生、课程、成绩七张表。
    - 不要怎么回答：不要只说有一个表。
    - 对应代码位置：`scripts/init.sql`

19. 学业查询如何实现？
    - 面试官想考什么：确定性数据处理。
    - 推荐回答：通过学生、课程、成绩表查询，不依赖模型生成。
    - 不要怎么回答：不要说让大模型猜成绩。
    - 对应代码位置：`AcademicServiceImpl`

20. 删除知识库时如何处理关联数据？
    - 面试官想考什么：数据一致性意识。
    - 推荐回答：删除知识库时删除对应文档和 chunk，再删除知识库。
    - 不要怎么回答：不要忽略关联数据。
    - 对应代码位置：`KnowledgeBaseServiceImpl`

## Redis / 异常处理 / 工程实践类

21. Redis 缓存了什么？
    - 面试官想考什么：缓存场景。
    - 推荐回答：缓存最近几轮会话上下文和 RAG 高频问答结果。
    - 不要怎么回答：不要说所有回答都永久缓存。
    - 对应代码位置：`ChatServiceImpl`

22. Redis 不可用怎么办？
    - 面试官想考什么：降级意识。
    - 推荐回答：缓存读写用 try-catch 包住，Redis 异常不会影响聊天主流程。
    - 不要怎么回答：不要让缓存失败导致核心接口失败。
    - 对应代码位置：`ChatServiceImpl`

23. 统一返回如何实现？
    - 面试官想考什么：接口规范。
    - 推荐回答：所有接口返回 `Result<T>`，包含 code、message、data。
    - 不要怎么回答：不要每个接口返回不同结构。
    - 对应代码位置：`Result`

24. 异常如何处理？
    - 面试官想考什么：工程实践。
    - 推荐回答：使用 `BusinessException` 和 `GlobalExceptionHandler` 统一处理业务异常和系统异常。
    - 不要怎么回答：不要到处 try-catch 返回字符串。
    - 对应代码位置：`GlobalExceptionHandler`

25. 如何保证文档和代码一致？
    - 面试官想考什么：交付意识。
    - 推荐回答：接口路径、请求参数、表名都同步写进 README 和 `docs/api.md`，后续改代码要同步改文档。
    - 不要怎么回答：不要说文档不重要。
    - 对应代码位置：`README.md`、`docs/api.md`
