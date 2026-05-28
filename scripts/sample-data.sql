USE campus_ai;

INSERT INTO kb_knowledge_base (id, name, description, created_at, updated_at)
VALUES (1, 'Java 复习资料库', '用于演示 RAG 问答的 Java 基础资料', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), updated_at = NOW();

INSERT INTO kb_document (id, knowledge_base_id, title, content, created_at, updated_at)
VALUES (
    1,
    1,
    'Java 集合基础',
    'ArrayList 底层基于动态数组，适合按照下标快速访问元素。LinkedList 底层基于链表，适合频繁在头尾插入和删除。HashMap 使用哈希表保存键值对，常用于根据 key 快速查找 value。复习集合时要关注数据结构、扩容机制、时间复杂度和常见使用场景。',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), updated_at = NOW();

INSERT INTO kb_document_chunk (id, document_id, knowledge_base_id, chunk_index, content, keywords, created_at)
VALUES (
    1,
    1,
    1,
    0,
    'ArrayList 底层基于动态数组，适合按照下标快速访问元素。LinkedList 底层基于链表，适合频繁在头尾插入和删除。HashMap 使用哈希表保存键值对，常用于根据 key 快速查找 value。复习集合时要关注数据结构、扩容机制、时间复杂度和常见使用场景。',
    'Java,集合,ArrayList,LinkedList,HashMap',
    NOW()
)
ON DUPLICATE KEY UPDATE content = VALUES(content), keywords = VALUES(keywords);

INSERT INTO student (id, student_no, name, major, grade)
VALUES
    (1, '20230001', '李明', '软件工程', '2023'),
    (2, '20230002', '王同学', '软件工程', '2023')
ON DUPLICATE KEY UPDATE name = VALUES(name), major = VALUES(major), grade = VALUES(grade);

INSERT INTO course (id, course_code, course_name, credit)
VALUES
    (1, 'JAVA101', 'Java 程序设计', 3.0),
    (2, 'DB101', '数据库原理', 3.0)
ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), credit = VALUES(credit);

INSERT INTO score (id, student_id, course_id, score, semester)
VALUES
    (1, 1, 1, 88.50, '2024-2025-1'),
    (2, 1, 2, 91.00, '2024-2025-1'),
    (3, 2, 1, 82.00, '2024-2025-1')
ON DUPLICATE KEY UPDATE score = VALUES(score), semester = VALUES(semester);
