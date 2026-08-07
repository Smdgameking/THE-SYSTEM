package com.thesystem.modules.settings.controller;

import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.settings.dto.NamespaceSettingsResponse;
import com.thesystem.modules.settings.dto.SettingDefinitionResponse;
import com.thesystem.modules.settings.dto.SettingResponse;
import com.thesystem.modules.settings.dto.SetSettingRequest;
import com.thesystem.modules.settings.registry.SettingDefinition;
import com.thesystem.modules.settings.service.SettingsService;
import com.thesystem.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
@Tag(name = "Settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/{namespace}/{key}")
    @Operation(summary = "Get user setting")
    public ResponseEntity<ApiResponse<SettingResponse>> getSetting(
            @PathVariable @Parameter(description = "Namespace") String namespace,
            @PathVariable @Parameter(description = "Setting key") String key) {
        UUID userId = SecurityUtils.getCurrentUserId();
        SettingResponse response = settingsService.getSetting(userId, namespace, key);
        return ResponseEntity.ok(ApiResponse.ok(response, "Setting retrieved successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/{namespace}/{key}")
    @Operation(summary = "Set user setting")
    public ResponseEntity<ApiResponse<SettingResponse>> setSetting(
            @PathVariable @Parameter(description = "Namespace") String namespace,
            @PathVariable @Parameter(description = "Setting key") String key,
            @Valid @RequestBody SetSettingRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        SettingResponse response = settingsService.setSetting(userId, namespace, key, request.value());
        return ResponseEntity.ok(ApiResponse.ok(response, "Setting updated successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{namespace}/{key}")
    @Operation(summary = "Delete user setting")
    public ResponseEntity<ApiResponse<Void>> deleteSetting(
            @PathVariable @Parameter(description = "Namespace") String namespace,
            @PathVariable @Parameter(description = "Setting key") String key) {
        UUID userId = SecurityUtils.getCurrentUserId();
        settingsService.deleteSetting(userId, namespace, key);
        return ResponseEntity.ok(ApiResponse.ok(null, "Setting deleted successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{namespace}")
    @Operation(summary = "Get namespace settings")
    public ResponseEntity<ApiResponse<NamespaceSettingsResponse>> getNamespaceSettings(
            @PathVariable @Parameter(description = "Namespace") String namespace) {
        UUID userId = SecurityUtils.getCurrentUserId();
        NamespaceSettingsResponse response = settingsService.getNamespaceSettings(userId, namespace);
        return ResponseEntity.ok(ApiResponse.ok(response, "Namespace settings retrieved successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/{namespace}")
    @Operation(summary = "Set namespace settings")
    public ResponseEntity<ApiResponse<NamespaceSettingsResponse>> setNamespaceSettings(
            @PathVariable @Parameter(description = "Namespace") String namespace,
            @Valid @RequestBody Map<String, Object> values) {
        UUID userId = SecurityUtils.getCurrentUserId();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            settingsService.setSetting(userId, namespace, entry.getKey(), entry.getValue());
        }
        NamespaceSettingsResponse response = settingsService.getNamespaceSettings(userId, namespace);
        return ResponseEntity.ok(ApiResponse.ok(response, "Namespace settings updated successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{namespace}/reset")
    @Operation(summary = "Reset namespace to defaults")
    public ResponseEntity<ApiResponse<Void>> resetNamespace(
            @PathVariable @Parameter(description = "Namespace") String namespace) {
        UUID userId = SecurityUtils.getCurrentUserId();
        NamespaceSettingsResponse current = settingsService.getNamespaceSettings(userId, namespace);
        for (String key : current.settings().keySet()) {
            settingsService.deleteSetting(userId, namespace, key);
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Namespace reset to defaults", UUID.randomUUID().toString()));
    }

    @GetMapping("/system/{namespace}/{key}")
    @Operation(summary = "Get system setting (admin)")
    public ResponseEntity<ApiResponse<SettingResponse>> getSystemSetting(
            @PathVariable @Parameter(description = "Namespace") String namespace,
            @PathVariable @Parameter(description = "Setting key") String key) {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Admin access required");
        }
        SettingResponse response = settingsService.getSystemSetting(namespace, key);
        return ResponseEntity.ok(ApiResponse.ok(response, "System setting retrieved successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/system/{namespace}/{key}")
    @Operation(summary = "Set system setting (admin)")
    public ResponseEntity<ApiResponse<SettingResponse>> setSystemSetting(
            @PathVariable @Parameter(description = "Namespace") String namespace,
            @PathVariable @Parameter(description = "Setting key") String key,
            @Valid @RequestBody SetSettingRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Admin access required");
        }
        SettingResponse response = settingsService.setSystemSetting(namespace, key, request.value());
        return ResponseEntity.ok(ApiResponse.ok(response, "System setting updated successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/definitions/{namespace}/{key}")
    @Operation(summary = "Get setting definition")
    public ResponseEntity<ApiResponse<SettingDefinitionResponse>> getDefinition(
            @PathVariable @Parameter(description = "Namespace") String namespace,
            @PathVariable @Parameter(description = "Setting key") String key) {
        SettingDefinitionResponse response = settingsService.getDefinition(namespace, key);
        return ResponseEntity.ok(ApiResponse.ok(response, "Definition retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/definitions/namespace/{namespace}")
    @Operation(summary = "Get definitions by namespace")
    public ResponseEntity<ApiResponse<List<SettingDefinitionResponse>>> getDefinitionsByNamespace(
            @PathVariable @Parameter(description = "Namespace") String namespace) {
        List<SettingDefinitionResponse> response = settingsService.getDefinitionsByNamespace(namespace);
        return ResponseEntity.ok(ApiResponse.ok(response, "Definitions retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/definitions/engine/{engine}")
    @Operation(summary = "Get definitions by owning engine")
    public ResponseEntity<ApiResponse<List<SettingDefinitionResponse>>> getDefinitionsByEngine(
            @PathVariable @Parameter(description = "Engine name") String engine) {
        List<SettingDefinitionResponse> response = settingsService.getDefinitionsByOwningEngine(engine);
        return ResponseEntity.ok(ApiResponse.ok(response, "Definitions retrieved successfully", UUID.randomUUID().toString()));
    }
}
