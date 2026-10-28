package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.UpdateChapterRequest;
import com.novelcopilot.backend.entity.Chapter;
import com.novelcopilot.backend.repository.ChapterRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChapterServiceTest {

    @Test
    void twoSessionsCannotOverwriteANewerChapterVersion() {
        ChapterRepository repository = mock(ChapterRepository.class);
        ChapterService service = new ChapterService(repository);
        Chapter storedChapter = chapter(7, 0L, "Texto original");

        when(repository.findById(7)).thenReturn(Optional.of(storedChapter));
        when(repository.saveAndFlush(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter saved = invocation.getArgument(0);
            saved.setVersion(saved.getVersion() + 1);
            return saved;
        });

        Chapter firstSessionResult = service.updateChapter(
                7,
                request("Texto da primeira sessão", 0L)
        ).orElseThrow();

        assertEquals(1L, firstSessionResult.getVersion());
        assertEquals("Texto da primeira sessão", storedChapter.getContent());

        ChapterVersionConflictException conflict = assertThrows(
                ChapterVersionConflictException.class,
                () -> service.updateChapter(
                        7,
                        request("Texto antigo da segunda sessão", 0L)
                )
        );

        assertEquals(0L, conflict.getRequestedVersion());
        assertEquals(1L, conflict.getCurrentVersion());
        assertEquals("Texto da primeira sessão", storedChapter.getContent());
        verify(repository).saveAndFlush(any(Chapter.class));
    }

    @Test
    void updateRequiresAChapterVersion() {
        ChapterRepository repository = mock(ChapterRepository.class);
        ChapterService service = new ChapterService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateChapter(7, request("Texto", null))
        );

        verify(repository, never()).findById(any());
    }

    private static Chapter chapter(Integer id, Long version, String content) {
        Chapter chapter = new Chapter();
        chapter.setVersion(version);
        chapter.setContent(content);
        return chapter;
    }

    private static UpdateChapterRequest request(String content, Long version) {
        return new UpdateChapterRequest(
                "Capítulo 1",
                1,
                "chapter",
                "",
                "",
                content,
                version
        );
    }
}
