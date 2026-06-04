package com.liminghan.campusai.service.vector;

import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.vo.MatchedChunkVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PgVectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(PgVectorSearchService.class);

    private final EmbeddingClientRouter embeddingClient;
    private final boolean enabled;
    private final boolean hnswEnabled;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private volatile boolean schemaReady = false;
    private volatile boolean disabledByFailure = false;

    public PgVectorSearchService(EmbeddingClientRouter embeddingClient,
                                 @Value("${app.vector.enabled:true}") boolean enabled,
                                 @Value("${app.vector.hnsw-enabled:true}") boolean hnswEnabled,
                                 @Value("${app.vector.jdbc-url:}") String jdbcUrl,
                                 @Value("${app.vector.username:}") String username,
                                 @Value("${app.vector.password:}") String password) {
        this.embeddingClient = embeddingClient;
        this.enabled = enabled;
        this.hnswEnabled = hnswEnabled;
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

        List<ChunkEmbedding> chunkEmbeddings = new ArrayList<>();
        try {
            for (KbDocumentChunk chunk : chunks) {
                EmbeddingVector embedding = embeddingClient.embed(
                        chunk.getContent() + " " + nullToEmpty(chunk.getKeywords()));
                chunkEmbeddings.add(new ChunkEmbedding(chunk, embedding));
            }
        } catch (Exception e) {
            disableForCurrentRun("embed chunks failed", e);
            return;
        }
        if (chunkEmbeddings.isEmpty()) {
            return;
        }

        try (Connection conn = openConnection()) {
            ensureSchema(conn, chunkEmbeddings.get(0).embedding().dimension());
            String sql = """
                    INSERT INTO kb_chunk_vector
                    (chunk_id, knowledge_base_id, document_id, chunk_index, document_title, content, keywords,
                     embedding, embedding_provider, embedding_model, embedding_dimension, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?, now())
                    ON CONFLICT (chunk_id) DO UPDATE SET
                        knowledge_base_id = EXCLUDED.knowledge_base_id,
                        document_id = EXCLUDED.document_id,
                        chunk_index = EXCLUDED.chunk_index,
                        document_title = EXCLUDED.document_title,
                        content = EXCLUDED.content,
                        keywords = EXCLUDED.keywords,
                        embedding = EXCLUDED.embedding,
                        embedding_provider = EXCLUDED.embedding_provider,
                        embedding_model = EXCLUDED.embedding_model,
                        embedding_dimension = EXCLUDED.embedding_dimension,
                        updated_at = now()
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (ChunkEmbedding item : chunkEmbeddings) {
                    KbDocumentChunk chunk = item.chunk();
                    EmbeddingVector embedding = item.embedding();
                    ps.setLong(1, chunk.getId());
                    ps.setLong(2, chunk.getKnowledgeBaseId());
                    ps.setLong(3, chunk.getDocumentId());
                    ps.setInt(4, chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex());
                    ps.setString(5, titleMap.getOrDefault(chunk.getDocumentId(), "Unknown document"));
                    ps.setString(6, chunk.getContent());
                    ps.setString(7, chunk.getKeywords());
                    ps.setString(8, toPgVectorLiteral(embedding.values()));
                    ps.setString(9, embedding.provider());
                    ps.setString(10, embedding.model());
                    ps.setInt(11, embedding.dimension());
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

        EmbeddingVector queryEmbedding;
        try {
            queryEmbedding = embeddingClient.embed(question);
        } catch (Exception e) {
            disableForCurrentRun("embed query failed", e);
            return List.of();
        }

        try (Connection conn = openConnection()) {
            ensureSchema(conn, queryEmbedding.dimension());
            String vectorLiteral = toPgVectorLiteral(queryEmbedding.values());
            String sql = """
                    SELECT chunk_id, chunk_index, document_title, content,
                           GREATEST(0, ROUND(((1 - (embedding <=> ?::vector)) * 100)::numeric, 0)::int) AS score
                    FROM kb_chunk_vector
                    WHERE knowledge_base_id = ?
                      AND embedding_provider = ?
                      AND embedding_model = ?
                      AND embedding_dimension = ?
                    ORDER BY embedding <=> ?::vector
                    LIMIT ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, vectorLiteral);
                ps.setLong(2, knowledgeBaseId);
                ps.setString(3, queryEmbedding.provider());
                ps.setString(4, queryEmbedding.model());
                ps.setInt(5, queryEmbedding.dimension());
                ps.setString(6, vectorLiteral);
                ps.setInt(7, topK);
                try (ResultSet rs = ps.executeQuery()) {
                    List<MatchedChunkVO> results = new ArrayList<>();
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
            ensureSchema(conn, embeddingClient.dimension());
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM kb_chunk_vector WHERE " + column + " = ?")) {
                ps.setLong(1, value);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            disableForCurrentRun("delete stale vectors failed", e);
        }
    }

    private void ensureSchema(Connection conn, int dimension) throws SQLException {
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
                            embedding_provider VARCHAR(80) NOT NULL DEFAULT 'local',
                            embedding_model VARCHAR(160) NOT NULL DEFAULT 'hashing-embedding',
                            embedding_dimension INT NOT NULL DEFAULT %d,
                            updated_at TIMESTAMP NOT NULL DEFAULT now()
                        )
                        """.formatted(dimension, dimension));
                stmt.execute("ALTER TABLE kb_chunk_vector ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(80) NOT NULL DEFAULT 'local'");
                stmt.execute("ALTER TABLE kb_chunk_vector ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(160) NOT NULL DEFAULT 'hashing-embedding'");
                stmt.execute("ALTER TABLE kb_chunk_vector ADD COLUMN IF NOT EXISTS embedding_dimension INT NOT NULL DEFAULT " + dimension);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_kb_chunk_vector_kb ON kb_chunk_vector (knowledge_base_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_kb_chunk_vector_model ON kb_chunk_vector (embedding_provider, embedding_model, embedding_dimension)");
                createHnswIndexIfEnabled(stmt);
            }
            schemaReady = true;
            log.info("pgvector schema is ready. embeddingProvider={}, embeddingModel={}, dimension={}",
                    embeddingClient.provider(), embeddingClient.model(), dimension);
        }
    }

    private void createHnswIndexIfEnabled(Statement stmt) {
        if (!hnswEnabled) {
            return;
        }
        try {
            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_kb_chunk_vector_embedding_hnsw
                    ON kb_chunk_vector
                    USING hnsw (embedding vector_cosine_ops)
                    """);
        } catch (Exception e) {
            log.warn("pgvector HNSW index creation skipped. Cause: {}", e.getMessage());
        }
    }

    private void disableForCurrentRun(String action, Exception e) {
        disabledByFailure = true;
        log.warn("pgvector {}. Falling back to keyword retrieval. Cause: {}", action, e.getMessage());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String toPgVectorLiteral(double[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.ROOT, "%.6f", vector[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    private record ChunkEmbedding(KbDocumentChunk chunk, EmbeddingVector embedding) {
    }
}
