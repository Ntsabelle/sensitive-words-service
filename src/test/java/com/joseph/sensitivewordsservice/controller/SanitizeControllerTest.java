package com.joseph.sensitivewordsservice.controller;

import com.joseph.sensitivewordsservice.dto.SanitizeRequest;
import com.joseph.sensitivewordsservice.dto.SanitizeResponse;
import com.joseph.sensitivewordsservice.service.SanitizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SanitizeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SanitizationService sanitizationService;

    private ObjectMapper objectMapper;

    private SanitizeRequest request;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SanitizeController(sanitizationService)).build();
        objectMapper = new ObjectMapper();

        request = new SanitizeRequest();
        request.setText("This is a badword text");
    }

    @Test
    void testSanitize_WithMatches() throws Exception {
        SanitizationService.SanitizeResult mockResult = new SanitizationService.SanitizeResult(
                "This is a ******* text", 1);
        
        when(sanitizationService.sanitize(anyString())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/sanitize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalText").value("This is a badword text"))
                .andExpect(jsonPath("$.sanitizedText").value("This is a ******* text"))
                .andExpect(jsonPath("$.matchCount").value(1));

        verify(sanitizationService, times(1)).sanitize("This is a badword text");
    }

    @Test
    void testSanitize_NoMatches() throws Exception {
        SanitizationService.SanitizeResult mockResult = new SanitizationService.SanitizeResult(
                "This is clean text", 0);
        
        when(sanitizationService.sanitize(anyString())).thenReturn(mockResult);

        request.setText("This is clean text");

        mockMvc.perform(post("/api/v1/sanitize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchCount").value(0))
                .andExpect(jsonPath("$.sanitizedText").value("This is clean text"));

        verify(sanitizationService, times(1)).sanitize("This is clean text");
    }

    @Test
    void testSanitize_MultipleMatches() throws Exception {
        String input = "bad bad bad";
        SanitizationService.SanitizeResult mockResult = new SanitizationService.SanitizeResult(
                "*** *** ***", 3);
        
        when(sanitizationService.sanitize(anyString())).thenReturn(mockResult);

        request.setText(input);

        mockMvc.perform(post("/api/v1/sanitize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchCount").value(3))
                .andExpect(jsonPath("$.sanitizedText").value("*** *** ***"));

        verify(sanitizationService, times(1)).sanitize(input);
    }

    @Test
    void testSanitize_InvalidInput() throws Exception {
        SanitizeRequest invalidRequest = new SanitizeRequest();
        invalidRequest.setText(""); // empty text

        mockMvc.perform(post("/api/v1/sanitize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSanitize_EmptyText() throws Exception {
        SanitizationService.SanitizeResult mockResult = new SanitizationService.SanitizeResult("", 0);
        
        when(sanitizationService.sanitize(anyString())).thenReturn(mockResult);

        request.setText("");

        mockMvc.perform(post("/api/v1/sanitize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSanitize_LongText() throws Exception {
        String longText = "This is a very long text with badword in the middle and it should be sanitized properly";
        SanitizationService.SanitizeResult mockResult = new SanitizationService.SanitizeResult(
                "This is a very long text with ******* in the middle and it should be sanitized properly", 1);
        
        when(sanitizationService.sanitize(anyString())).thenReturn(mockResult);

        request.setText(longText);

        mockMvc.perform(post("/api/v1/sanitize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchCount").value(1));

        verify(sanitizationService, times(1)).sanitize(longText);
    }
}
