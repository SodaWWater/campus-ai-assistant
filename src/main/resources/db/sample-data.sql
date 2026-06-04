SET NAMES utf8mb4;
USE campus_ai;

-- ============================================================
-- Demo accounts. DataInitializer refreshes these passwords to BCrypt("123456")
-- after loading this script, so the SQL hash is only an initialization seed.
-- ============================================================
INSERT INTO sys_user (id, username, password, nickname, role, status, created_at, updated_at)
VALUES
    (1, 'student',  '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQ4sLmzqJ3aYfFVL8PzLG', '张同学', 'STUDENT', 'ENABLED', NOW(), NOW()),
    (2, 'teacher',  '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQ4sLmzqJ3aYfFVL8PzLG', '李老师', 'TEACHER', 'ENABLED', NOW(), NOW()),
    (3, 'admin',    '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQ4sLmzqJ3aYfFVL8PzLG', '平台管理员', 'ADMIN', 'ENABLED', NOW(), NOW()),
    (4, 'student2', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQ4sLmzqJ3aYfFVL8PzLG', '王同学', 'STUDENT', 'ENABLED', NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), nickname = VALUES(nickname), role = VALUES(role), status = VALUES(status), updated_at = NOW();

-- ============================================================
-- Knowledge bases with real source references
-- ============================================================
INSERT INTO kb_knowledge_base (id, name, description, owner_id, owner_name, visibility, document_count, chunk_count, created_at, updated_at)
VALUES
    (1, 'Java 程序设计与软件构造', '参考 MIT OCW 6.005、OpenDSA 与中文 Java 课程页整理的课程知识库。', 2, '李老师', 'PUBLIC', 3, 5, NOW(), NOW()),
    (2, '数据结构与算法 Java 版', '基于 Open Data Structures 主题整理的 Java 数据结构复习知识库。', 2, '李老师', 'PUBLIC', 2, 4, NOW(), NOW()),
    (3, '数据库系统基础', '参考国家高等教育智慧教育平台课程主题和校内实验要求整理。', 2, '李老师', 'COURSE_ONLY', 2, 4, NOW(), NOW()),
    (4, '校园学习事务指南', '明华大学软件工程学院演示资料，覆盖实验、奖学金、论文格式等事务。', 2, '李老师', 'PUBLIC', 1, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    owner_id = VALUES(owner_id),
    owner_name = VALUES(owner_name),
    visibility = VALUES(visibility),
    document_count = VALUES(document_count),
    chunk_count = VALUES(chunk_count),
    updated_at = NOW();

-- ============================================================
-- Documents
-- ============================================================
INSERT INTO kb_document (id, knowledge_base_id, title, content, file_name, file_type, file_size, status, error_message, chunk_count, uploaded_by, processed_at, created_at, updated_at)
VALUES
    (1, 1, 'MIT OCW 6.005 软件构造主题整理',
     '来源：MIT OpenCourseWare 6.005 Software Construction。课程强调软件正确性、可理解性、可维护性和可变性控制。规格说明描述方法对调用者的承诺，包括输入要求、输出结果、异常情况和副作用。测试应覆盖正常情况、边界情况和异常情况。',
     'mit-ocw-6-005-summary.md', 'md', 620, 'DONE', NULL, 2, 2, NOW(), NOW(), NOW()),
    (2, 1, 'Java 集合与数据结构复习',
     '来源：Open Data Structures 与校内 Java 复习讲义。ArrayList 基于动态数组，随机访问效率高，适合按下标读取和尾部追加。LinkedList 基于链表，表达节点关系更直接，但普通业务场景通常优先考虑 ArrayList。HashMap 通过哈希函数定位桶位置，平均查询效率接近 O(1)。',
     'java-collections-review.md', 'md', 760, 'DONE', NULL, 2, 2, NOW(), NOW(), NOW()),
    (3, 1, 'Java 实验指导：单元测试与边界条件',
     '明华大学软件工程学院实验指导。实验要求学生为核心业务方法编写单元测试，覆盖正常输入、空值、边界值和异常分支。提交内容包括测试用例说明、运行截图和失败用例分析。',
     'java-test-lab-guide.md', 'md', 520, 'DONE', NULL, 1, 2, NOW(), NOW(), NOW()),
    (4, 2, 'Open Data Structures 主题整理',
     '来源：Open Data Structures。数组适合连续存储和快速随机访问；链式结构适合表达节点关系；栈遵循后进先出，常用于函数调用和撤销操作；队列遵循先进先出，常用于任务调度和广度优先搜索。',
     'open-data-structures-summary.md', 'md', 700, 'DONE', NULL, 2, 2, NOW(), NOW(), NOW()),
    (5, 2, '哈希表与树结构复习',
     '哈希表通过哈希函数将 key 映射到数组下标，负载因子和冲突处理会影响性能。树结构适合表达层级关系，二叉搜索树如果长期按有序数据插入可能退化，平衡树通过旋转等策略控制高度。',
     'hash-tree-review.md', 'md', 620, 'DONE', NULL, 2, 2, NOW(), NOW(), NOW()),
    (6, 3, '数据库系统课程主题整理',
     '来源：国家高等教育智慧教育平台课程主题与校内整理资料。关系模型使用表、行、列表达数据。SQL 用于数据定义、查询、更新和权限控制。索引可以减少扫描范围，但会增加写入维护成本。',
     'database-course-summary.md', 'md', 650, 'DONE', NULL, 2, 2, NOW(), NOW(), NOW()),
    (7, 3, '事务与范式复习说明',
     '事务 ACID 包括原子性、一致性、隔离性和持久性。范式用于减少数据冗余和更新异常。数据库实验报告需要包含需求说明、表结构设计、SQL 截图和结果分析。',
     'transaction-normal-form.md', 'md', 560, 'DONE', NULL, 2, 2, NOW(), NOW(), NOW()),
    (8, 4, '明华大学软件工程学院学习事务指南',
     '实验报告应在课程平台规定时间前提交，内容包括实验目标、核心代码、运行截图、问题分析和总结。综合奖学金评定参考课程成绩、综合素质、科研竞赛和违纪记录。毕业论文应包含题目、摘要、关键词、正文、参考文献和致谢。',
     'campus-learning-guide.md', 'md', 680, 'DONE', NULL, 3, 2, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    file_name = VALUES(file_name),
    file_type = VALUES(file_type),
    file_size = VALUES(file_size),
    status = VALUES(status),
    error_message = VALUES(error_message),
    chunk_count = VALUES(chunk_count),
    uploaded_by = VALUES(uploaded_by),
    processed_at = VALUES(processed_at),
    updated_at = NOW();

-- ============================================================
-- Chunks
-- ============================================================
INSERT INTO kb_document_chunk (id, document_id, knowledge_base_id, chunk_index, content, keywords, created_at)
VALUES
    (1, 1, 1, 0, '来源：MIT OpenCourseWare 6.005 Software Construction。课程强调软件正确性、可理解性、可维护性和可变性控制。', 'MIT,OCW,Software Construction,正确性,可维护性', NOW()),
    (2, 1, 1, 1, '规格说明描述方法对调用者的承诺，包括输入要求、输出结果、异常情况和副作用。测试应覆盖正常情况、边界情况和异常情况。', '规格说明,测试,边界条件,异常', NOW()),
    (3, 2, 1, 0, 'ArrayList 基于动态数组，随机访问效率高，适合按下标读取和尾部追加。LinkedList 基于链表，表达节点关系更直接。', 'ArrayList,LinkedList,动态数组,链表', NOW()),
    (4, 2, 1, 1, 'HashMap 通过哈希函数定位桶位置，平均查询效率接近 O(1)。实际使用时需要关注哈希分布、负载因子和冲突处理。', 'HashMap,哈希表,负载因子,O(1)', NOW()),
    (5, 3, 1, 0, 'Java 单元测试实验要求覆盖正常输入、空值、边界值和异常分支。提交内容包括测试用例说明、运行截图和失败用例分析。', '单元测试,边界值,异常分支,实验指导', NOW()),
    (6, 4, 2, 0, 'Open Data Structures 说明了数组、链表、栈、队列等结构。数组适合连续存储和快速随机访问；链式结构适合表达节点关系。', 'OpenDSA,数组,链表,随机访问', NOW()),
    (7, 4, 2, 1, '栈遵循后进先出，常用于函数调用、括号匹配和撤销操作。队列遵循先进先出，常用于任务调度和广度优先搜索。', '栈,队列,后进先出,先进先出,BFS', NOW()),
    (8, 5, 2, 0, '哈希表通过哈希函数将 key 映射到数组下标，负载因子和冲突处理会影响性能。', '哈希表,哈希函数,冲突处理,负载因子', NOW()),
    (9, 5, 2, 1, '树结构适合表达层级关系，二叉搜索树如果长期按有序数据插入可能退化，平衡树通过旋转等策略控制高度。', '树,二叉搜索树,平衡树,退化', NOW()),
    (10, 6, 3, 0, '关系模型使用表、行、列表达数据。SQL 用于数据定义、查询、更新和权限控制。', '关系模型,SQL,表,查询', NOW()),
    (11, 6, 3, 1, '索引可以减少扫描范围，提升查询速度，但会增加写入维护成本。需要结合查询条件和数据分布设计索引。', '索引,查询优化,写入成本', NOW()),
    (12, 7, 3, 0, '事务 ACID 包括原子性、一致性、隔离性和持久性。事务提交后结果应持久保存。', '事务,ACID,一致性,隔离性', NOW()),
    (13, 7, 3, 1, '范式用于减少数据冗余和更新异常。数据库实验报告需要包含需求说明、表结构设计、SQL 截图和结果分析。', '范式,冗余,实验报告,SQL截图', NOW()),
    (14, 8, 4, 0, '实验报告应在课程平台规定时间前提交，内容包括实验目标、核心代码、运行截图、问题分析和总结。迟交 24 小时内扣除该实验成绩 10%。', '实验报告,迟交,课程平台', NOW()),
    (15, 8, 4, 1, '综合奖学金评定参考课程成绩、综合素质、科研竞赛和违纪记录。申请学生原则上应无不及格课程。', '奖学金,成绩,综合素质,竞赛', NOW()),
    (16, 8, 4, 2, '毕业论文应包含题目、摘要、关键词、正文、参考文献和致谢。引用资料应标明来源，图表应有编号和标题。', '毕业论文,格式,参考文献,图表', NOW())
ON DUPLICATE KEY UPDATE
    document_id = VALUES(document_id),
    knowledge_base_id = VALUES(knowledge_base_id),
    chunk_index = VALUES(chunk_index),
    content = VALUES(content),
    keywords = VALUES(keywords);

-- ============================================================
-- Demo conversations
-- ============================================================
INSERT INTO chat_conversation (id, user_id, title, knowledge_base_id, created_at, updated_at)
VALUES
    (1, 1, 'ArrayList 和 LinkedList 怎么选？', 1, NOW(), NOW()),
    (2, 1, '数据库事务复习', 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), knowledge_base_id = VALUES(knowledge_base_id), updated_at = NOW();

INSERT INTO chat_record (id, user_id, username, conversation_id, knowledge_base_id, question, answer, source_type, matched_chunk_ids, prompt_preview, llm_mode, retrieval_time_ms, generation_time_ms, created_at)
VALUES
    (1, 1, 'student', 1, 1, 'ArrayList 和 LinkedList 怎么选？',
     '如果主要按下标读取、遍历和尾部追加，优先选择 ArrayList；如果已经定位到节点并频繁在附近插入删除，可以考虑 LinkedList。但在普通业务代码中，ArrayList 通常因为内存连续和缓存友好更常用。',
     'RAG', '3', '参考资料：OpenDSA 与 Java 集合复习讲义。', 'mock', 8, 20, NOW()),
    (2, 1, 'student', 2, 3, '事务 ACID 分别是什么意思？',
     'ACID 分别是原子性、一致性、隔离性和持久性。它们共同保证数据库事务在异常、并发和提交后的可靠行为。',
     'RAG', '12', '参考资料：数据库系统基础事务复习说明。', 'mock', 7, 18, NOW())
ON DUPLICATE KEY UPDATE
    question = VALUES(question),
    answer = VALUES(answer),
    matched_chunk_ids = VALUES(matched_chunk_ids),
    prompt_preview = VALUES(prompt_preview),
    created_at = VALUES(created_at);

-- ============================================================
-- Academic data
-- ============================================================
INSERT INTO student (id, user_id, student_no, name, major, grade)
VALUES
    (1, 1, '20230001', '张同学', '软件工程', '2023'),
    (2, 4, '20230002', '王同学', '软件工程', '2023')
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), name = VALUES(name), major = VALUES(major), grade = VALUES(grade);

INSERT INTO course (id, course_code, course_name, credit)
VALUES
    (1, 'JAVA101', 'Java 程序设计', 3.0),
    (2, 'DB101', '数据库系统', 3.0),
    (3, 'DS101', '数据结构', 4.0),
    (4, 'MATH101', '高等数学', 4.0),
    (5, 'ENG101', '大学英语', 2.0)
ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), credit = VALUES(credit);

INSERT INTO score (id, student_id, course_id, score, semester)
VALUES
    (1, 1, 1, 88.50, '2024-2025-1'),
    (2, 1, 2, 91.00, '2024-2025-1'),
    (3, 1, 3, 84.00, '2024-2025-1'),
    (4, 1, 4, 76.50, '2024-2025-1'),
    (5, 1, 5, 89.00, '2024-2025-1'),
    (6, 2, 1, 82.00, '2024-2025-1'),
    (7, 2, 2, 79.50, '2024-2025-1')
ON DUPLICATE KEY UPDATE score = VALUES(score), semester = VALUES(semester);
