package com.thesystem.modules.memory.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.memory.dto.CreateMemoryRequest;
import com.thesystem.modules.memory.dto.MemoryFilterRequest;
import com.thesystem.modules.memory.dto.MemoryResponse;
import com.thesystem.modules.memory.dto.UpdateMemoryRequest;
import com.thesystem.modules.memory.entity.Memory;
import com.thesystem.modules.memory.events.MemoryCreatedEvent;
import com.thesystem.modules.memory.events.MemoryDeletedEvent;
import com.thesystem.modules.memory.events.MemoryUpdatedEvent;
import com.thesystem.modules.memory.mapper.MemoryMapper;
import com.thesystem.modules.memory.repository.MemoryRepository;
import com.thesystem.modules.memory.service.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MemoryServiceImpl implements MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryServiceImpl.class);

    private final MemoryRepository memoryRepository;
    private final MemoryMapper memoryMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public MemoryServiceImpl(
            MemoryRepository memoryRepository,
            MemoryMapper memoryMapper,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.memoryRepository = memoryRepository;
        this.memoryMapper = memoryMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public MemoryResponse createMemory(UUID userId, CreateMemoryRequest request) {
        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setTitle(request.title().trim());
        memory.setContent(request.content().trim());
        memory.setType(request.type() != null ? request.type() : com.thesystem.modules.memory.enums.MemoryType.NOTE);
        memory.setImportance(request.importance() != null ? request.importance() : com.thesystem.modules.memory.enums.MemoryImportance.NORMAL);
        memory.setSource(request.source() != null ? request.source() : com.thesystem.modules.memory.enums.MemorySource.MANUAL);
        memory.setSourceId(request.sourceId());
        memory.setTags(toJson(normalizeTags(request.tags())));
        memory.setCustomMetadata(toJson(request.customMetadata()));

        Memory saved = memoryRepository.save(memory);
        eventPublisher.publishEvent(new MemoryCreatedEvent(
                saved.getId(), saved.getUserId(), saved.getType().name(), saved.getTitle()));
        log.info("Memory created: id={}, userId={}, type={}", saved.getId(), userId, saved.getType());
        return memoryMapper.toMemoryResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MemoryResponse getMemory(UUID userId, UUID memoryId) {
        Memory memory = findOwned(memoryId, userId);
        return memoryMapper.toMemoryResponse(memory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryResponse> listMemories(UUID userId, MemoryFilterRequest filter) {
        List<Memory> memories;
        if (filter.search() != null && !filter.search().isBlank()) {
            memories = memoryRepository.searchByUserIdAndKeyword(userId, filter.search().trim());
        } else if (filter.type() != null) {
            memories = memoryRepository.findByUserIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(userId, filter.type());
        } else if (filter.importance() != null) {
            memories = memoryRepository.findByUserIdAndImportanceAndDeletedAtIsNullOrderByCreatedAtDesc(userId, filter.importance());
        } else if (filter.source() != null) {
            memories = memoryRepository.findByUserIdAndSourceAndDeletedAtIsNullOrderByCreatedAtDesc(userId, filter.source());
        } else {
            memories = memoryRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        }
        return memories.stream()
                .map(memoryMapper::toMemoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MemoryResponse updateMemory(UUID userId, UUID memoryId, UpdateMemoryRequest request) {
        Memory memory = findOwned(memoryId, userId);

        if (request.title() != null) memory.setTitle(request.title().trim());
        if (request.content() != null) memory.setContent(request.content().trim());
        if (request.type() != null) memory.setType(request.type());
        if (request.importance() != null) memory.setImportance(request.importance());
        if (request.source() != null) memory.setSource(request.source());
        if (request.sourceId() != null) memory.setSourceId(request.sourceId());
        if (request.tags() != null) memory.setTags(toJson(normalizeTags(request.tags())));
        if (request.customMetadata() != null) memory.setCustomMetadata(toJson(request.customMetadata()));

        Memory saved = memoryRepository.save(memory);
        eventPublisher.publishEvent(new MemoryUpdatedEvent(
                saved.getId(), saved.getUserId(), saved.getType().name(), saved.getTitle()));
        log.info("Memory updated: id={}, userId={}", saved.getId(), userId);
        return memoryMapper.toMemoryResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMemory(UUID userId, UUID memoryId) {
        Memory memory = findOwned(memoryId, userId);
        memory.setDeletedAt(Instant.now());
        memoryRepository.save(memory);
        eventPublisher.publishEvent(new MemoryDeletedEvent(memoryId, userId));
        log.info("Memory deleted: id={}, userId={}", memoryId, userId);
    }

    private Memory findOwned(UUID memoryId, UUID userId) {
        return memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(memoryId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Memory not found"));
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return null;
        }
        return tags.stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toList());
    }

    private String toJson(Object value) {
        try {
            return value != null ? objectMapper.writeValueAsString(value) : null;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Invalid JSON metadata");
        }
    }
}
