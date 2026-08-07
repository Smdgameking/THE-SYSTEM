package com.thesystem.modules.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.settings.dto.NamespaceSettingsResponse;
import com.thesystem.modules.settings.dto.SettingDefinitionResponse;
import com.thesystem.modules.settings.dto.SettingResponse;
import com.thesystem.modules.settings.dto.SetSettingRequest;
import com.thesystem.modules.settings.enums.SettingType;
import com.thesystem.modules.settings.enums.Visibility;
import com.thesystem.modules.settings.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        static SettingsService settingsService = Mockito.mock(SettingsService.class);

        @Bean
        SettingsService settingsService() {
            return settingsService;
        }
    }

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null)
        );
    }

    @Test
    void shouldGetSetting() throws Exception {
        UUID userId = UUID.randomUUID();
        SettingResponse response = new SettingResponse(
                "notification", "email_enabled", true, SettingType.BOOLEAN,
                "Enable email notifications", false, java.time.Instant.now()
        );

        Mockito.when(TestConfig.settingsService.getSetting(any(UUID.class), any(String.class), any(String.class)))
                .thenReturn(response);

        mockMvc.perform(get("/settings/notification/email_enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.namespace").value("notification"))
                .andExpect(jsonPath("$.data.key").value("email_enabled"));
    }

    @Test
    void shouldSetSetting() throws Exception {
        UUID userId = UUID.randomUUID();
        SettingResponse response = new SettingResponse(
                "notification", "email_enabled", true, SettingType.BOOLEAN,
                "Enable email notifications", false, java.time.Instant.now()
        );

        Mockito.when(TestConfig.settingsService.setSetting(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(response);

        mockMvc.perform(put("/settings/notification/email_enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SetSettingRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.value").value(true));
    }

    @Test
    void shouldDeleteSetting() throws Exception {
        mockMvc.perform(delete("/settings/notification/email_enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Setting deleted successfully"));
    }

    @Test
    void shouldGetNamespaceSettings() throws Exception {
        UUID userId = UUID.randomUUID();
        Map<String, SettingResponse> settingsMap = Map.of(
                "email_enabled", new SettingResponse("notification", "email_enabled", true, SettingType.BOOLEAN, "Enable email notifications", false, java.time.Instant.now()),
                "push_enabled", new SettingResponse("notification", "push_enabled", false, SettingType.BOOLEAN, "Enable push notifications", false, java.time.Instant.now())
        );
        NamespaceSettingsResponse response = new NamespaceSettingsResponse("notification", settingsMap);

        Mockito.when(TestConfig.settingsService.getNamespaceSettings(any(UUID.class), any(String.class)))
                .thenReturn(response);

        mockMvc.perform(get("/settings/notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.namespace").value("notification"));
    }

    @Test
    void shouldResetNamespace() throws Exception {
        Mockito.when(TestConfig.settingsService.getNamespaceSettings(any(UUID.class), any(String.class)))
                .thenReturn(new NamespaceSettingsResponse("notification", Map.of()));

        mockMvc.perform(post("/settings/notification/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Namespace reset to defaults"));
    }

    @Test
    void shouldGetDefinition() throws Exception {
        SettingDefinitionResponse response = new SettingDefinitionResponse(
                "notification", "email_enabled", SettingType.BOOLEAN, true,
                "Enable email notifications", Visibility.PUBLIC, "notification"
        );

        Mockito.when(TestConfig.settingsService.getDefinition("notification", "email_enabled"))
                .thenReturn(response);

        mockMvc.perform(get("/settings/definitions/notification/email_enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.key").value("email_enabled"));
    }

    @Test
    void shouldGetDefinitionsByNamespace() throws Exception {
        List<SettingDefinitionResponse> responses = List.of(
                new SettingDefinitionResponse("notification", "email_enabled", SettingType.BOOLEAN, true, "Enable email notifications", Visibility.PUBLIC, "notification"),
                new SettingDefinitionResponse("notification", "push_enabled", SettingType.BOOLEAN, false, "Enable push notifications", Visibility.PUBLIC, "notification")
        );

        Mockito.when(TestConfig.settingsService.getDefinitionsByNamespace("notification"))
                .thenReturn(responses);

        mockMvc.perform(get("/settings/definitions/namespace/notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].key").value("email_enabled"));
    }

    @Test
    void shouldGetDefinitionsByEngine() throws Exception {
        List<SettingDefinitionResponse> responses = List.of(
                new SettingDefinitionResponse("notification", "email_enabled", SettingType.BOOLEAN, true, "Enable email notifications", Visibility.PUBLIC, "notification")
        );

        Mockito.when(TestConfig.settingsService.getDefinitionsByOwningEngine("notification"))
                .thenReturn(responses);

        mockMvc.perform(get("/settings/definitions/engine/notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].owningEngine").value("notification"));
    }
}
