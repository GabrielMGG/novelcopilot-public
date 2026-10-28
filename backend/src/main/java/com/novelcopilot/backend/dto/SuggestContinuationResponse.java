package com.novelcopilot.backend.dto;

import java.util.List;

public record SuggestContinuationResponse(
        List<SuggestContinuationOption> options
) {
}
