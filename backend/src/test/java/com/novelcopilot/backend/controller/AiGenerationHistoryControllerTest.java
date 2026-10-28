package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.entity.Chapter;
import com.novelcopilot.backend.dto.AiGenerationHistoryItem;
import com.novelcopilot.backend.dto.AiGenerationHistoryPageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcopilot.backend.repository.ChapterRepository;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import com.novelcopilot.backend.service.AiGenerationHistoryService;
import com.novelcopilot.backend.service.StoryAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGenerationHistoryControllerTest {

    @Mock private AiGenerationHistoryService historyService;
    @Mock private StoryAccessService storyAccessService;
    @Mock private ChapterRepository chapterRepository;

    @Test
    void queriesHistoryUsingAuthenticatedUserAndBoundedPagination() {
        AiGenerationHistoryController controller = controller();
        AuthenticatedUserPrincipal user = new AuthenticatedUserPrincipal("user-1", null, null, null, null);
        when(chapterRepository.findByIdAndStoryId(50, 21)).thenReturn(Optional.of(new Chapter()));
        when(historyService.list(org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.eq(21),
                org.mockito.ArgumentMatchers.eq(50), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        controller.list(user, 21, 50, -2, 100);

        verify(storyAccessService).requireStoryAccess(21);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(historyService).list(org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.eq(21),
                org.mockito.ArgumentMatchers.eq(50), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(25, pageable.getValue().getPageSize());
    }

    @Test
    void rejectsChapterThatDoesNotBelongToStory() {
        AiGenerationHistoryController controller = controller();
        AuthenticatedUserPrincipal user = new AuthenticatedUserPrincipal("user-1", null, null, null, null);
        when(chapterRepository.findByIdAndStoryId(50, 21)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.list(user, 21, 50, 0, 10));

        verify(historyService, never()).list(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        );
    }

    @Test
    void returnsStablePageWithStructuredTitleOptions() throws Exception {
        AiGenerationHistoryController controller = controller();
        AuthenticatedUserPrincipal user = new AuthenticatedUserPrincipal("user-1", null, null, null, null);
        when(chapterRepository.findByIdAndStoryId(50, 21)).thenReturn(Optional.of(new Chapter()));
        var structured = new ObjectMapper().readTree(
                "{\"options\":[\"O Zumbido no Bueiro\",\"Memórias de Outro Mundo\",\"Sombras na Fábrica de Processamento\"]}"
        );
        var item = new AiGenerationHistoryItem(
                7L, 38L, "GENERATE_CHAPTER_TITLE", "STRUCTURED", null, structured,
                "Cloudflare", "gemma", null
        );
        when(historyService.list(org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.eq(21),
                org.mockito.ArgumentMatchers.eq(50), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        AiGenerationHistoryPageResponse response = controller.list(user, 21, 50, 0, 10).getBody();

        assertEquals(1, response.content().size());
        assertEquals(38L, response.content().get(0).usageId());
        assertEquals(
                List.of("O Zumbido no Bueiro", "Memórias de Outro Mundo", "Sombras na Fábrica de Processamento"),
                response.content().get(0).structuredResult().get("options")
        );
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"structuredResult\""));
        org.junit.jupiter.api.Assertions.assertFalse(json.contains("pageable"));
    }

    private AiGenerationHistoryController controller() {
        return new AiGenerationHistoryController(historyService, storyAccessService, chapterRepository);
    }
}
