package com.joseph.sensitivewordsservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload containing the raw text to be sanitized")
public class SanitizeRequest {

    @NotBlank(message = "Input text must not be blank")
    @Schema(description = "The text to be sanitized",
            example = "This is a sample text with some sensitive words.",requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;
}
