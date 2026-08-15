package com.thesystem.modules.memory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.memory.dto.CreateMemoryRequest;
import com.thesystem.modules.memory.dto.MemoryFilterRequest;
import com.thesystem.modules.memory.dto.MemoryResponse;
import com.thesystem.modules.memory.dto.UpdateMemoryRequest;
import com.thesystem.modules.memory.entity.Memory;
import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;
import com.thesystem.modules.memory.events.MemoryCreatedEvent;
import com.thesystem.modules.memory.events.MemoryDeletedEvent;
import com.thesystem.modules.memory.events.MemoryUpdatedEvent;
import com.thesystem.modules.memory.mapper.MemoryMapper;
import com.thesystem.modules.memory.repository.MemoryRepository;
import com.thesystem.modules.memory.service.impl.MemoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
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
class MemoryServiceTest {

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private MemoryMapper memoryMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MemoryServiceImpl memoryService;
    private UUID userId;
    private UUID memoryId;
    private Memory memory;

    @BeforeEach
    void setUp() {
        memoryService = new MemoryServiceImpl(
                memoryRepository, memoryMapper, new ObjectMapper(), eventPublisher);
        userId = UUID.randomUUID();
        memoryId = UUID.randomUUID();
        memory = new Memory();
        memory.setId(memoryId);
        memory.setUserId(userId);
        memory.setTitle("Test Memory");
        memory.setContent("Memory content");
        memory.setType(MemoryType.NOTE);
        memory.setImportance(MemoryImportance.NORMAL);
        memory.setSource(MemorySource.MANUAL);
        memory.setCreatedAt(Instant.now());
        memory.setUpdatedAt(Instant.now());
    }

    private MemoryResponse memoryResponse(UUID id, String title) {
        return new MemoryResponse(
                id, userId, title, "Memory content", MemoryType.NOTE,
                MemoryImportance.NORMAL, MemorySource.MANUAL, null,
                List.of("tag1"), Map.of(), Instant.now(), Instant.now(), null, null, null
        );
    }

    @Test
    void shouldCreateMemorySuccessfully() {
        CreateMemoryRequest request = new CreateMemoryRequest(
                "New Memory", "Some content", MemoryType.FACT, MemoryImportance.HIGH,
                MemorySource.MANUAL, null, List.of("tag1", " tag2 "), Map.of("key", "value")
        );
        when(memoryRepository.save(any(Memory.class))).thenAnswer(invocation -> {
            Memory m = invocation.getArgument(0);
            m.setId(memoryId);
            return m;
        });
        when(memoryMapper.toMemoryResponse(any(Memory.class)))
                .thenReturn(memoryResponse(memoryId, "New Memory"));

        MemoryResponse response = memoryService.createMemory(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("New Memory");
        ArgumentCaptor<Memory> captor = ArgumentCaptor.forClass(Memory.class);
        verify(memoryRepository).save(captor.capture());
        Memory saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getType()).isEqualTo(MemoryType.FACT);
        assertThat(saved.getImportance()).isEqualTo(MemoryImportance.HIGH);
        assertThat(saved.getTags()).isEqualTo("[\"tag1\",\"tag2\"]");
        ArgumentCaptor<MemoryCreatedEvent> eventCaptor = ArgumentCaptor.forClass(MemoryCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().memoryId()).isEqualTo(memoryId);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void shouldApplyDefaultsOnCreateWhenFieldsMissing() {
        CreateMemoryRequest request = new CreateMemoryRequest(
                "Default Memory", "Content", null, null, null, null, null, null
        );
        when(memoryRepository.save(any(Memory.class))).thenAnswer(invocation -> {
            Memory m = invocation.getArgument(0);
            m.setId(memoryId);
            return m;
        });
        when(memoryMapper.toMemoryResponse(any(Memory.class))).thenReturn(memoryResponse(memoryId, "Default Memory"));

        memoryService.createMemory(userId, request);

        ArgumentCaptor<Memory> captor = ArgumentCaptor.forClass(Memory.class);
        verify(memoryRepository).save(captor.capture());
        Memory saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(MemoryType.NOTE);
        assertThat(saved.getImportance()).isEqualTo(MemoryImportance.NORMAL);
        assertThat(saved.getSource()).isEqualTo(MemorySource.MANUAL);
    }

    @Test
    void shouldGetMemorySuccessfully() {
        when(memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(memoryId, userId)).thenReturn(Optional.of(memory));
        when(memoryMapper.toMemoryResponse(memory)).thenReturn(memoryResponse(memoryId, "Test Memory"));

        MemoryResponse response = memoryService.getMemory(userId, memoryId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Test Memory");
    }

    @Test
    void shouldThrowNotFoundWhenGettingNonExistentMemory() {
        when(memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(memoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.getMemory(userId, memoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Memory not found");
    }

    @Test
    void shouldThrowNotFoundWhenMemoryBelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        when(memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(memoryId, otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.getMemory(otherUserId, memoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Memory not found");
    }

    @Test
    void shouldListMemoriesWithoutFilter() {
        when(memoryRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)).thenReturn(List.of(memory));
        when(memoryMapper.toMemoryResponse(memory)).thenReturn(memoryResponse(memoryId, "Test Memory"));

        List<MemoryResponse> responses = memoryService.listMemories(userId, new MemoryFilterRequest(
                null, null, null, null, null, null, null, null));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("Test Memory");
    }

    @Test
    void shouldListMemoriesFilteredByType() {
        when(memoryRepository.findByUserIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(userId, MemoryType.FACT))
                .thenReturn(List.of(memory));
        when(memoryMapper.toMemoryResponse(memory)).thenReturn(memoryResponse(memoryId, "Test Memory"));

        List<MemoryResponse> responses = memoryService.listMemories(userId, new MemoryFilterRequest(
                MemoryType.FACT, null, null, null, null, null, null, null));

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldSearchMemoriesByKeyword() {
        when(memoryRepository.searchByUserIdAndKeyword(userId, "keyword")).thenReturn(List.of(memory));
        when(memoryMapper.toMemoryResponse(memory)).thenReturn(memoryResponse(memoryId, "Test Memory"));

        List<MemoryResponse> responses = memoryService.listMemories(userId, new MemoryFilterRequest(
                null, null, null, " keyword ", null, null, null, null));

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldUpdateMemorySuccessfully() {
        UpdateMemoryRequest request = new UpdateMemoryRequest(
                "Updated Title", "Updated content", MemoryType.PREFERENCE, MemoryImportance.HIGH,
                MemorySource.AI, UUID.randomUUID(), List.of("new-tag"), Map.of("k", "v")
        );
        when(memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(memoryId, userId)).thenReturn(Optional.of(memory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memoryMapper.toMemoryResponse(any(Memory.class))).thenReturn(memoryResponse(memoryId, "Updated Title"));

        MemoryResponse response = memoryService.updateMemory(userId, memoryId, request);

        assertThat(response.title()).isEqualTo("Updated Title");
        assertThat(memory.getType()).isEqualTo(MemoryType.PREFERENCE);
        assertThat(memory.getSource()).isEqualTo(MemorySource.AI);
        assertThat(memory.getTitle()).isEqualTo("Updated Title");
        verify(eventPublisher).publishEvent(any(MemoryUpdatedEvent.class));
    }

    @Test
    void shouldPartiallyUpdateOnlyProvidedFields() {
        UpdateMemoryRequest request = new UpdateMemoryRequest(
                "Only Title", null, null, null, null, null, null, null
        );
        when(memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(memoryId, userId)).thenReturn(Optional.of(memory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memoryMapper.toMemoryResponse(any(Memory.class))).thenReturn(memoryResponse(memoryId, "Only Title"));

        memoryService.updateMemory(userId, memoryId, request);

        assertThat(memory.getTitle()).isEqualTo("Only Title");
        assertThat(memory.getContent()).isEqualTo("Memory content");
        assertThat(memory.getType()).isEqualTo(MemoryType.NOTE);
        verify(eventPublisher).publishEvent(any(MemoryUpdatedEvent.class));
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingMemory() {
        UpdateMemoryRequest request = new UpdateMemoryRequest(
                "Title", null, null, null, null, null, null, null
        );
        when(memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(memoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.updateMemory(userId, memoryId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Memory not found");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldDeleteMemorySuccessfully() {
        when(memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(memoryId, userId)).thenReturn(Optional.of(memory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        memoryService.deleteMemory(userId, memoryId);

        assertThat(memory.getDeletedAt()).isNotNull();
        ArgumentCaptor<MemoryDeletedEvent> eventCaptor = ArgumentCaptor.forClass(MemoryDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().memoryId()).isEqualTo(memoryId);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingAnotherUsersMemory() {
        UUID otherUserId = UUID.randomUUID();
        when(memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(memoryId, otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.deleteMemory(otherUserId, memoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Memory not found");
        verify(memoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
