package com.joseph.sensitivewordsservice.service;

import com.joseph.sensitivewordsservice.dto.SensitiveWordRequest;
import com.joseph.sensitivewordsservice.entity.SensitiveWord;
import com.joseph.sensitivewordsservice.exception.DuplicateWordException;
import com.joseph.sensitivewordsservice.exception.ResourceNotFoundException;
import com.joseph.sensitivewordsservice.repository.SensitiveWordRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SensitiveWordService {
    private final SensitiveWordRepository sensitiveWordRepository;
    private final SanitizationService sanitizationService;

    /**
     * Create a new sensitive word.
     * Normalizes word (trim), checks for duplicates (case-insensitive),
     * and saves to database. Note: Controller handles cache refresh.
     * 
     * @param request SensitiveWordRequest with word, active status
     * @return Saved SensitiveWord entity
     * @throws DuplicateWordException if word already exists (case-insensitive)
     */
    public SensitiveWord create(SensitiveWordRequest request) {
        String word = normalize(request.getWord());
        log.debug("Create sensitive word: word='{}', active={}", word, request.getActive());

        if(sensitiveWordRepository.existsByWordIgnoreCase(word)){
            throw new DuplicateWordException("Sensitive word already exists: " + word);
        }
        
        SensitiveWord entity = SensitiveWord.builder()
                .word(word)
                .active(request.getActive() == null || request.getActive())
                .build();

        SensitiveWord saved = sensitiveWordRepository.save(entity);
        sanitizationService.refresh();
        log.info("Sensitive word created: id={}, word='{}', active={}", saved.getId(), saved.getWord(), saved.isActive());
        return saved;
    }

    /**
     * Retrieve a sensitive word by ID.
     * Read-only transaction.
     * 
     * @param id The word ID
     * @return SensitiveWord entity
     * @throws ResourceNotFoundException if word not found
     */
    @Transactional(readOnly = true)
    public SensitiveWord getById(Long id) {
        log.debug("Get sensitive word by id: {}", id);
        return sensitiveWordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensitive word not found with id: " + id));
    }

    /**
     * List all sensitive words with pagination.
     * Optionally filter by active status.
     * Read-only transaction.
     * 
     * @param active Filter by active status (null = no filter)
     * @param pageable Pagination info
     * @return Page of SensitiveWord entities
     */
    @Transactional(readOnly = true)
    public Page<SensitiveWord> list(Boolean active, Pageable pageable) {
        log.debug("List sensitive words: active={}, pageable={}", active, pageable);
        if(active == null){
            // No filter - return all words
            return sensitiveWordRepository.findAll(pageable);
        }
        // Filter by active status
        return sensitiveWordRepository.findAllByActive(active, pageable);
    }

    /**
     * Update an existing sensitive word.
     * Validates new word is not a duplicate (unless it's the same word).
     * 
     * @param id The word ID to update
     * @param request Updated SensitiveWordRequest
     * @return Updated SensitiveWord entity
     * @throws ResourceNotFoundException if word not found
     * @throws DuplicateWordException if new word already exists (case-insensitive)
     */
    public SensitiveWord update(Long id, SensitiveWordRequest request) {
        log.debug("Update sensitive word: id={}, request={}", id, request);
        SensitiveWord existing = getById(id);
        String oldWord = existing.getWord();

        String newWord = normalize(request.getWord());
        log.debug("Updating sensitive word: id={}, oldWord='{}', newWord='{}', active={}", id, oldWord, newWord, request.getActive());
        
        if(!newWord.equalsIgnoreCase(existing.getWord())
                && sensitiveWordRepository.existsByWordIgnoreCase(newWord)){
            throw new DuplicateWordException("Sensitive word already exists: " + newWord);
        }
        
        existing.setWord(newWord);
        existing.setActive(request.getActive() == null || request.getActive());
        SensitiveWord saved = sensitiveWordRepository.save(existing);
        sanitizationService.refresh();
        log.info("Sensitive word updated: id={}, oldWord='{}', newWord='{}', active={}",
                saved.getId(), oldWord, saved.getWord(), saved.isActive());
        return saved;
    }

    /**
     * Delete a sensitive word by ID.
     * 
     * @param id The word ID to delete
     * @throws ResourceNotFoundException if word not found
     */
    public void delete(Long id) {
        SensitiveWord existing = getById(id);
        log.debug("Delete sensitive word: id={}, word='{}'", existing.getId(), existing.getWord());
        sensitiveWordRepository.delete(existing);
        sanitizationService.refresh();
        log.info("Sensitive word deleted: id={}, word='{}'", existing.getId(), existing.getWord());
    }

    /**
     * Normalize word input - trim whitespace.
     * 
     * @param word Raw input word
     * @return Trimmed word
     */
    private String normalize(String word) {
        return word.trim();
    }
}
