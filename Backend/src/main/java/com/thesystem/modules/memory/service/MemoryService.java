package com.thesystem.modules.memory.service;

import com.thesystem.modules.memory.dto.CreateMemoryRequest;
import com.thesystem.modules.memory.dto.MemoryFilterRequest;
import com.thesystem.modules.memory.dto.MemoryResponse;
import com.thesystem.modules.memory.dto.UpdateMemoryRequest;

import java.util.List;
import java.util.UUID;

public interface MemoryService {

    /**
     * Creates a new memory for the specified user.
     *
     * @param userId  the ID of the memory owner
     * @param request the memory creation request
     * @return the created memory response
     */
    MemoryResponse createMemory(UUID userId, CreateMemoryRequest request);

    /**
     * Retrieves a memory owned by the specified user.
     *
     * @param userId   the ID of the memory owner
     * @param memoryId the ID of the memory to retrieve
     * @return the memory response
     */
    MemoryResponse getMemory(UUID userId, UUID memoryId);

    /**
     * Lists memories for a user with optional filtering.
     *
     * @param userId the ID of the memory owner
     * @param filter the filter criteria
     * @return a list of memory responses
     */
    List<MemoryResponse> listMemories(UUID userId, MemoryFilterRequest filter);

    /**
     * Updates an existing memory owned by the specified user.
     *
     * @param userId   the ID of the memory owner
     * @param memoryId the ID of the memory to update
     * @param request  the update request containing modified fields
     * @return the updated memory response
     */
    MemoryResponse updateMemory(UUID userId, UUID memoryId, UpdateMemoryRequest request);

    /**
     * Soft-deletes a memory owned by the specified user.
     *
     * @param userId   the ID of the memory owner
     * @param memoryId the ID of the memory to delete
     */
    void deleteMemory(UUID userId, UUID memoryId);
}
