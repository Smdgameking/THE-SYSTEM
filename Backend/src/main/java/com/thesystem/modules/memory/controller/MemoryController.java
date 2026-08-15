package com.thesystem.modules.memory.controller;

import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.memory.dto.CreateMemoryRequest;
import com.thesystem.modules.memory.dto.MemoryFilterRequest;
import com.thesystem.modules.memory.dto.MemoryResponse;
import com.thesystem.modules.memory.dto.UpdateMemoryRequest;
import com.thesystem.modules.memory.service.MemoryService;
import com.thesystem.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories")
@Tag(name = "Memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping
    @Operation(summary = "Create memory")
    public ResponseEntity<ApiResponse<MemoryResponse>> createMemory(@Valid @RequestBody CreateMemoryRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MemoryResponse response = memoryService.createMemory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Memory created successfully", UUID.randomUUID().toString()));
    }

    @GetMapping
    @Operation(summary = "List memories")
    public ResponseEntity<ApiResponse<List<MemoryResponse>>> listMemories(@Valid MemoryFilterRequest filter) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<MemoryResponse> response = memoryService.listMemories(userId, filter);
        return ResponseEntity.ok(ApiResponse.ok(response, "Memories retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get memory by ID")
    public ResponseEntity<ApiResponse<MemoryResponse>> getMemory(@PathVariable @Parameter(description = "Memory ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MemoryResponse response = memoryService.getMemory(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Memory retrieved successfully", UUID.randomUUID().toString()));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update memory")
    public ResponseEntity<ApiResponse<MemoryResponse>> updateMemory(
            @PathVariable @Parameter(description = "Memory ID") UUID id,
            @Valid @RequestBody UpdateMemoryRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MemoryResponse response = memoryService.updateMemory(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Memory updated successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete memory")
    public ResponseEntity<ApiResponse<Void>> deleteMemory(@PathVariable @Parameter(description = "Memory ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        memoryService.deleteMemory(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Memory deleted successfully", UUID.randomUUID().toString()));
    }
}
