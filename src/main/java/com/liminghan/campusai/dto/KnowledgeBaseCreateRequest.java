package com.liminghan.campusai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeBaseCreateRequest {

    @NotBlank(message = "knowledge base name cannot be blank")
    private String name;

    private String description;

    private String visibility = "PUBLIC";
}
