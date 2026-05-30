-- ============================================================
-- 迁移脚本：将旧版数据库升级到 Campus Knowledge Hub 版本
-- 如果表不存在则创建，如果表已存在则添加缺失字段
-- ============================================================
USE campus_ai;

-- 1. sys_user 表（新版）
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    role VARCHAR(20) NOT NULL COMMENT 'STUDENT / TEACHER / ADMIN',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 2. kb_knowledge_base 补充字段
ALTER TABLE kb_knowledge_base ADD COLUMN IF NOT EXISTS owner_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE kb_knowledge_base ADD COLUMN IF NOT EXISTS owner_name VARCHAR(50) NOT NULL DEFAULT '';
ALTER TABLE kb_knowledge_base ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE kb_knowledge_base ADD COLUMN IF NOT EXISTS document_count INT NOT NULL DEFAULT 0;
ALTER TABLE kb_knowledge_base ADD COLUMN IF NOT EXISTS chunk_count INT NOT NULL DEFAULT 0;

-- 3. kb_document 补充字段
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS file_name VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS file_type VARCHAR(20) NOT NULL DEFAULT 'txt';
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS file_size BIGINT NOT NULL DEFAULT 0;
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING';
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS chunk_count INT NOT NULL DEFAULT 0;
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS uploaded_by BIGINT NOT NULL DEFAULT 0;
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS processed_at DATETIME;

-- 4. chat_record 补充字段
ALTER TABLE chat_record ADD COLUMN IF NOT EXISTS username VARCHAR(50) NOT NULL DEFAULT '';
ALTER TABLE chat_record ADD COLUMN IF NOT EXISTS knowledge_base_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chat_record ADD COLUMN IF NOT EXISTS prompt_preview TEXT;
ALTER TABLE chat_record ADD COLUMN IF NOT EXISTS llm_mode VARCHAR(20) NOT NULL DEFAULT 'mock';
ALTER TABLE chat_record ADD COLUMN IF NOT EXISTS retrieval_time_ms BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chat_record ADD COLUMN IF NOT EXISTS generation_time_ms BIGINT NOT NULL DEFAULT 0;
