package com.liminghan.campusai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Initializes the local demo environment on startup.
 *
 * The SQL scripts are idempotent, so restarting the app refreshes the demo
 * accounts, course knowledge bases, source documents, chunks, conversations
 * and academic records without duplicating rows.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        runScript("db/init.sql", "schema");
        migrateLegacyColumns();
        runScript("db/sample-data.sql", "sample data");
        fixPasswords();
    }

    /**
     * Ensure demo account passwords are correctly BCrypt-encoded for "123456".
     * The hash in sample-data.sql may be for a different password, so we
     * overwrite it with a fresh correct encoding on every startup.
     */
    private void fixPasswords() {
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hash = encoder.encode("123456");
        int updated = jdbcTemplate.update(
                "UPDATE sys_user SET password = ? WHERE username IN ('student', 'teacher', 'admin', 'student2')", hash);
        log.info("Demo passwords refreshed: {} account(s) updated.", updated);
    }

    private void runScript(String path, String label) {
        Resource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.warn("Skip {} initialization because {} does not exist on classpath.", label, path);
            return;
        }

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.setSeparator(";");
        populator.setContinueOnError(false);
        populator.addScript(resource);

        try {
            populator.execute(jdbcTemplate.getDataSource());
            log.info("Demo {} initialized from {}.", label, path);
        } catch (Exception e) {
            log.error("Failed to initialize {} from {}.", label, path, e);
            throw e;
        }
    }

    private void migrateLegacyColumns() {
        safeAlter("kb_knowledge_base", "ADD COLUMN owner_id BIGINT NOT NULL DEFAULT 0");
        safeAlter("kb_knowledge_base", "ADD COLUMN owner_name VARCHAR(50) NOT NULL DEFAULT ''");
        safeAlter("kb_knowledge_base", "ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC'");
        safeAlter("kb_knowledge_base", "ADD COLUMN document_count INT NOT NULL DEFAULT 0");
        safeAlter("kb_knowledge_base", "ADD COLUMN chunk_count INT NOT NULL DEFAULT 0");

        safeAlter("kb_document", "ADD COLUMN file_name VARCHAR(255) NOT NULL DEFAULT ''");
        safeAlter("kb_document", "ADD COLUMN file_type VARCHAR(20) NOT NULL DEFAULT 'txt'");
        safeAlter("kb_document", "ADD COLUMN file_size BIGINT NOT NULL DEFAULT 0");
        safeAlter("kb_document", "ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'");
        safeAlter("kb_document", "ADD COLUMN error_message VARCHAR(500)");
        safeAlter("kb_document", "ADD COLUMN chunk_count INT NOT NULL DEFAULT 0");
        safeAlter("kb_document", "ADD COLUMN uploaded_by BIGINT NOT NULL DEFAULT 0");
        safeAlter("kb_document", "ADD COLUMN processed_at DATETIME");

        safeAlter("chat_record", "ADD COLUMN username VARCHAR(50) NOT NULL DEFAULT ''");
        safeAlter("chat_record", "ADD COLUMN knowledge_base_id BIGINT NOT NULL DEFAULT 0");
        safeAlter("chat_record", "ADD COLUMN prompt_preview TEXT");
        safeAlter("chat_record", "ADD COLUMN llm_mode VARCHAR(20) NOT NULL DEFAULT 'mock'");
        safeAlter("chat_record", "ADD COLUMN retrieval_time_ms BIGINT NOT NULL DEFAULT 0");
        safeAlter("chat_record", "ADD COLUMN generation_time_ms BIGINT NOT NULL DEFAULT 0");
        safeAlter("chat_record", "ADD COLUMN conversation_id BIGINT NOT NULL DEFAULT 0");

        safeAlter("student", "ADD COLUMN user_id BIGINT");
        log.info("Legacy schema migration checked.");
    }

    private void safeAlter(String table, String action) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " " + action);
        } catch (Exception e) {
            log.debug("Skip alter {} {}: {}", table, action, e.getMessage());
        }
    }
}
