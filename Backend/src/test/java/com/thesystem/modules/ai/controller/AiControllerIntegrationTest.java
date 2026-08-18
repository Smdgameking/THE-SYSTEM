package com.thesystem.modules.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.ai.dto.AiInteractionResponse;
import com.thesystem.modules.ai.dto.CreateAiInteractionRequest;
import com.thesystem.modules.ai.provider.AiContextItem;
import com.thesystem.modules.ai.service.AiService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        static AiService aiService = Mockito.mock(AiService.class);

        @Bean
        AiService aiService() {
            return aiService;
        }
    }

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null)
        );
    }

    private AiInteractionResponse response(UUID id) {
        return new AiInteractionResponse(
                id, UUID.randomUUID(), "Tell me about my day", "Here is a summary",
                "mock-provider", "mock-model", List.of(new AiContextItem("Title", "FACT", "HIGH", "MANUAL", "Content")),
                10, 5, 15, "stop", Instant.now(), Instant.now(), null, null, null
        );
    }

    @Test
    void shouldCreateInteraction() throws Exception {
        UUID interactionId = UUID.randomUUID();
        CreateAiInteractionRequest request = new CreateAiInteractionRequest("Tell me about my day", null);
        Mockito.when(TestConfig.aiService.createInteraction(any(UUID.class), any(CreateAiInteractionRequest.class)))
                .thenReturn(response(interactionId));

        mockMvc.perform(post("/api/v1/ai/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Tell me about my day"))
                .andExpect(jsonPath("$.data.provider").value("mock-provider"));
    }

    @Test
    void shouldListInteractions() throws Exception {
        Mockito.when(TestConfig.aiService.listInteractions(any(UUID.class)))
                .thenReturn(List.of(response(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/ai/interactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].provider").value("mock-provider"));
    }

    @Test
    void shouldGetInteraction() throws Exception {
        UUID interactionId = UUID.randomUUID();
        Mockito.when(TestConfig.aiService.getInteraction(any(UUID.class), eq(interactionId)))
                .thenReturn(response(interactionId));

        mockMvc.perform(get("/api/v1/ai/interactions/" + interactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(interactionId.toString()));
    }

    @Test
    void shouldDeleteInteraction() throws Exception {
        UUID interactionId = UUID.randomUUID();
        Mockito.doNothing().when(TestConfig.aiService).deleteInteraction(any(UUID.class), eq(interactionId));

        mockMvc.perform(delete("/api/v1/ai/interactions/" + interactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldRejectCreateWithBlankMessage() throws Exception {
        String body = """
                {"message": ""}
                """;

        mockMvc.perform(post("/api/v1/ai/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectCreateWithOversizedMessage() throws Exception {
        String oversized = "x".repeat(4001);
        String body = """
                {"message": "%s"}
                """.formatted(oversized);

        mockMvc.perform(post("/api/v1/ai/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnNotFoundWhenInteractionMissing() throws Exception {
        UUID interactionId = UUID.randomUUID();
        Mockito.doThrow(new BusinessException(ErrorCodes.NOT_FOUND, "AI interaction not found"))
                .when(TestConfig.aiService).getInteraction(any(UUID.class), eq(interactionId));

        mockMvc.perform(get("/api/v1/ai/interactions/" + interactionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenNoProviderConfigured() throws Exception {
        Mockito.when(TestConfig.aiService.createInteraction(any(UUID.class), any(CreateAiInteractionRequest.class)))
                .thenThrow(new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE, "No AI provider is configured"));
        CreateAiInteractionRequest request = new CreateAiInteractionRequest("Message", null);

        mockMvc.perform(post("/api/v1/ai/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"));
    }
}
