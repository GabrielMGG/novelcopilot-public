package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.dto.NovelAiAction;
import com.novelcopilot.backend.dto.NovelAiRequest;
import com.novelcopilot.backend.repository.ChapterRepository;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import com.novelcopilot.backend.service.NovelAiService;
import com.novelcopilot.backend.service.StoryAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NovelAiControllerTest {

    @Mock private NovelAiService novelAiService;
    @Mock private StoryAccessService storyAccessService;
    @Mock private ChapterRepository chapterRepository;

    @Test
    void rejectsGenerationWhenChapterDoesNotBelongToRequestedStory() {
        NovelAiController controller = new NovelAiController(novelAiService, storyAccessService, chapterRepository);
        NovelAiRequest request = new NovelAiRequest(
                NovelAiAction.CONTINUE_SCENE,
                21,
                50,
                null, null, null, null, null, null, null, null, null,
                false,
                "history-isolation"
        );
        when(chapterRepository.findByIdAndStoryId(50, 21)).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> controller.generate(
                        new AuthenticatedUserPrincipal("user-1", null, null, null, null),
                        request
                )
        );

        verify(storyAccessService).requireStoryAccess(21);
        verify(storyAccessService).requireChapterAccess(50);
        verify(novelAiService, never()).generate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }
}
