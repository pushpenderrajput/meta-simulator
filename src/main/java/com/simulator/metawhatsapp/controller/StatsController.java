package com.simulator.metawhatsapp.controller;

import com.simulator.metawhatsapp.dto.response.StatsResponse;
import com.simulator.metawhatsapp.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }

    @GetMapping("/refresh")
    public ResponseEntity<StatsResponse> refreshStats() {
        return ResponseEntity.ok(statsService.resetStats());
    }
}