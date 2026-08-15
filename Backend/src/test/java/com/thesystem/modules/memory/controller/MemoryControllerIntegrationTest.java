package com.thesystem.modules.memory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.memory.dto.CreateMemoryRequest;
import com.thesystem.modules.memory.dto.MemoryResponse;
import com.thesystem.modules.memory.dto.UpdateMemoryRequest;
import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;
import com.thesystem.modules.memory.service.MemoryService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MemoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        static MemoryService memoryService = Mockito.mock(MemoryService.class);

        @Bean
        MemoryService memoryService() {
            return memoryService;
        }
    }

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null)
        );
    }

    private MemoryResponse response(UUID id) {
        return new MemoryResponse(
                id, UUID.randomUUID(), "Test Memory", "Memory content", MemoryType.NOTE,
                MemoryImportance.NORMAL, MemorySource.MANUAL, null,
                List.of("tag1"), Map.of(), Instant.now(), Instant.now(), null, null, null
        );
    }

    @Test
    void shouldCreateMemory() throws Exception {
        UUID memoryId = UUID.randomUUID();
        CreateMemoryRequest request = new CreateMemoryRequest(
                "Test Memory", "Memory content", MemoryType.NOTE, MemoryImportance.NORMAL,
                MemorySource.MANUAL, null, List.of("tag1"), Map.of()
        );
        Mockito.when(TestConfig.memoryService.createMemory(any(UUID.class), any(CreateMemoryRequest.class)))
                .thenReturn(response(memoryId));

        mockMvc.perform(post("/api/v1/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Memory"));
    }

    @Test
    void shouldListMemories() throws Exception {
        Mockito.when(TestConfig.memoryService.listMemories(any(UUID.class), any())).thenReturn(List.of(response(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/memories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Test Memory"));
    }

    @Test
    void shouldGetMemory() throws Exception {
        UUID memoryId = UUID.randomUUID();
        Mockito.when(TestConfig.memoryService.getMemory(any(UUID.class), any(UUID.class))).thenReturn(response(memoryId));

        mockMvc.perform(get("/api/v1/memories/" + memoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(memoryId.toString()));
    }

    @Test
    void shouldUpdateMemory() throws Exception {
        UUID memoryId = UUID.randomUUID();
        UpdateMemoryRequest request = new UpdateMemoryRequest(
                "Updated Memory", null, null, MemoryImportance.HIGH, null, null, null, null
        );
        Mockito.when(TestConfig.memoryService.updateMemory(any(UUID.class), any(UUID.class), any(UpdateMemoryRequest.class)))
                .thenReturn(response(memoryId));

        mockMvc.perform(patch("/api/v1/memories/" + memoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Memory"));
    }

    @Test
    void shouldDeleteMemory() throws Exception {
        UUID memoryId = UUID.randomUUID();
        Mockito.doNothing().when(TestConfig.memoryService).deleteMemory(any(UUID.class), any(UUID.class));

        mockMvc.perform(delete("/api/v1/memories/" + memoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldRejectCreateWithBlankTitle() throws Exception {
        String body = """
                {"title": "", "content": "Some content"}
                """;

        mockMvc.perform(post("/api/v1/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectCreateWithBlankContent() throws Exception {
        String body = """
                {"title": "A title", "content": "  "}
                """;

        mockMvc.perform(post("/api/v1/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectCreateWithOversizedTitle() throws Exception {
        String oversized = "x".repeat(256);
        String body = """
                {"title": "%s", "content": "Some content"}
                """.formatted(oversized);

        mockMvc.perform(post("/api/v1/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectUpdateWithBlankTitle() throws Exception {
        UUID memoryId = UUID.randomUUID();
        String body = """
                {"title": ""}
                """;

        mockMvc.perform(patch("/api/v1/memories/" + memoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnNotFoundWhenMemoryMissing() throws Exception {
        UUID memoryId = UUID.randomUUID();
        Mockito.doThrow(new BusinessException(ErrorCodes.NOT_FOUND, "Memory not found"))
                .when(TestConfig.memoryService).getMemory(any(UUID.class), any(UUID.class));

        mockMvc.perform(get("/api/v1/memories/" + memoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
