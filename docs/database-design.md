# 数据库设计

数据库：`campus_ai`。SQL 字段使用下划线，Java Entity 使用驼峰，MyBatis-Plus 开启 `map-underscore-to-camel-case`。

| 表名 | 说明 | 主要字段 |
| --- | --- | --- |
| `kb_knowledge_base` | 知识库 | `id`, `name`, `description`, `created_at`, `updated_at` |
| `kb_document` | 文档 | `id`, `knowledge_base_id`, `title`, `content`, `created_at`, `updated_at` |
| `kb_document_chunk` | 文档片段 | `id`, `document_id`, `knowledge_base_id`, `chunk_index`, `content`, `keywords`, `created_at` |
| `chat_record` | 聊天记录 | `id`, `user_id`, `question`, `answer`, `source_type`, `matched_chunk_ids`, `created_at` |
| `student` | 学生 | `id`, `student_no`, `name`, `major`, `grade` |
| `course` | 课程 | `id`, `course_code`, `course_name`, `credit` |
| `score` | 成绩 | `id`, `student_id`, `course_id`, `score`, `semester` |

初始化脚本：

- `scripts/init.sql`
- `scripts/sample-data.sql`

文档切分消息使用 RabbitMQ，不新增业务表；切分结果仍写入 `kb_document_chunk`。
