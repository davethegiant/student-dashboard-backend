package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.response.MetaResponse;
import com.example.studentdashboard.service.MetaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
@Tag(name = "Meta", description = "Bootstrap data for frontend dropdowns/filters: sessions, terms, classes, subjects")
public class MetaController {

    private final MetaService metaService;

    @GetMapping
    @Operation(summary = "Sessions, terms, classes, subjects, and which session/term is current")
    public ResponseEntity<MetaResponse> getMeta() {
        return ResponseEntity.ok(metaService.getMeta());
    }
}
