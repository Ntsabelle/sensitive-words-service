package com.joseph.sensitivewordsservice.mapper;


import com.joseph.sensitivewordsservice.dto.SensitiveWordResponse;
import com.joseph.sensitivewordsservice.entity.SensitiveWord;

public class SensitiveWordMapper {
    private SensitiveWordMapper() {
        // Private constructor to prevent instantiation
    }

    public static SensitiveWordResponse toResponse(SensitiveWord sensitiveWord) {
       return SensitiveWordResponse.builder()
               .id(sensitiveWord.getId())
               .word(sensitiveWord.getWord())
               .active(sensitiveWord.isActive())
               .createdAt(sensitiveWord.getCreatedAt())
               .updatedAt(sensitiveWord.getUpdatedAt())
               .build();
    }
}
