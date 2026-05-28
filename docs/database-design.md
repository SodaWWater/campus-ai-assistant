# 数据库设计

数据库名：`campus_ai`

字段命名约定：

- SQL 字段使用下划线命名。
- Java 实体字段使用驼峰命名。
- MyBatis-Plus 使用 `map-underscore-to-camel-case: true` 映射。

## 表结构

- `kb_knowledge_base`：知识库
- `kb_document`：知识库文档
- `kb_document_chunk`：文档切分片段
- `chat_record`：问答记录
- `student`：学生
- `course`：课程
- `score`：成绩

完整建表语句见 `scripts/init.sql`，演示数据见 `scripts/sample-data.sql`。
