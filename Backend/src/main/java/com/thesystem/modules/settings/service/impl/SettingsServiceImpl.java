package com.thesystem.modules.settings.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.settings.dto.NamespaceSettingsResponse;
import com.thesystem.modules.settings.dto.SettingDefinitionResponse;
import com.thesystem.modules.settings.dto.SettingResponse;
import com.thesystem.modules.settings.entity.Setting;
import com.thesystem.modules.settings.enums.SettingType;
import com.thesystem.modules.settings.enums.Visibility;
import com.thesystem.modules.settings.mapper.SettingMapper;
import com.thesystem.modules.settings.registry.InMemorySettingRegistry;
import com.thesystem.modules.settings.registry.SettingDefinition;
import com.thesystem.modules.settings.registry.SettingRegistry;
import com.thesystem.modules.settings.repository.SettingRepository;
import com.thesystem.modules.settings.service.SettingsService;
import com.thesystem.security.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SettingsServiceImpl implements SettingsService {

    private final SettingRepository settingRepository;
    private final SettingMapper settingMapper;
    private final SettingRegistry settingRegistry;
    private final ObjectMapper objectMapper;

    public SettingsServiceImpl(
            SettingRepository settingRepository,
            SettingMapper settingMapper,
            SettingRegistry settingRegistry,
            ObjectMapper objectMapper
    ) {
        this.settingRepository = settingRepository;
        this.settingMapper = settingMapper;
        this.settingRegistry = settingRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public SettingResponse getSetting(UUID userId, String namespace, String key) {
        validateAccess(userId, namespace, key);
        Setting setting = findSetting(userId, namespace, key);
        return settingMapper.toSettingResponse(setting);
    }

    @Override
    @Transactional
    public SettingResponse setSetting(UUID userId, String namespace, String key, Object value) {
        validateAccess(userId, namespace, key);
        SettingDefinition definition = settingRegistry.getDefinition(namespace, key);
        settingRegistry.validate(namespace, key, value);

        Setting setting = settingRepository.findByUserIdAndNamespaceAndKeyAndDeletedAtIsNull(userId, namespace, key)
                .orElseGet(() -> {
                    Setting s = new Setting();
                    s.setId(UUID.randomUUID());
                    s.setUserId(userId);
                    s.setNamespace(namespace);
                    s.setKey(key);
                    s.setValueType(definition.type());
                    return s;
                });

        setSettingValue(setting, value, definition.type());
        Setting saved = settingRepository.save(setting);
        return settingMapper.toSettingResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSetting(UUID userId, String namespace, String key) {
        validateAccess(userId, namespace, key);
        Setting setting = findSetting(userId, namespace, key);
        settingRepository.delete(setting);
    }

    @Override
    @Transactional(readOnly = true)
    public NamespaceSettingsResponse getNamespaceSettings(UUID userId, String namespace) {
        List<Setting> settings = settingRepository.findByUserIdAndNamespaceAndDeletedAtIsNull(userId, namespace);
        Map<String, SettingResponse> map = settings.stream()
                .map(settingMapper::toSettingResponse)
                .collect(Collectors.toMap(SettingResponse::key, s -> s));
        return new NamespaceSettingsResponse(namespace, map);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, SettingResponse>> getAllUserSettings(UUID userId) {
        List<Setting> settings = settingRepository.findByUserIdAndDeletedAtIsNull(userId);
        return settings.stream()
                .map(settingMapper::toSettingResponse)
                .collect(Collectors.groupingBy(
                        SettingResponse::namespace,
                        Collectors.toMap(SettingResponse::key, s -> s)
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public SettingResponse getSystemSetting(String namespace, String key) {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Admin access required");
        }
        Setting setting = settingRepository.findByUserIdIsNullAndNamespaceAndKeyAndDeletedAtIsNull(namespace, key)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "System setting not found"));
        return settingMapper.toSettingResponse(setting);
    }

    @Override
    @Transactional
    public SettingResponse setSystemSetting(String namespace, String key, Object value) {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Admin access required");
        }
        SettingDefinition definition = settingRegistry.getDefinition(namespace, key);
        settingRegistry.validate(namespace, key, value);

        Setting setting = settingRepository.findByUserIdIsNullAndNamespaceAndKeyAndDeletedAtIsNull(namespace, key)
                .orElseGet(() -> {
                    Setting s = new Setting();
                    s.setId(UUID.randomUUID());
                    s.setUserId(null);
                    s.setNamespace(namespace);
                    s.setKey(key);
                    s.setIsSystem(true);
                    s.setValueType(definition.type());
                    return s;
                });

        setSettingValue(setting, value, definition.type());
        Setting saved = settingRepository.save(setting);
        return settingMapper.toSettingResponse(saved);
    }

    @Override
    public void registerDefinition(SettingDefinition definition) {
        settingRegistry.register(definition);
    }

    @Override
    public SettingDefinitionResponse getDefinition(String namespace, String key) {
        SettingDefinition definition = settingRegistry.getDefinition(namespace, key);
        return toDefinitionResponse(definition);
    }

    @Override
    public List<SettingDefinitionResponse> getDefinitionsByNamespace(String namespace) {
        return settingRegistry.getDefinitionsByNamespace(namespace).stream()
                .map(this::toDefinitionResponse)
                .toList();
    }

    @Override
    public List<SettingDefinitionResponse> getDefinitionsByOwningEngine(String engine) {
        return settingRegistry.getDefinitionsByOwningEngine(engine).stream()
                .map(this::toDefinitionResponse)
                .toList();
    }

    private Setting findSetting(UUID userId, String namespace, String key) {
        return settingRepository.findByUserIdAndNamespaceAndKeyAndDeletedAtIsNull(userId, namespace, key)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Setting not found"));
    }

    private void validateAccess(UUID userId, String namespace, String key) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Access denied");
        }
    }

    private void setSettingValue(Setting setting, Object value, SettingType type) {
        switch (type) {
            case STRING -> setting.setValue(Objects.toString(value, null));
            case BOOLEAN -> setting.setValue(Objects.toString(value, null));
            case INTEGER -> setting.setValue(Objects.toString(value, null));
            case DOUBLE -> setting.setValue(Objects.toString(value, null));
            case JSON -> {
                try {
                    setting.setValueJson(objectMapper.writeValueAsString(value));
                } catch (JsonProcessingException e) {
                    throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Invalid JSON value");
                }
            }
            case ENUM -> setting.setValue(Objects.toString(value, null));
            default -> throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Unsupported setting type: " + type);
        }
    }

    private SettingDefinitionResponse toDefinitionResponse(SettingDefinition definition) {
        return new SettingDefinitionResponse(
                definition.namespace(),
                definition.key(),
                definition.type(),
                definition.defaultValue(),
                definition.description(),
                definition.visibility(),
                definition.owningEngine()
        );
    }
}
