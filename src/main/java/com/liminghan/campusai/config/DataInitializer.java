package com.liminghan.campusai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liminghan.campusai.entity.SysUser;
import com.liminghan.campusai.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时自动完成两件事：
 * 1. 迁移旧数据库（补充缺失字段，失败即跳过）
 * 2. 初始化三个演示账号（使用 BCrypt 正确编码密码 123456）
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(JdbcTemplate jdbcTemplate,
                           SysUserMapper sysUserMapper,
                           PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        migrateSchema();
        initDemoAccounts();
    }

    private void migrateSchema() {
        log.info("开始数据库迁移...");

        // sys_user
        safeExecute("""
            CREATE TABLE IF NOT EXISTS sys_user (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                nickname VARCHAR(50),
                role VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
                created_at DATETIME NOT NULL,
                updated_at DATETIME NOT NULL
            )
        """);

        // kb_knowledge_base 补充字段
        safeAlter("kb_knowledge_base", "ADD COLUMN owner_id BIGINT NOT NULL DEFAULT 0");
        safeAlter("kb_knowledge_base", "ADD COLUMN owner_name VARCHAR(50) NOT NULL DEFAULT ''");
        safeAlter("kb_knowledge_base", "ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC'");
        safeAlter("kb_knowledge_base", "ADD COLUMN document_count INT NOT NULL DEFAULT 0");
        safeAlter("kb_knowledge_base", "ADD COLUMN chunk_count INT NOT NULL DEFAULT 0");

        // kb_document 补充字段
        safeAlter("kb_document", "ADD COLUMN file_name VARCHAR(255) NOT NULL DEFAULT ''");
        safeAlter("kb_document", "ADD COLUMN file_type VARCHAR(20) NOT NULL DEFAULT 'txt'");
        safeAlter("kb_document", "ADD COLUMN file_size BIGINT NOT NULL DEFAULT 0");
        safeAlter("kb_document", "ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'");
        safeAlter("kb_document", "ADD COLUMN error_message VARCHAR(500)");
        safeAlter("kb_document", "ADD COLUMN chunk_count INT NOT NULL DEFAULT 0");
        safeAlter("kb_document", "ADD COLUMN uploaded_by BIGINT NOT NULL DEFAULT 0");
        safeAlter("kb_document", "ADD COLUMN processed_at DATETIME");

        // chat_record 补充字段
        safeAlter("chat_record", "ADD COLUMN username VARCHAR(50) NOT NULL DEFAULT ''");
        safeAlter("chat_record", "ADD COLUMN knowledge_base_id BIGINT NOT NULL DEFAULT 0");
        safeAlter("chat_record", "ADD COLUMN prompt_preview TEXT");
        safeAlter("chat_record", "ADD COLUMN llm_mode VARCHAR(20) NOT NULL DEFAULT 'mock'");
        safeAlter("chat_record", "ADD COLUMN retrieval_time_ms BIGINT NOT NULL DEFAULT 0");
        safeAlter("chat_record", "ADD COLUMN generation_time_ms BIGINT NOT NULL DEFAULT 0");
        safeAlter("chat_record", "ADD COLUMN conversation_id BIGINT NOT NULL DEFAULT 0");

        // 会话表
        safeExecute("""
            CREATE TABLE IF NOT EXISTS chat_conversation (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                title VARCHAR(200) NOT NULL DEFAULT '新对话',
                knowledge_base_id BIGINT,
                created_at DATETIME NOT NULL,
                updated_at DATETIME NOT NULL,
                INDEX idx_conv_user_id (user_id)
            )
        """);

        // student 表关联 sys_user（通过用户名匹配）
        safeAlter("student", "ADD COLUMN user_id BIGINT");
        ensureStudentUserLink("student", "李同学");   // 李明 → student
        ensureStudentUserLink("student2", "王同学");  // 王同学 → student2

        log.info("数据库迁移完成");
    }

    private void initDemoAccounts() {
        log.info("初始化演示账号...");

        ensureUser("student", "李同学", "STUDENT");
        ensureUser("student2", "王同学", "STUDENT");
        ensureUser("teacher", "张老师", "TEACHER");
        ensureUser("admin", "管理员", "ADMIN");

        log.info("演示账号就绪: student/123456, student2/123456, teacher/123456, admin/123456");
    }

    private void ensureUser(String username, String nickname, String role) {
        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (existing != null) {
            // 更新密码确保正确
            existing.setPassword(passwordEncoder.encode("123456"));
            existing.setNickname(nickname);
            existing.setRole(role);
            existing.setStatus("ENABLED");
            existing.setUpdatedAt(LocalDateTime.now());
            sysUserMapper.updateById(existing);
            return;
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus("ENABLED");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);
    }

    private void safeExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.debug("跳过: {}", e.getMessage());
        }
    }

    /**
     * 确保 student 表中指定姓名的记录关联到正确的 sys_user
     */
    private void ensureStudentUserLink(String username, String studentName) {
        try {
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
            if (user == null) return;
            jdbcTemplate.update(
                    "UPDATE student SET user_id = ? WHERE name = ? AND user_id IS NULL",
                    user.getId(), studentName);
        } catch (Exception e) {
            log.debug("关联学生失败: {} -> {}, {}", username, studentName, e.getMessage());
        }
    }

    private void safeAlter(String table, String action) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " " + action);
        } catch (Exception e) {
            // 字段已存在时跳过
            log.debug("跳过 {} {}: {}", table, action, e.getMessage());
        }
    }
}
