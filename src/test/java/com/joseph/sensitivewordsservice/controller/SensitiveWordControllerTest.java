package com.joseph.sensitivewordsservice.controller;

import com.joseph.sensitivewordsservice.dto.SensitiveWordRequest;
import com.joseph.sensitivewordsservice.dto.SensitiveWordResponse;
import com.joseph.sensitivewordsservice.entity.SensitiveWord;
import com.joseph.sensitivewordsservice.mapper.SensitiveWordMapper;
import com.joseph.sensitivewordsservice.service.SensitiveWordService;
import com.joseph.sensitivewordsservice.service.SanitizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SensitiveWordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SensitiveWordService sensitiveWordService;

    @Mock
    private SanitizationService sanitizationService;

    private ObjectMapper objectMapper;

    private SensitiveWord sensitiveWord;
    private SensitiveWordRequest request;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SensitiveWordController(sensitiveWordService, sanitizationService)).build();
        objectMapper = new ObjectMapper();

        sensitiveWord = SensitiveWord.builder()
                .id(1L)
                .word("badword")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        request = new SensitiveWordRequest();
        request.setWord("badword");
        request.setActive(true);
    }

    @Test
    void testCreateSensitiveWord_Success() throws Exception {
        when(sensitiveWordService.create(any(SensitiveWordRequest.class))).thenReturn(sensitiveWord);

        mockMvc.perform(post("/api/v1/sensitive-words")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.word").value("badword"))
                .andExpect(jsonPath("$.active").value(true));

        verify(sensitiveWordService, times(1)).create(any(SensitiveWordRequest.class));
    }

    @Test
    void testCreateSensitiveWord_InvalidInput() throws Exception {
        SensitiveWordRequest invalidRequest = new SensitiveWordRequest();
        invalidRequest.setWord(""); // empty word

        mockMvc.perform(post("/api/v1/sensitive-words")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSensitiveWordById_Success() throws Exception {
        when(sensitiveWordService.getById(1L)).thenReturn(sensitiveWord);

        mockMvc.perform(get("/api/v1/sensitive-words/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.word").value("badword"))
                .andExpect(jsonPath("$.active").value(true));

        verify(sensitiveWordService, times(1)).getById(1L);
    }

    @Test
    void testGetSensitiveWordById_NotFound() throws Exception {
        when(sensitiveWordService.getById(1L)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/v1/sensitive-words/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testListSensitiveWords_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SensitiveWord> page = new PageImpl<>(List.of(sensitiveWord), pageable, 1);
        when(sensitiveWordService.list(null, pageable)).thenReturn(page);

        mockMvc.perform(get("/api/v1/sensitive-words"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].word").value("badword"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(sensitiveWordService, times(1)).list(null, pageable);
    }

    @Test
    void testListSensitiveWords_FilterByActive() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SensitiveWord> page = new PageImpl<>(List.of(sensitiveWord), pageable, 1);
        when(sensitiveWordService.list(true, pageable)).thenReturn(page);

        mockMvc.perform(get("/api/v1/sensitive-words?active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(sensitiveWordService, times(1)).list(true, pageable);
    }

    @Test
    void testUpdateSensitiveWord_Success() throws Exception {
        SensitiveWord updated = SensitiveWord.builder()
                .id(1L)
                .word("newbadword")
                .active(false)
                .createdAt(sensitiveWord.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        request.setWord("newbadword");
        request.setActive(false);

        when(sensitiveWordService.update(eq(1L), any(SensitiveWordRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/sensitive-words/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("newbadword"))
                .andExpect(jsonPath("$.active").value(false));

        verify(sensitiveWordService, times(1)).update(eq(1L), any(SensitiveWordRequest.class));
    }

    @Test
    void testDeleteSensitiveWord_Success() throws Exception {
        doNothing().when(sensitiveWordService).delete(1L);

        mockMvc.perform(delete("/api/v1/sensitive-words/1"))
                .andExpect(status().isNoContent());

        verify(sensitiveWordService, times(1)).delete(1L);
    }

    @Test
    void testDeleteSensitiveWord_NotFound() throws Exception {
        doThrow(new RuntimeException("Not found")).when(sensitiveWordService).delete(1L);

        mockMvc.perform(delete("/api/v1/sensitive-words/1"))
                .andExpect(status().isInternalServerError());
    }
}
