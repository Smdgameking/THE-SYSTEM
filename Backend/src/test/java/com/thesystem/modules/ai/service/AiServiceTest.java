package com.thesystem.modules.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.ai.dto.AiInteractionResponse;
import com.thesystem.modules.ai.dto.CreateAiInteractionRequest;
import com.thesystem.modules.ai.entity.AiInteraction;
import com.thesystem.modules.ai.events.AiInteractionCreatedEvent;
import com.thesystem.modules.ai.mapper.AiInteractionMapper;
import com.thesystem.modules.ai.provider.AiContextItem;
import com.thesystem.modules.ai.provider.AiProvider;
import com.thesystem.modules.ai.provider.AiProviderException;
import com.thesystem.modules.ai.provider.AiProviderRegistry;
import com.thesystem.modules.ai.provider.AiProviderRequest;
import com.thesystem.modules.ai.provider.AiProviderResponse;
import com.thesystem.modules.ai.repository.AiInteractionRepository;
import com.thesystem.modules.ai.service.impl.AiServiceImpl;
import com.thesystem.modules.memory.dto.MemoryFilterRequest;
import com.thesystem.modules.memory.dto.MemoryResponse;
import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;
import com.thesystem.modules.memory.service.MemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private AiInteractionRepository aiInteractionRepository;

    @Mock
    private AiInteractionMapper aiInteractionMapper;

    @Mock
    private MemoryService memoryService;

    @Mock
    private AiProviderRegistry providerRegistry;

    @Mock
    private AiProvider aiProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AiServiceImpl aiService;
    private UUID userId;
    private UUID interactionId;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(aiProvider.name()).thenReturn("mock-provider");
        aiService = new AiServiceImpl(
                aiInteractionRepository, aiInteractionMapper, memoryService, providerRegistry,
                eventPublisher, new ObjectMapper(), "mock-provider");
        userId = UUID.randomUUID();
        interactionId = UUID.randomUUID();
    }

    private AiInteraction interaction() {
        AiInteraction interaction = new AiInteraction();
        interaction.setId(interactionId);
        interaction.setUserId(userId);
        interaction.setMessage("Tell me about my day");
        interaction.setResponse("Here is a summary");
        interaction.setProvider("mock-provider");
        interaction.setModel("mock-model");
        interaction.setCreatedAt(Instant.now());
        interaction.setUpdatedAt(Instant.now());
        return interaction;
    }

    private AiInteractionResponse response(AiInteraction interaction) {
        return new AiInteractionResponse(
                interaction.getId(), interaction.getUserId(), interaction.getMessage(),
                interaction.getResponse(), interaction.getProvider(), interaction.getModel(),
                List.of(), 10, 5, 15, "stop", interaction.getCreatedAt(),
                interaction.getUpdatedAt(), null, null, null
        );
    }

    private MemoryResponse memoryResponse(String title, MemoryType type) {
        return new MemoryResponse(
                UUID.randomUUID(), userId, title, "Content for " + title, type,
                MemoryImportance.HIGH, MemorySource.MANUAL, null,
                List.of("tag"), Map.of(), Instant.now(), Instant.now(), null, null, null
        );
    }

    @Test
    void shouldCreateInteractionSuccessfully() {
        when(providerRegistry.find("mock-provider")).thenReturn(Optional.of(aiProvider));
        when(aiProvider.isConfigured()).thenReturn(true);
        when(aiProvider.generate(any(AiProviderRequest.class)))
                .thenReturn(new AiProviderResponse("Generated response", "mock-model", 10, 5, 15, "stop"));
        when(memoryService.listMemories(any(UUID.class), any(MemoryFilterRequest.class))).thenReturn(List.of());
        when(aiInteractionRepository.save(any(AiInteraction.class))).thenAnswer(invocation -> {
            AiInteraction saved = invocation.getArgument(0);
            saved.setId(interactionId);
            return saved;
        });
        when(aiInteractionMapper.toResponse(any(AiInteraction.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

        AiInteractionResponse result = aiService.createInteraction(userId,
                new CreateAiInteractionRequest("Tell me about my day", null));

        assertThat(result).isNotNull();
        assertThat(result.message()).isEqualTo("Tell me about my day");
        ArgumentCaptor<AiInteraction> captor = ArgumentCaptor.forClass(AiInteraction.class);
        verify(aiInteractionRepository).save(captor.capture());
        AiInteraction saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getMessage()).isEqualTo("Tell me about my day");
        assertThat(saved.getResponse()).isEqualTo("Generated response");
        assertThat(saved.getProvider()).isEqualTo("mock-provider");
        assertThat(saved.getModel()).isEqualTo("mock-model");
        assertThat(saved.getPromptTokens()).isEqualTo(10);
        assertThat(saved.getCompletionTokens()).isEqualTo(5);
        assertThat(saved.getTotalTokens()).isEqualTo(15);
        assertThat(saved.getFinishReason()).isEqualTo("stop");
        ArgumentCaptor<AiInteractionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(AiInteractionCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().interactionId()).isEqualTo(interactionId);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().provider()).isEqualTo("mock-provider");
    }

    @Test
    void shouldPassMemoryContextToProvider() {
        when(providerRegistry.find("mock-provider")).thenReturn(Optional.of(aiProvider));
        when(aiProvider.isConfigured()).thenReturn(true);
        when(aiProvider.generate(any(AiProviderRequest.class)))
                .thenReturn(new AiProviderResponse("Generated response", null, null, null, null, null));
        when(memoryService.listMemories(any(UUID.class), any(MemoryFilterRequest.class)))
                .thenReturn(List.of(memoryResponse("Prefers mornings", MemoryType.PREFERENCE)));
        when(aiInteractionRepository.save(any(AiInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiInteractionMapper.toResponse(any(AiInteraction.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

        aiService.createInteraction(userId, new CreateAiInteractionRequest("Plan my day", true));

        ArgumentCaptor<AiProviderRequest> requestCaptor = ArgumentCaptor.forClass(AiProviderRequest.class);
        verify(aiProvider).generate(requestCaptor.capture());
        AiProviderRequest providerRequest = requestCaptor.getValue();
        assertThat(providerRequest.context()).hasSize(1);
        AiContextItem item = providerRequest.context().get(0);
        assertThat(item.title()).isEqualTo("Prefers mornings");
        assertThat(item.type()).isEqualTo("PREFERENCE");
        assertThat(item.importance()).isEqualTo("HIGH");
        assertThat(item.source()).isEqualTo("MANUAL");
        assertThat(item.content()).isEqualTo("Content for Prefers mornings");
        verify(memoryService).listMemories(userId, new MemoryFilterRequest(null, null, null, null, null, null, null, null));
    }

    @Test
    void shouldBoundContextToMaximumItems() {
        when(providerRegistry.find("mock-provider")).thenReturn(Optional.of(aiProvider));
        when(aiProvider.isConfigured()).thenReturn(true);
        when(aiProvider.generate(any(AiProviderRequest.class)))
                .thenReturn(new AiProviderResponse("Generated response", null, null, null, null, null));
        List<MemoryResponse> many = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            many.add(memoryResponse("Memory " + i, MemoryType.NOTE));
        }
        when(memoryService.listMemories(any(UUID.class), any(MemoryFilterRequest.class))).thenReturn(many);
        when(aiInteractionRepository.save(any(AiInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiInteractionMapper.toResponse(any(AiInteraction.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

        aiService.createInteraction(userId, new CreateAiInteractionRequest("Message", null));

        ArgumentCaptor<AiProviderRequest> requestCaptor = ArgumentCaptor.forClass(AiProviderRequest.class);
        verify(aiProvider).generate(requestCaptor.capture());
        assertThat(requestCaptor.getValue().context()).hasSize(50);
    }

    @Test
    void shouldSkipMemoryQueryWhenIncludeMemoryContextFalse() {
        when(providerRegistry.find("mock-provider")).thenReturn(Optional.of(aiProvider));
        when(aiProvider.isConfigured()).thenReturn(true);
        when(aiProvider.generate(any(AiProviderRequest.class)))
                .thenReturn(new AiProviderResponse("Generated response", null, null, null, null, null));
        when(aiInteractionRepository.save(any(AiInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiInteractionMapper.toResponse(any(AiInteraction.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

        aiService.createInteraction(userId, new CreateAiInteractionRequest("Message", false));

        verify(memoryService, never()).listMemories(any(UUID.class), any(MemoryFilterRequest.class));
        ArgumentCaptor<AiProviderRequest> requestCaptor = ArgumentCaptor.forClass(AiProviderRequest.class);
        verify(aiProvider).generate(requestCaptor.capture());
        assertThat(requestCaptor.getValue().context()).isEmpty();
    }

    @Test
    void shouldFailWhenNoProviderConfigured() {
        aiService = new AiServiceImpl(
                aiInteractionRepository, aiInteractionMapper, memoryService, providerRegistry,
                eventPublisher, new ObjectMapper(), "");

        assertThatThrownBy(() -> aiService.createInteraction(userId, new CreateAiInteractionRequest("Message", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No AI provider is configured")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCodes.SERVICE_UNAVAILABLE);
        verify(aiInteractionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldFailWhenConfiguredProviderNotFound() {
        when(providerRegistry.find("mock-provider")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.createInteraction(userId, new CreateAiInteractionRequest("Message", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI provider is not available")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCodes.SERVICE_UNAVAILABLE);
        verify(aiInteractionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldFailWhenProviderNotConfigured() {
        when(providerRegistry.find("mock-provider")).thenReturn(Optional.of(aiProvider));
        when(aiProvider.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> aiService.createInteraction(userId, new CreateAiInteractionRequest("Message", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI provider is not configured")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCodes.SERVICE_UNAVAILABLE);
        verify(aiProvider, never()).generate(any());
        verify(aiInteractionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldFailWithoutSideEffectsWhenProviderThrows() {
        when(providerRegistry.find("mock-provider")).thenReturn(Optional.of(aiProvider));
        when(aiProvider.isConfigured()).thenReturn(true);
        when(aiProvider.generate(any(AiProviderRequest.class)))
                .thenThrow(new AiProviderException("provider exploded"));

        assertThatThrownBy(() -> aiService.createInteraction(userId, new CreateAiInteractionRequest("Message", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI provider is unavailable")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCodes.SERVICE_UNAVAILABLE);
        verify(aiInteractionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldFailWithoutSideEffectsWhenProviderThrowsRuntimeException() {
        when(providerRegistry.find("mock-provider")).thenReturn(Optional.of(aiProvider));
        when(aiProvider.isConfigured()).thenReturn(true);
        when(aiProvider.generate(any(AiProviderRequest.class)))
                .thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aiService.createInteraction(userId, new CreateAiInteractionRequest("Message", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI provider failed")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCodes.SERVICE_UNAVAILABLE);
        verify(aiInteractionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldGetInteractionSuccessfully() {
        AiInteraction interaction = interaction();
        when(aiInteractionRepository.findByIdAndUserIdAndDeletedAtIsNull(interactionId, userId))
                .thenReturn(Optional.of(interaction));
        when(aiInteractionMapper.toResponse(interaction)).thenReturn(response(interaction));

        AiInteractionResponse result = aiService.getInteraction(userId, interactionId);

        assertThat(result).isNotNull();
        assertThat(result.message()).isEqualTo("Tell me about my day");
    }

    @Test
    void shouldThrowNotFoundWhenGettingMissingInteraction() {
        when(aiInteractionRepository.findByIdAndUserIdAndDeletedAtIsNull(interactionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.getInteraction(userId, interactionId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI interaction not found");
    }

    @Test
    void shouldThrowNotFoundWhenInteractionBelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        when(aiInteractionRepository.findByIdAndUserIdAndDeletedAtIsNull(interactionId, otherUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.getInteraction(otherUserId, interactionId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI interaction not found");
    }

    @Test
    void shouldListInteractions() {
        AiInteraction interaction = interaction();
        when(aiInteractionRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(interaction));
        when(aiInteractionMapper.toResponse(interaction)).thenReturn(response(interaction));

        List<AiInteractionResponse> results = aiService.listInteractions(userId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).message()).isEqualTo("Tell me about my day");
    }

    @Test
    void shouldDeleteInteractionSuccessfully() {
        AiInteraction interaction = interaction();
        when(aiInteractionRepository.findByIdAndUserIdAndDeletedAtIsNull(interactionId, userId))
                .thenReturn(Optional.of(interaction));
        when(aiInteractionRepository.save(any(AiInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        aiService.deleteInteraction(userId, interactionId);

        assertThat(interaction.getDeletedAt()).isNotNull();
        verify(aiInteractionRepository).save(interaction);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowNotFoundWhenDeletingAnotherUsersInteraction() {
        UUID otherUserId = UUID.randomUUID();
        when(aiInteractionRepository.findByIdAndUserIdAndDeletedAtIsNull(interactionId, otherUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.deleteInteraction(otherUserId, interactionId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI interaction not found");
        verify(aiInteractionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
