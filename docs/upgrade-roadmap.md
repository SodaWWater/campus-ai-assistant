# Campus Knowledge Hub 升级路线图

## 边界原则

本路线图将升级拆成 4 个阶段。每个阶段都必须先完成计划文档，再按文档落地，避免临时扩展范围。

本轮执行范围：

- 编写完整阶段计划文档。
- 落地阶段一中风险可控的页面与产品闭环升级。
- 不在本轮实现向量数据库、微服务拆分、复杂模型评测和生产级部署。

## 阶段概览

| 阶段 | 名称 | 目标 | 本轮是否执行 |
| --- | --- | --- | --- |
| Phase 1 | 产品闭环与页面真实感 | 学生、教师、管理员三端形成更完整的业务闭环 | 执行 |
| Phase 2 | RAG 能力升级 | 从关键词检索升级到混合检索、引用编号和拒答策略 | 只写计划 |
| Phase 3 | 工程加固 | 权限、测试、可观测性、部署、安全 | 只写计划 |
| Phase 4 | 面试与交付包装 | 文档、截图、演示脚本、简历话术 | 只写计划 |

## 当前项目事实

- 后端：Spring Boot、JWT、MyBatis-Plus、Redis、RabbitMQ、MySQL。
- 前端：Vue 3、Element Plus、Vite。
- 当前 RAG：基于 `KeywordMatcher` 的关键词/字符匹配，不是向量检索。
- 当前样例数据：已引入 MIT OCW、OpenDSA、OpenStax、智慧高教参考和校内演示资料。
- 当前自动初始化：`DataInitializer` 会执行 `scripts/init.sql` 和 `scripts/sample-data.sql`。

## 不做幻觉式承诺

以下能力未实现前，不应在 README 或面试话术中说成已经完成：

- 向量数据库。
- embedding 生成。
- rerank 模型。
- OCR。
- 生产级限流。
- OpenTelemetry 链路追踪。
- Kubernetes 部署。

这些能力可以作为后续阶段计划，但不能写成当前功能。

