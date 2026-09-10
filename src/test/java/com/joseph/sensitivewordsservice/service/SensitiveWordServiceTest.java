package com.joseph.sensitivewordsservice.service;

import com.joseph.sensitivewordsservice.dto.SensitiveWordRequest;
import com.joseph.sensitivewordsservice.entity.SensitiveWord;
import com.joseph.sensitivewordsservice.exception.DuplicateWordException;
import com.joseph.sensitivewordsservice.exception.ResourceNotFoundException;
import com.joseph.sensitivewordsservice.repository.SensitiveWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensitiveWordServiceTest {

    @Mock
    private SensitiveWordRepository sensitiveWordRepository;

    @Mock
    private SanitizationService sanitizationService;

    @InjectMocks
    private SensitiveWordService sensitiveWordService;

    private SensitiveWordRequest request;
    private SensitiveWord sensitiveWord;

    @BeforeEach
    void setUp() {
        request = new SensitiveWordRequest();
        request.setWord("badword");
        request.setActive(true);

        sensitiveWord = SensitiveWord.builder()
                .id(1L)
                .word("badword")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testCreateSensitiveWord_Success() {
        when(sensitiveWordRepository.existsByWordIgnoreCase(anyString())).thenReturn(false);
        when(sensitiveWordRepository.save(any(SensitiveWord.class))).thenReturn(sensitiveWord);

        SensitiveWord result = sensitiveWordService.create(request);

        assertNotNull(result);
        assertEquals("badword", result.getWord());
        assertTrue(result.isActive());
        verify(sensitiveWordRepository, times(1)).save(any(SensitiveWord.class));
        verify(sanitizationService, times(1)).refresh();
    }

    @Test
    void testCreateSensitiveWord_DuplicateWord() {
        when(sensitiveWordRepository.existsByWordIgnoreCase(anyString())).thenReturn(true);

        assertThrows(DuplicateWordException.class, () -> sensitiveWordService.create(request));
        verify(sensitiveWordRepository, never()).save(any(SensitiveWord.class));
    }

    @Test
    void testCreateSensitiveWord_DefaultActive() {
        request.setActive(null);
        when(sensitiveWordRepository.existsByWordIgnoreCase(anyString())).thenReturn(false);
        when(sensitiveWordRepository.save(any(SensitiveWord.class))).thenReturn(sensitiveWord);

        SensitiveWord result = sensitiveWordService.create(request);

        assertNotNull(result);
        assertTrue(result.isActive());
    }

    @Test
    void testGetByIdSuccess() {
        when(sensitiveWordRepository.findById(1L)).thenReturn(Optional.of(sensitiveWord));

        SensitiveWord result = sensitiveWordService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("badword", result.getWord());
    }

    @Test
    void testGetByIdNotFound() {
        when(sensitiveWordRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sensitiveWordService.getById(1L));
    }

    @Test
    void testListAllWords() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SensitiveWord> page = new PageImpl<>(List.of(sensitiveWord), pageable, 1);
        when(sensitiveWordRepository.findAll(pageable)).thenReturn(page);

        Page<SensitiveWord> result = sensitiveWordService.list(null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("badword", result.getContent().get(0).getWord());
    }

    @Test
    void testListActiveWords() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SensitiveWord> page = new PageImpl<>(List.of(sensitiveWord), pageable, 1);
        when(sensitiveWordRepository.findAllByActive(true, pageable)).thenReturn(page);

        Page<SensitiveWord> result = sensitiveWordService.list(true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(sensitiveWordRepository, times(1)).findAllByActive(true, pageable);
    }

    @Test
    void testUpdateSensitiveWord_Success() {
        when(sensitiveWordRepository.findById(1L)).thenReturn(Optional.of(sensitiveWord));
        when(sensitiveWordRepository.existsByWordIgnoreCase("newbadword")).thenReturn(false);
        when(sensitiveWordRepository.save(any(SensitiveWord.class))).thenReturn(sensitiveWord);

        request.setWord("newbadword");
        SensitiveWord result = sensitiveWordService.update(1L, request);

        assertNotNull(result);
        verify(sensitiveWordRepository, times(1)).save(any(SensitiveWord.class));
        verify(sanitizationService, times(1)).refresh();
    }

    @Test
    void testUpdateSensitiveWord_NotFound() {
        when(sensitiveWordRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sensitiveWordService.update(1L, request));
    }

    @Test
    void testUpdateSensitiveWord_DuplicateNewWord() {
        when(sensitiveWordRepository.findById(1L)).thenReturn(Optional.of(sensitiveWord));
        when(sensitiveWordRepository.existsByWordIgnoreCase("anotheword")).thenReturn(true);

        request.setWord("anotheword");
        assertThrows(DuplicateWordException.class, () -> sensitiveWordService.update(1L, request));
    }

    @Test
    void testDeleteSensitiveWord_Success() {
        when(sensitiveWordRepository.findById(1L)).thenReturn(Optional.of(sensitiveWord));

        sensitiveWordService.delete(1L);

        verify(sensitiveWordRepository, times(1)).delete(sensitiveWord);
        verify(sanitizationService, times(1)).refresh();
    }

    @Test
    void testDeleteSensitiveWord_NotFound() {
        when(sensitiveWordRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sensitiveWordService.delete(1L));
    }

    @Test
    void testCreateSensitiveWord_TrimsWhitespace() {
        request.setWord("  badword  ");
        when(sensitiveWordRepository.existsByWordIgnoreCase(anyString())).thenReturn(false);
        when(sensitiveWordRepository.save(any(SensitiveWord.class))).thenReturn(sensitiveWord);

        SensitiveWord result = sensitiveWordService.create(request);

        assertNotNull(result);
        verify(sensitiveWordRepository, times(1)).save(any(SensitiveWord.class));
    }
}
