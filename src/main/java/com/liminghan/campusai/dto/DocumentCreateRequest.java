package com.liminghan.campusai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentCreateRequest {

    @NotBlank(message = "document title cannot be blank")
    private String title;

    @NotBlank(message = "document content cannot be blank")
    private String content;
}
