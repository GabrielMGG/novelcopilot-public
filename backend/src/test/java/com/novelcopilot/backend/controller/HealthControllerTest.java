package com.novelcopilot.backend.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void returnsStableStructuredLivenessResponse() {
        HealthController controller = new HealthController();

        assertEquals(
                Map.of("status", "UP", "service", "novelcopilot-backend"),
                controller.health()
        );
    }
}
