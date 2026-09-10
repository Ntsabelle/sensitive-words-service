package com.joseph.sensitivewordsservice.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to sanitize a word")

public class SensitiveWordRequest {
    @NotBlank(message = "Input word must not be blank")
    @Size(min = 1, max = 255, message = "Input word must be between 1 and 255 characters long")
    @Schema(description = "The word to be sanitized", example = "sensitiveWord", requiredMode = Schema.RequiredMode.REQUIRED)
    private String word;

    @Schema(description = "Whether the word is currently enforce by the sanitizer",
            example = "true", defaultValue = "true")
    private Boolean active = true;
}
