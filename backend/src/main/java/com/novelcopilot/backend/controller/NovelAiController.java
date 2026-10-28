package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.dto.NovelAiRequest;
import com.novelcopilot.backend.dto.NovelAiResponse;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import com.novelcopilot.backend.repository.ChapterRepository;
import com.novelcopilot.backend.service.NovelAiService;
import com.novelcopilot.backend.service.StoryAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
public class NovelAiController {

    private final NovelAiService novelAiService;
    private final StoryAccessService storyAccessService;
    private final ChapterRepository chapterRepository;

    public NovelAiController(
            NovelAiService novelAiService,
            StoryAccessService storyAccessService,
            ChapterRepository chapterRepository
    ) {
        this.novelAiService = novelAiService;
        this.storyAccessService = storyAccessService;
        this.chapterRepository = chapterRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<NovelAiResponse> generate(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestBody NovelAiRequest request) {

        requireRequestAccess(request);

        NovelAiResponse response = novelAiService.generate(user.getId(), request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/estimate")
    public ResponseEntity<java.util.Map<String, Object>> estimate(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestBody NovelAiRequest request) {

        requireRequestAccess(request);

        java.util.Map<String, Object> response = novelAiService.estimate(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    private void requireRequestAccess(NovelAiRequest request) {
        if (request.storyId() != null) {
            storyAccessService.requireStoryAccess(request.storyId());
        }
        if (request.chapterId() != null) {
            storyAccessService.requireChapterAccess(request.chapterId());
        }
        if (request.storyId() != null
                && request.chapterId() != null
                && chapterRepository.findByIdAndStoryId(request.chapterId(), request.storyId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Capítulo não encontrado nesta história");
        }
    }
}
