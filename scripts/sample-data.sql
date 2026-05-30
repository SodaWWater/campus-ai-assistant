USE campus_ai;

-- ============================================================
-- 内置账号（密码 123456 的 BCrypt 哈希）
-- ============================================================
INSERT INTO sys_user (id, username, password, nickname, role, status, created_at, updated_at)
VALUES
    (1, 'student', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQ4sLmzqJ3aYfFVL8PzLG', '李同学', 'STUDENT', 'ENABLED', NOW(), NOW()),
    (2, 'teacher', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQ4sLmzqJ3aYfFVL8PzLG', '张老师', 'TEACHER', 'ENABLED', NOW(), NOW()),
    (3, 'admin',   '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQ4sLmzqJ3aYfFVL8PzLG', '管理员', 'ADMIN',   'ENABLED', NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), nickname = VALUES(nickname), role = VALUES(role), status = VALUES(status), updated_at = NOW();

-- ============================================================
-- 示例知识库（归属教师）
-- ============================================================
INSERT INTO kb_knowledge_base (id, name, description, owner_id, owner_name, visibility, document_count, chunk_count, created_at, updated_at)
VALUES (1, 'Java 复习资料库', '用于演示 RAG 问答的 Java 基础资料', 2, '张老师', 'PUBLIC', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), owner_id = VALUES(owner_id), owner_name = VALUES(owner_name), updated_at = NOW();

INSERT INTO kb_document (id, knowledge_base_id, title, content, file_name, file_type, file_size, status, error_message, chunk_count, uploaded_by, processed_at, created_at, updated_at)
VALUES (
    1,
    1,
    'Java 集合基础',
    'ArrayList 底层基于动态数组，适合按照下标快速访问元素。LinkedList 底层基于链表，适合频繁在头尾插入和删除。HashMap 使用哈希表保存键值对，常用于根据 key 快速查找 value。复习集合时要关注数据结构、扩容机制、时间复杂度和常见使用场景。',
    'java-collections.txt',
    'txt',
    204,
    'DONE',
    NULL,
    1,
    2,
    NOW(),
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), file_name = VALUES(file_name), status = VALUES(status), updated_at = NOW();

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

-- ============================================================
-- 学业数据（保持不变）
-- ============================================================
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
