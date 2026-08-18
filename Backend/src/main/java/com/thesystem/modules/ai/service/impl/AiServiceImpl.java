package com.thesystem.modules.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.thesystem.modules.ai.service.AiService;
import com.thesystem.modules.memory.dto.MemoryFilterRequest;
import com.thesystem.modules.memory.service.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final int MAX_CONTEXT_ITEMS = 50;

    private final AiInteractionRepository aiInteractionRepository;
    private final AiInteractionMapper aiInteractionMapper;
    private final MemoryService memoryService;
    private final AiProviderRegistry providerRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final String configuredProviderName;

    public AiServiceImpl(
            AiInteractionRepository aiInteractionRepository,
            AiInteractionMapper aiInteractionMapper,
            MemoryService memoryService,
            AiProviderRegistry providerRegistry,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Value("${thesystem.ai.provider:}") String configuredProviderName
    ) {
        this.aiInteractionRepository = aiInteractionRepository;
        this.aiInteractionMapper = aiInteractionMapper;
        this.memoryService = memoryService;
        this.providerRegistry = providerRegistry;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.configuredProviderName = configuredProviderName;
    }

    @Override
    @Transactional
    public AiInteractionResponse createInteraction(UUID userId, CreateAiInteractionRequest request) {
        AiProvider provider = resolveProvider(configuredProviderName);
        boolean includeMemoryContext = request.includeMemoryContext() == null || request.includeMemoryContext();
        List<AiContextItem> context = includeMemoryContext ? buildContext(userId) : List.of();
        AiProviderResponse providerResponse = callProvider(provider, request.message().trim(), context);

        AiInteraction interaction = new AiInteraction();
        interaction.setUserId(userId);
        interaction.setMessage(request.message().trim());
        interaction.setResponse(providerResponse.content());
        interaction.setProvider(provider.name());
        interaction.setModel(providerResponse.model());
        interaction.setContext(toJson(context));
        interaction.setPromptTokens(providerResponse.promptTokens());
        interaction.setCompletionTokens(providerResponse.completionTokens());
        interaction.setTotalTokens(providerResponse.totalTokens());
        interaction.setFinishReason(providerResponse.finishReason());

        AiInteraction saved = aiInteractionRepository.save(interaction);
        eventPublisher.publishEvent(new AiInteractionCreatedEvent(
                saved.getId(), saved.getUserId(), saved.getProvider()));
        log.info("AI interaction created: id={}, userId={}, provider={}, promptTokens={}, totalTokens={}",
                saved.getId(), userId, saved.getProvider(), saved.getPromptTokens(), saved.getTotalTokens());
        return aiInteractionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiInteractionResponse> listInteractions(UUID userId) {
        return aiInteractionRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(aiInteractionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AiInteractionResponse getInteraction(UUID userId, UUID interactionId) {
        AiInteraction interaction = findOwned(interactionId, userId);
        return aiInteractionMapper.toResponse(interaction);
    }

    @Override
    @Transactional
    public void deleteInteraction(UUID userId, UUID interactionId) {
        AiInteraction interaction = findOwned(interactionId, userId);
        interaction.setDeletedAt(Instant.now());
        aiInteractionRepository.save(interaction);
        log.info("AI interaction deleted: id={}, userId={}", interactionId, userId);
    }

    private AiInteraction findOwned(UUID interactionId, UUID userId) {
        return aiInteractionRepository.findByIdAndUserIdAndDeletedAtIsNull(interactionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "AI interaction not found"));
    }

    private AiProvider resolveProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            throw new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE,
                    "No AI provider is configured. Set THE_SYSTEM_AI_PROVIDER to select a provider.");
        }
        AiProvider provider = providerRegistry.find(providerName)
                .orElseThrow(() -> new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE,
                        "AI provider is not available: " + providerName));
        if (!provider.isConfigured()) {
            throw new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE,
                    "AI provider is not configured: " + providerName);
        }
        return provider;
    }

    private AiProviderResponse callProvider(AiProvider provider, String message, List<AiContextItem> context) {
        try {
            return provider.generate(new AiProviderRequest(message, context));
        } catch (AiProviderException e) {
            log.warn("AI provider failed: provider={}, reason={}", provider.name(), e.getMessage());
            throw new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE,
                    "AI provider is unavailable: " + provider.name());
        } catch (RuntimeException e) {
            log.error("Unexpected AI provider error: provider={}", provider.name(), e);
            throw new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE,
                    "AI provider failed: " + provider.name());
        }
    }

    private List<AiContextItem> buildContext(UUID userId) {
        return memoryService.listMemories(userId, new MemoryFilterRequest(null, null, null, null, null, null, null, null))
                .stream()
                .limit(MAX_CONTEXT_ITEMS)
                .map(memory -> new AiContextItem(
                        memory.title(),
                        memory.type() != null ? memory.type().name() : null,
                        memory.importance() != null ? memory.importance().name() : null,
                        memory.source() != null ? memory.source().name() : null,
                        memory.content()))
                .toList();
    }

    private String toJson(List<AiContextItem> context) {
        try {
            return context != null ? objectMapper.writeValueAsString(context) : null;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Invalid context metadata");
        }
    }
}
