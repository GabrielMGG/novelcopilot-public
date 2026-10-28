package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.dto.AiGenerationHistoryItem;
import com.novelcopilot.backend.dto.AiGenerationHistoryPageResponse;
import com.novelcopilot.backend.repository.ChapterRepository;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import com.novelcopilot.backend.service.AiGenerationHistoryService;
import com.novelcopilot.backend.service.StoryAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/stories/{storyId}/chapters/{chapterId}/ai-history")
public class AiGenerationHistoryController {

    private static final int MAX_PAGE_SIZE = 25;

    private final AiGenerationHistoryService historyService;
    private final StoryAccessService storyAccessService;
    private final ChapterRepository chapterRepository;

    public AiGenerationHistoryController(
            AiGenerationHistoryService historyService,
            StoryAccessService storyAccessService,
            ChapterRepository chapterRepository
    ) {
        this.historyService = historyService;
        this.storyAccessService = storyAccessService;
        this.chapterRepository = chapterRepository;
    }

    @GetMapping
    public ResponseEntity<AiGenerationHistoryPageResponse> list(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable Integer storyId,
            @PathVariable Integer chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        storyAccessService.requireStoryAccess(storyId);
        if (chapterRepository.findByIdAndStoryId(chapterId, storyId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Capítulo não encontrado");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        Page<AiGenerationHistoryItem> history = historyService.list(user.getId(), storyId, chapterId, pageable);
        return ResponseEntity.ok(AiGenerationHistoryPageResponse.from(history));
    }
}
