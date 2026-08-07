package com.thesystem.modules.settings.registry;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.settings.enums.SettingType;
import com.thesystem.modules.settings.enums.Visibility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class InMemorySettingRegistry implements SettingRegistry {

    private final Map<String, SettingDefinition> definitions = new ConcurrentHashMap<>();
    private volatile boolean locked = false;

    @Override
    public synchronized void register(SettingDefinition definition) {
        if (locked) {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Setting registry is locked. Definitions cannot be modified after startup.");
        }
        validateDefinition(definition);
        String compositeKey = compositeKey(definition.namespace(), definition.key());
        if (definitions.containsKey(compositeKey)) {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Setting already registered: " + compositeKey);
        }
        definitions.put(compositeKey, definition);
    }

    @Override
    public synchronized void registerAll(List<SettingDefinition> definitionsToRegister) {
        for (SettingDefinition definition : definitionsToRegister) {
            register(definition);
        }
    }

    @Override
    public SettingDefinition getDefinition(String namespace, String key) {
        String compositeKey = compositeKey(namespace, key);
        SettingDefinition definition = definitions.get(compositeKey);
        if (definition == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND,
                    "Setting not registered: " + namespace + "." + key);
        }
        return definition;
    }

    @Override
    public List<SettingDefinition> getDefinitionsByNamespace(String namespace) {
        return definitions.values().stream()
                .filter(d -> d.namespace().equals(namespace))
                .toList();
    }

    @Override
    public List<SettingDefinition> getDefinitionsByOwningEngine(String engine) {
        return definitions.values().stream()
                .filter(d -> d.owningEngine().equals(engine))
                .toList();
    }

    @Override
    public boolean isRegistered(String namespace, String key) {
        return definitions.containsKey(compositeKey(namespace, key));
    }

    @Override
    public void validate(String namespace, String key, Object value) {
        SettingDefinition definition = getDefinition(namespace, key);
        if (definition.validator() != null && !definition.validator().test(value)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Invalid value for setting " + namespace + "." + key + ": " + value);
        }
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public synchronized void lock() {
        this.locked = true;
    }

    private void validateDefinition(SettingDefinition definition) {
        if (definition.namespace() == null || definition.namespace().isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Setting namespace cannot be blank");
        }
        if (definition.key() == null || definition.key().isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Setting key cannot be blank");
        }
        if (definition.type() == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Setting type cannot be null");
        }
        if (definition.owningEngine() == null || definition.owningEngine().isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Setting owningEngine cannot be blank");
        }
    }

    private String compositeKey(String namespace, String key) {
        return namespace + ":" + key;
    }
}
