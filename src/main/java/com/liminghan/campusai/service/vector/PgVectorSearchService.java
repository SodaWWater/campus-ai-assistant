package com.liminghan.campusai.service.vector;

import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.vo.MatchedChunkVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.List;
import java.util.Map;

@Service
public class PgVectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(PgVectorSearchService.class);

    private final HashingEmbeddingService embeddingService;
    private final boolean enabled;
    private final int dimension;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private volatile boolean schemaReady = false;
    private volatile boolean disabledByFailure = false;

    public PgVectorSearchService(HashingEmbeddingService embeddingService,
                                 @Value("${app.vector.enabled:true}") boolean enabled,
                                 @Value("${app.vector.dimension:128}") int dimension,
                                 @Value("${app.vector.jdbc-url:}") String jdbcUrl,
                                 @Value("${app.vector.username:}") String username,
                                 @Value("${app.vector.password:}") String password) {
        this.embeddingService = embeddingService;
        this.enabled = enabled;
        this.dimension = dimension;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled && !disabledByFailure && jdbcUrl != null && !jdbcUrl.isBlank();
    }

    public void indexChunks(List<KbDocumentChunk> chunks, Map<Long, String> titleMap) {
        if (!isEnabled() || chunks == null || chunks.isEmpty()) {
            return;
        }
        try (Connection conn = openConnection()) {
            ensureSchema(conn);
            String sql = """
                    INSERT INTO kb_chunk_vector
                    (chunk_id, knowledge_base_id, document_id, chunk_index, document_title, content, keywords, embedding, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, now())
                    ON CONFLICT (chunk_id) DO UPDATE SET
                        knowledge_base_id = EXCLUDED.knowledge_base_id,
                        document_id = EXCLUDED.document_id,
                        chunk_index = EXCLUDED.chunk_index,
                        document_title = EXCLUDED.document_title,
                        content = EXCLUDED.content,
                        keywords = EXCLUDED.keywords,
                        embedding = EXCLUDED.embedding,
                        updated_at = now()
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (KbDocumentChunk chunk : chunks) {
                    ps.setLong(1, chunk.getId());
                    ps.setLong(2, chunk.getKnowledgeBaseId());
                    ps.setLong(3, chunk.getDocumentId());
                    ps.setInt(4, chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex());
                    ps.setString(5, titleMap.getOrDefault(chunk.getDocumentId(), "未知文档"));
                    ps.setString(6, chunk.getContent());
                    ps.setString(7, chunk.getKeywords());
                    ps.setString(8, embeddingService.toPgVectorLiteral(
                            embeddingService.embed(chunk.getContent() + " " + nullToEmpty(chunk.getKeywords()))));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (Exception e) {
            disableForCurrentRun("index chunks failed", e);
        }
    }

    public List<MatchedChunkVO> search(Long knowledgeBaseId, String question, int topK) {
        if (!isEnabled() || knowledgeBaseId == null) {
            return List.of();
        }
        try (Connection conn = openConnection()) {
            ensureSchema(conn);
            String vectorLiteral = embeddingService.toPgVectorLiteral(embeddingService.embed(question));
            String sql = """
                    SELECT chunk_id, chunk_index, document_title, content,
                           GREATEST(0, ROUND(((1 - (embedding <=> ?::vector)) * 100)::numeric, 0)::int) AS score
                    FROM kb_chunk_vector
                    WHERE knowledge_base_id = ?
                    ORDER BY embedding <=> ?::vector
                    LIMIT ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, vectorLiteral);
                ps.setLong(2, knowledgeBaseId);
                ps.setString(3, vectorLiteral);
                ps.setInt(4, topK);
                try (ResultSet rs = ps.executeQuery()) {
                    java.util.ArrayList<MatchedChunkVO> results = new java.util.ArrayList<>();
                    while (rs.next()) {
                        MatchedChunkVO vo = new MatchedChunkVO();
                        vo.setId(rs.getLong("chunk_id"));
                        vo.setChunkIndex(rs.getInt("chunk_index"));
                        vo.setDocumentTitle(rs.getString("document_title"));
                        vo.setContent(rs.getString("content"));
                        vo.setScore(rs.getInt("score"));
                        if (vo.getScore() > 0) {
                            results.add(vo);
                        }
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            disableForCurrentRun("vector search failed", e);
            return List.of();
        }
    }

    public void deleteByDocumentId(Long documentId) {
        deleteByColumn("document_id", documentId);
    }

    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        deleteByColumn("knowledge_base_id", knowledgeBaseId);
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private void deleteByColumn(String column, Long value) {
        if (!isEnabled() || value == null) {
            return;
        }
        try (Connection conn = openConnection()) {
            ensureSchema(conn);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM kb_chunk_vector WHERE " + column + " = ?")) {
                ps.setLong(1, value);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            disableForCurrentRun("delete stale vectors failed", e);
        }
    }

    private void ensureSchema(Connection conn) throws SQLException {
        if (schemaReady) {
            return;
        }
        synchronized (this) {
            if (schemaReady) {
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS kb_chunk_vector (
                            chunk_id BIGINT PRIMARY KEY,
                            knowledge_base_id BIGINT NOT NULL,
                            document_id BIGINT NOT NULL,
                            chunk_index INT NOT NULL,
                            document_title VARCHAR(255),
                            content TEXT NOT NULL,
                            keywords TEXT,
                            embedding vector(%d) NOT NULL,
                            updated_at TIMESTAMP NOT NULL DEFAULT now()
                        )
                        """.formatted(dimension));
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_kb_chunk_vector_kb ON kb_chunk_vector (knowledge_base_id)");
            }
            schemaReady = true;
            log.info("pgvector schema is ready.");
        }
    }

    private void disableForCurrentRun(String action, Exception e) {
        disabledByFailure = true;
        log.warn("pgvector {}. Falling back to keyword retrieval. Cause: {}", action, e.getMessage());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

