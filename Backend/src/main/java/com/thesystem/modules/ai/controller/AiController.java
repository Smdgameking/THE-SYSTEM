package com.thesystem.modules.ai.controller;

import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.ai.dto.AiInteractionResponse;
import com.thesystem.modules.ai.dto.CreateAiInteractionRequest;
import com.thesystem.modules.ai.service.AiService;
import com.thesystem.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/interactions")
    @Operation(summary = "Create AI interaction")
    public ResponseEntity<ApiResponse<AiInteractionResponse>> createInteraction(
            @Valid @RequestBody CreateAiInteractionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AiInteractionResponse response = aiService.createInteraction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "AI interaction created successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/interactions")
    @Operation(summary = "List AI interactions")
    public ResponseEntity<ApiResponse<List<AiInteractionResponse>>> listInteractions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<AiInteractionResponse> response = aiService.listInteractions(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "AI interactions retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/interactions/{id}")
    @Operation(summary = "Get AI interaction by ID")
    public ResponseEntity<ApiResponse<AiInteractionResponse>> getInteraction(
            @PathVariable @Parameter(description = "AI interaction ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AiInteractionResponse response = aiService.getInteraction(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "AI interaction retrieved successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/interactions/{id}")
    @Operation(summary = "Delete AI interaction")
    public ResponseEntity<ApiResponse<Void>> deleteInteraction(
            @PathVariable @Parameter(description = "AI interaction ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        aiService.deleteInteraction(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "AI interaction deleted successfully", UUID.randomUUID().toString()));
    }
}
