package com.liminghan.campusai.util;

import com.liminghan.campusai.entity.KbDocumentChunk;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    public String buildRagPrompt(String question, List<KbDocumentChunk> chunks) {
        String context = chunks.stream()
                .map(chunk -> "片段 " + chunk.getChunkIndex() + ": " + chunk.getContent())
                .collect(Collectors.joining("\n"));
        return """
                你是校园资料智能问答助手。
                请只基于给定资料回答，不要编造。
                如果资料不足，请回答“当前知识库未找到相关内容”。

                【资料片段】
                %s

                【用户问题】
                %s
                """.formatted(context, question);
    }

    public String buildGeneralPrompt(String question) {
        return """
                你是一个面向大学生的学习助手。
                请用简洁、可靠、适合本科生理解的方式回答。

                用户问题：%s
                """.formatted(question);
    }
}
