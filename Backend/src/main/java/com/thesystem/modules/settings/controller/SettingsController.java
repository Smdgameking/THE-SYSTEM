package com.thesystem.modules.settings.controller;

import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.settings.dto.NamespaceSettingsResponse;
import com.thesystem.modules.settings.dto.SettingDefinitionResponse;
import com.thesystem.modules.settings.dto.SettingResponse;
import com.thesystem.modules.settings.dto.SetSettingRequest;
import com.thesystem.modules.settings.registry.SettingDefinition;
import com.thesystem.modules.settings.service.SettingsService;
import com.thesystem.security.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/{namespace}/{key}")
    public ResponseEntity<ApiResponse<SettingResponse>> getSetting(
            @PathVariable String namespace,
            @PathVariable String key) {
        UUID userId = SecurityUtils.getCurrentUserId();
        SettingResponse response = settingsService.getSetting(userId, namespace, key);
        return ResponseEntity.ok(ApiResponse.ok(response, "Setting retrieved successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/{namespace}/{key}")
    public ResponseEntity<ApiResponse<SettingResponse>> setSetting(
            @PathVariable String namespace,
            @PathVariable String key,
            @Valid @RequestBody SetSettingRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        SettingResponse response = settingsService.setSetting(userId, namespace, key, request.value());
        return ResponseEntity.ok(ApiResponse.ok(response, "Setting updated successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{namespace}/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteSetting(
            @PathVariable String namespace,
            @PathVariable String key) {
        UUID userId = SecurityUtils.getCurrentUserId();
        settingsService.deleteSetting(userId, namespace, key);
        return ResponseEntity.ok(ApiResponse.ok(null, "Setting deleted successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{namespace}")
    public ResponseEntity<ApiResponse<NamespaceSettingsResponse>> getNamespaceSettings(
            @PathVariable String namespace) {
        UUID userId = SecurityUtils.getCurrentUserId();
        NamespaceSettingsResponse response = settingsService.getNamespaceSettings(userId, namespace);
        return ResponseEntity.ok(ApiResponse.ok(response, "Namespace settings retrieved successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/{namespace}")
    public ResponseEntity<ApiResponse<NamespaceSettingsResponse>> setNamespaceSettings(
            @PathVariable String namespace,
            @Valid @RequestBody Map<String, Object> values) {
        UUID userId = SecurityUtils.getCurrentUserId();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            settingsService.setSetting(userId, namespace, entry.getKey(), entry.getValue());
        }
        NamespaceSettingsResponse response = settingsService.getNamespaceSettings(userId, namespace);
        return ResponseEntity.ok(ApiResponse.ok(response, "Namespace settings updated successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{namespace}/reset")
    public ResponseEntity<ApiResponse<Void>> resetNamespace(
            @PathVariable String namespace) {
        UUID userId = SecurityUtils.getCurrentUserId();
        NamespaceSettingsResponse current = settingsService.getNamespaceSettings(userId, namespace);
        for (String key : current.settings().keySet()) {
            settingsService.deleteSetting(userId, namespace, key);
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Namespace reset to defaults", UUID.randomUUID().toString()));
    }

    @GetMapping("/system/{namespace}/{key}")
    public ResponseEntity<ApiResponse<SettingResponse>> getSystemSetting(
            @PathVariable String namespace,
            @PathVariable String key) {
        SettingResponse response = settingsService.getSystemSetting(namespace, key);
        return ResponseEntity.ok(ApiResponse.ok(response, "System setting retrieved successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/system/{namespace}/{key}")
    public ResponseEntity<ApiResponse<SettingResponse>> setSystemSetting(
            @PathVariable String namespace,
            @PathVariable String key,
            @Valid @RequestBody SetSettingRequest request) {
        SettingResponse response = settingsService.setSystemSetting(namespace, key, request.value());
        return ResponseEntity.ok(ApiResponse.ok(response, "System setting updated successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/definitions/{namespace}/{key}")
    public ResponseEntity<ApiResponse<SettingDefinitionResponse>> getDefinition(
            @PathVariable String namespace,
            @PathVariable String key) {
        SettingDefinitionResponse response = settingsService.getDefinition(namespace, key);
        return ResponseEntity.ok(ApiResponse.ok(response, "Definition retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/definitions/namespace/{namespace}")
    public ResponseEntity<ApiResponse<List<SettingDefinitionResponse>>> getDefinitionsByNamespace(
            @PathVariable String namespace) {
        List<SettingDefinitionResponse> response = settingsService.getDefinitionsByNamespace(namespace);
        return ResponseEntity.ok(ApiResponse.ok(response, "Definitions retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/definitions/engine/{engine}")
    public ResponseEntity<ApiResponse<List<SettingDefinitionResponse>>> getDefinitionsByEngine(
            @PathVariable String engine) {
        List<SettingDefinitionResponse> response = settingsService.getDefinitionsByOwningEngine(engine);
        return ResponseEntity.ok(ApiResponse.ok(response, "Definitions retrieved successfully", UUID.randomUUID().toString()));
    }
}
