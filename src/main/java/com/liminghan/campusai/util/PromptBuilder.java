package com.liminghan.campusai.util;

import com.liminghan.campusai.entity.KbDocumentChunk;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    /**
     * 构建 RAG 辅助的对话 Prompt。
     * RAG 资料作为「参考资料」注入，但 LLM 可以结合自身知识回答，
     * 当资料与问题无关时 LLM 应自行判断并给出合理回答。
     */
    public String buildRagPrompt(String question, List<KbDocumentChunk> chunks, String conversationHistory) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是校园知识库智能助手，帮助学生解答课程相关问题。\n");
        sb.append("回答原则：\n");
        sb.append("1. 优先参考提供的课程资料，在回答中引用资料内容\n");
        sb.append("2. 如果资料与问题无关或资料不足，可以使用你自己的知识补充回答\n");
        sb.append("3. 回答要准确、简洁、适合学生理解\n\n");

        if (conversationHistory != null && !conversationHistory.isBlank()) {
            sb.append("【历史对话】\n");
            sb.append(conversationHistory).append("\n\n");
        }

        if (chunks != null && !chunks.isEmpty()) {
            sb.append("【参考资料（来自课程知识库）】\n");
            for (KbDocumentChunk chunk : chunks) {
                sb.append("--- 资料片段 ").append(chunk.getChunkIndex()).append(" ---\n");
                sb.append(chunk.getContent()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("【用户问题】\n");
        sb.append(question);
        return sb.toString();
    }

    public String buildRagPrompt(String question, List<KbDocumentChunk> chunks) {
        return buildRagPrompt(question, chunks, null);
    }

    public String buildGeneralPrompt(String question, String conversationHistory) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是校园学习助手，帮助学生解答学习相关问题。\n");
        sb.append("请用简洁、可靠、适合本科生理解的方式回答。\n\n");

        if (conversationHistory != null && !conversationHistory.isBlank()) {
            sb.append("【历史对话】\n");
            sb.append(conversationHistory).append("\n\n");
        }

        sb.append("用户问题：").append(question);
        return sb.toString();
    }

    public String buildGeneralPrompt(String question) {
        return buildGeneralPrompt(question, null);
    }
}
