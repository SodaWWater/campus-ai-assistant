CREATE DATABASE IF NOT EXISTS campus_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE campus_ai;

-- ============================================================
-- 用户表 (Phase 6: 多角色认证)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    role VARCHAR(20) NOT NULL COMMENT 'STUDENT / TEACHER / ADMIN',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS kb_knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    owner_id BIGINT NOT NULL DEFAULT 0,
    owner_name VARCHAR(50) NOT NULL DEFAULT '',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC / PRIVATE / COURSE_ONLY',
    document_count INT NOT NULL DEFAULT 0,
    chunk_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_kb_owner_id (owner_id)
);

CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    knowledge_base_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL DEFAULT '',
    file_type VARCHAR(20) NOT NULL DEFAULT 'txt' COMMENT 'txt / md / pdf / docx / doc',
    file_size BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING / DONE / FAILED',
    error_message VARCHAR(500),
    chunk_count INT NOT NULL DEFAULT 0,
    uploaded_by BIGINT NOT NULL DEFAULT 0,
    processed_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_kb_document_kb_id (knowledge_base_id),
    INDEX idx_kb_document_status (status)
);

CREATE TABLE IF NOT EXISTS kb_document_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    keywords VARCHAR(1000),
    created_at DATETIME NOT NULL,
    INDEX idx_chunk_kb_id (knowledge_base_id),
    INDEX idx_chunk_document_id (document_id)
);

CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    knowledge_base_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_conversation_user_id (user_id),
    INDEX idx_conversation_kb_id (knowledge_base_id)
);

CREATE TABLE IF NOT EXISTS chat_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL DEFAULT '',
    conversation_id BIGINT,
    knowledge_base_id BIGINT NOT NULL DEFAULT 0,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    matched_chunk_ids VARCHAR(500),
    prompt_preview TEXT,
    llm_mode VARCHAR(20) NOT NULL DEFAULT 'mock',
    retrieval_time_ms BIGINT NOT NULL DEFAULT 0,
    generation_time_ms BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_chat_user_id (user_id),
    INDEX idx_chat_conversation_id (conversation_id),
    INDEX idx_chat_kb_id (knowledge_base_id)
);

CREATE TABLE IF NOT EXISTS student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    student_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    major VARCHAR(100),
    grade VARCHAR(20),
    INDEX idx_student_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(50) NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    credit DECIMAL(4,1) NOT NULL
);

CREATE TABLE IF NOT EXISTS score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    semester VARCHAR(50) NOT NULL,
    INDEX idx_score_student_id (student_id),
    INDEX idx_score_course_id (course_id)
);
