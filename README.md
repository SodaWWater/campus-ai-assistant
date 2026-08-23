# Campus Knowledge Hub

校园课程知识库与智能答疑平台。项目面向高校课程资料管理和学生智能答疑场景，提供教师资料维护、学生 RAG 溯源问答、学业查询、管理员运营治理和文档异步解析能力。

界面按角色场景区分：学生端采用 AI 工作台，教师和管理员端采用教务与运营后台；资料库引用开放教育资源，并使用中文整理稿作为演示知识片段。

## 演示账号

| 角色 | 账号 | 密码 | 入口说明 |
| --- | --- | --- | --- |
| 学生 | `student` | `123456` | 课程知识库问答、学业查询 |
| 教师 | `teacher` | `123456` | 知识库管理、文档中心、问题分析 |
| 管理员 | `admin` | `123456` | 运营看板、用户管理、任务监控 |

## 核心能力

- 多角色平台：学生、教师、管理员独立工作台和权限边界。
- RAG 问答：基于知识库片段检索，回答返回引用来源和命中片段。
- 真实资料源：引用 MIT OCW、Open Data Structures、OpenStax、国家高等教育智慧教育平台课程链接。
- 文档处理：支持 TXT、Markdown、PDF、Word 文档抽取、切片和异步处理。
- 对话记忆：Redis 热缓存 + MySQL 持久化保存多轮问答。
- 学业查询：成绩、课程、学分等结构化问题直接查询数据库。
- 运营治理：管理员查看用户、知识库、文档任务、服务健康和资料来源。

## 真实资料来源

| 知识库 | 主要来源 | 链接 |
| --- | --- | --- |
| Java 程序设计与软件构造 | MIT OCW 6.005 Software Construction | https://ocw.mit.edu/courses/6-005-software-construction-spring-2016/ |
| 数据结构与算法 Java 版 | Open Data Structures | https://opendatastructures.org/ |
| 开放教材参考 | OpenStax | https://openstax.org/license/ |
| 中文课程参考 | 国家高等教育智慧教育平台 | https://higher.smartedu.cn/courses |

项目内置正文以中文二次整理资料为主，不大段复制开放课程正文；页面和文档中展示来源平台、授权标签和参考链接。

## 技术栈

- 后端：Java 21、Spring Boot 3.3、Spring Security 6、JWT、MyBatis-Plus
- 数据：MySQL 8、Redis 7
- 向量检索：PostgreSQL + pgvector（可选增强层，主业务库仍为 MySQL）
- 消息队列：RabbitMQ，支持文档异步处理、重试和死信队列
- 文档解析：PDFBox、Apache POI
- AI：`LlmClient` 抽象，支持 Mock、DeepSeek、Spring AI OpenAI-compatible client
- 前端：Vue 3、Vite、Vue Router、Element Plus、Axios
- API 文档：SpringDoc OpenAPI

## 快速启动

```powershell
# 1. 启动 Redis 与 RabbitMQ
.\start-infra.bat

# 2. 启动后端
mvn spring-boot:run

# 3. 启动前端
cd frontend
npm install
npm run dev
```

访问地址：

- 前端：http://localhost:5173
- Swagger：http://localhost:8081/swagger-ui.html
- RabbitMQ 管理台：http://localhost:15672
- pgvector：localhost:5433 / `campus_ai_vector`

## 初始化样例资料

启动后端时，`DataInitializer` 会自动执行 `scripts/init.sql` 和 `scripts/sample-data.sql`。脚本是幂等的，重复启动会刷新演示资料，不会重复插入主键数据。

数据库脚本包含：

- 四个知识库：Java 软件构造、数据结构 Java 版、数据库系统、校园学习事务指南。
- 真实来源资料说明：MIT OCW、OpenDSA、OpenStax、智慧高教参考。
- 学生、教师、管理员账号。
- 演示对话和学业成绩。

真实模型调用默认关闭，系统默认使用 `LLM_MODE=mock`。如需连接 DeepSeek，可在启动前设置：

```powershell
$env:LLM_MODE="real"
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn spring-boot:run
```

向量检索默认启用，但它是可选增强层。若 pgvector 未启动，系统会自动回退到关键词检索。关闭向量检索：

```powershell
$env:VECTOR_SEARCH_ENABLED="false"
mvn spring-boot:run
```

当前向量索引使用本地 hashing embedding 打通工程链路，不等同于真实模型 embedding。后续可替换为 OpenAI/DeepSeek embedding 或其他中文 embedding 模型。

资料说明见：

- `docs/product-design.md`
- `docs/sample-data.md`
- `docs/sample-materials/`

## 推荐演示流程

1. 使用 `teacher / 123456` 登录，进入课程知识库管理。
2. 查看《Java 程序设计与软件构造》知识库的资料来源和文档中心。
3. 使用 `student / 123456` 登录，选择 Java 知识库提问：“ArrayList 和 LinkedList 怎么选？”
4. 查看右侧引用来源，确认回答来自 OpenDSA / 校内 Java 复习资料。
5. 继续追问：“如果频繁在中间插入元素呢？”
6. 提问：“事务 ACID 分别是什么意思？”并切换到数据库知识库。
7. 提问：“我的数据库成绩怎么样？”展示结构化学业查询。
8. 使用 `admin / 123456` 登录，查看运营看板、文档处理状态和服务健康。

## 项目定位

面向高校课程资料管理与智能答疑场景的多角色 RAG 知识库平台，支持教师资料维护、学生溯源问答、管理员审核治理、文档异步解析和结构化学业查询。项目引入 MIT OCW、OpenDSA 等开放教育资源，并通过引用卡片呈现回答依据。

进一步了解项目：

- `docs/architecture.md`：总体架构、分层职责与关键代码入口
- `docs/demo-script.md`：完整演示流程与代码定位
- `docs/rag-deep-dive.md`：RAG 文档处理、chunk 规则、关键词提取、pgvector 索引深度讲解
- `docs/project-overview.md`：业务背景、系统边界与核心链路
- `docs/code-walkthrough.md`：核心流程与关键代码讲解
