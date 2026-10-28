package com.novelcopilot.backend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AiGenerationHistoryPageResponse(
        List<Item> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static AiGenerationHistoryPageResponse from(Page<AiGenerationHistoryItem> page) {
        return new AiGenerationHistoryPageResponse(
                page.getContent().stream().map(Item::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public record Item(
            Long id,
            Long usageId,
            String action,
            String outputType,
            String content,
            Map<String, Object> structuredResult,
            String provider,
            String model,
            LocalDateTime createdAt
    ) {
        private static Item from(AiGenerationHistoryItem item) {
            Map<String, Object> structuredResult = item.structuredResult() == null
                    ? null
                    : OBJECT_MAPPER.convertValue(item.structuredResult(), Map.class);
            return new Item(
                    item.id(), item.usageId(), item.action(), item.outputType(), item.content(), structuredResult,
                    item.provider(), item.model(), item.createdAt()
            );
        }
    }
}
