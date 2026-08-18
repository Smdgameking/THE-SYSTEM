package com.thesystem.modules.settings.service;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
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
import com.thesystem.modules.settings.service.impl.SettingsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceUnitTest {

    @Mock
    private SettingRepository settingRepository;

    @Mock
    private SettingMapper settingMapper;

    @Mock
    private ObjectMapper objectMapper;

    private SettingsServiceImpl settingsService;
    private SettingRegistry settingRegistry;
    private UUID userId;

    @BeforeEach
    void setUp() {
        settingRegistry = new InMemorySettingRegistry();
        settingsService = new SettingsServiceImpl(settingRepository, settingMapper, settingRegistry, objectMapper);
        userId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRegisterDefinitionSuccessfully() {
        SettingDefinition definition = new SettingDefinition(
                "notification", "email_enabled", SettingType.BOOLEAN, true,
                "Enable email notifications", null, Visibility.PUBLIC, "notification"
        );
        settingsService.registerDefinition(definition);
        assertThat(settingRegistry.isRegistered("notification", "email_enabled")).isTrue();
    }

    @Test
    void shouldThrowConflictWhenRegisteringDuplicateDefinition() {
        SettingDefinition definition1 = new SettingDefinition(
                "notification", "email_enabled", SettingType.BOOLEAN, true,
                "Enable email notifications", null, Visibility.PUBLIC, "notification"
        );
        SettingDefinition definition2 = new SettingDefinition(
                "notification", "email_enabled", SettingType.BOOLEAN, false,
                "Duplicate setting", null, Visibility.PUBLIC, "notification"
        );
        settingsService.registerDefinition(definition1);
        assertThrows(BusinessException.class, () -> settingsService.registerDefinition(definition2));
    }

    @Test
    void shouldGetDefinition() {
        SettingDefinition definition = new SettingDefinition(
                "notification", "email_enabled", SettingType.BOOLEAN, true,
                "Enable email notifications", null, Visibility.PUBLIC, "notification"
        );
        settingsService.registerDefinition(definition);
        SettingDefinitionResponse response = settingsService.getDefinition("notification", "email_enabled");
        assertThat(response).isNotNull();
        assertThat(response.namespace()).isEqualTo("notification");
        assertThat(response.key()).isEqualTo("email_enabled");
    }

    @Test
    void shouldThrowNotFoundForUnregisteredDefinition() {
        assertThrows(BusinessException.class, () -> settingsService.getDefinition("notification", "unknown"));
    }

    @Test
    void shouldGetDefinitionsByNamespace() {
        SettingDefinition definition1 = new SettingDefinition(
                "notification", "email_enabled", SettingType.BOOLEAN, true,
                "Enable email notifications", null, Visibility.PUBLIC, "notification"
        );
        SettingDefinition definition2 = new SettingDefinition(
                "notification", "push_enabled", SettingType.BOOLEAN, false,
                "Enable push notifications", null, Visibility.PUBLIC, "notification"
        );
        settingsService.registerDefinition(definition1);
        settingsService.registerDefinition(definition2);
        List<SettingDefinitionResponse> responses = settingsService.getDefinitionsByNamespace("notification");
        assertThat(responses).hasSize(2);
    }

    @Test
    void shouldGetDefinitionsByOwningEngine() {
        SettingDefinition definition = new SettingDefinition(
                "notification", "email_enabled", SettingType.BOOLEAN, true,
                "Enable email notifications", null, Visibility.PUBLIC, "notification"
        );
        settingsService.registerDefinition(definition);
        List<SettingDefinitionResponse> responses = settingsService.getDefinitionsByOwningEngine("notification");
        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldAssignUuidWhenCreatingNewSetting() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null)
        );

        SettingDefinition definition = new SettingDefinition(
                "appearance", "theme", SettingType.STRING, "light",
                "UI theme", null, Visibility.PUBLIC, "settings"
        );
        settingsService.registerDefinition(definition);

        when(settingRepository.findByUserIdAndNamespaceAndKeyAndDeletedAtIsNull(userId, "appearance", "theme"))
                .thenReturn(Optional.empty());
        when(settingRepository.save(any(Setting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settingMapper.toSettingResponse(any(Setting.class)))
                .thenReturn(new SettingResponse("appearance", "theme", "dark", SettingType.STRING,
                        "UI theme", false, java.time.Instant.now()));

        settingsService.setSetting(userId, "appearance", "theme", "dark");

        ArgumentCaptor<Setting> captor = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNotNull();
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getValueType()).isEqualTo(SettingType.STRING);
    }
}
