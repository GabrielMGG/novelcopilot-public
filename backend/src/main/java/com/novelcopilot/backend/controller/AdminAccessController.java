package com.novelcopilot.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAccessController {

    @GetMapping("/access")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> checkAccess() {
        return Map.of("admin", true);
    }
}
