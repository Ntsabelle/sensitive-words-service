package com.joseph.sensitivewordsservice.repository;

import com.joseph.sensitivewordsservice.entity.SensitiveWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SensitiveWordRepositoryTest {

    @Autowired
    private SensitiveWordRepository sensitiveWordRepository;

    @Autowired
    private TestEntityManager entityManager;

    private SensitiveWord sensitiveWord;

    @BeforeEach
    void setUp() {
        sensitiveWord = SensitiveWord.builder()
                .word("badword")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testSaveAndFindById() {
        SensitiveWord saved = sensitiveWordRepository.save(sensitiveWord);
        entityManager.flush();

        Optional<SensitiveWord> found = sensitiveWordRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("badword", found.get().getWord());
        assertTrue(found.get().isActive());
    }

    @Test
    void testFindByWordIgnoreCase() {
        sensitiveWordRepository.save(sensitiveWord);
        entityManager.flush();

        Optional<SensitiveWord> found = sensitiveWordRepository.findByWordIgnoreCase("BADWORD");

        assertTrue(found.isPresent());
        assertEquals("badword", found.get().getWord());
    }

    @Test
    void testFindByWordIgnoreCase_NotFound() {
        sensitiveWordRepository.save(sensitiveWord);
        entityManager.flush();

        Optional<SensitiveWord> found = sensitiveWordRepository.findByWordIgnoreCase("nonexistent");

        assertTrue(found.isEmpty());
    }

    @Test
    void testExistsByWordIgnoreCase_True() {
        sensitiveWordRepository.save(sensitiveWord);
        entityManager.flush();

        boolean exists = sensitiveWordRepository.existsByWordIgnoreCase("BADWORD");

        assertTrue(exists);
    }

    @Test
    void testExistsByWordIgnoreCase_False() {
        sensitiveWordRepository.save(sensitiveWord);
        entityManager.flush();

        boolean exists = sensitiveWordRepository.existsByWordIgnoreCase("nonexistent");

        assertFalse(exists);
    }

    @Test
    void testFindAllActiveWords() {
        SensitiveWord word1 = SensitiveWord.builder()
                .word("badword1")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        SensitiveWord word2 = SensitiveWord.builder()
                .word("badword2")
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        SensitiveWord word3 = SensitiveWord.builder()
                .word("badword3")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sensitiveWordRepository.saveAll(List.of(word1, word2, word3));
        entityManager.flush();

        List<String> activeWords = sensitiveWordRepository.findAllActiveWords();

        assertEquals(2, activeWords.size());
        assertTrue(activeWords.contains("badword1"));
        assertTrue(activeWords.contains("badword3"));
        assertFalse(activeWords.contains("badword2"));
    }

    @Test
    void testFindAllByActiveTrue() {
        SensitiveWord word1 = SensitiveWord.builder()
                .word("active1")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        SensitiveWord word2 = SensitiveWord.builder()
                .word("inactive")
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sensitiveWordRepository.saveAll(List.of(word1, word2));
        entityManager.flush();

        List<SensitiveWord> activeWords = sensitiveWordRepository.findAllByActiveTrue();

        assertEquals(1, activeWords.size());
        assertEquals("active1", activeWords.get(0).getWord());
    }

    @Test
    void testFindAllByActive_WithPagination() {
        for (int i = 1; i <= 5; i++) {
            SensitiveWord word = SensitiveWord.builder()
                    .word("word" + i)
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            sensitiveWordRepository.save(word);
        }
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 2);
        Page<SensitiveWord> page = sensitiveWordRepository.findAllByActive(true, pageable);

        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void testFindAllByActive_False() {
        SensitiveWord word1 = SensitiveWord.builder()
                .word("active")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        SensitiveWord word2 = SensitiveWord.builder()
                .word("inactive1")
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        SensitiveWord word3 = SensitiveWord.builder()
                .word("inactive2")
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sensitiveWordRepository.saveAll(List.of(word1, word2, word3));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);
        Page<SensitiveWord> page = sensitiveWordRepository.findAllByActive(false, pageable);

        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void testFindAll_WithPagination() {
        for (int i = 1; i <= 3; i++) {
            SensitiveWord word = SensitiveWord.builder()
                    .word("word" + i)
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            sensitiveWordRepository.save(word);
        }
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 2);
        Page<SensitiveWord> page = sensitiveWordRepository.findAll(pageable);

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void testDelete() {
        SensitiveWord saved = sensitiveWordRepository.save(sensitiveWord);
        entityManager.flush();

        sensitiveWordRepository.delete(saved);
        entityManager.flush();

        Optional<SensitiveWord> deleted = sensitiveWordRepository.findById(saved.getId());

        assertTrue(deleted.isEmpty());
    }

    @Test
    void testUpdate() {
        SensitiveWord saved = sensitiveWordRepository.save(sensitiveWord);
        entityManager.flush();

        saved.setWord("updatedword");
        saved.setActive(false);
        SensitiveWord updated = sensitiveWordRepository.save(saved);
        entityManager.flush();

        SensitiveWord found = sensitiveWordRepository.findById(updated.getId()).orElseThrow();

        assertEquals("updatedword", found.getWord());
        assertFalse(found.isActive());
    }
}
