package com.thesystem.modules.ai.service;

import com.thesystem.modules.ai.dto.AiInteractionResponse;
import com.thesystem.modules.ai.dto.CreateAiInteractionRequest;

import java.util.List;
import java.util.UUID;

public interface AiService {

    /**
     * Processes a new AI interaction for the specified user: builds context from
     * memory, calls the configured provider, and persists the interaction on success.
     *
     * @param userId  the ID of the interaction owner
     * @param request the interaction request
     * @return the created interaction response
     */
    AiInteractionResponse createInteraction(UUID userId, CreateAiInteractionRequest request);

    /**
     * Lists AI interactions owned by the specified user, newest first.
     *
     * @param userId the ID of the interaction owner
     * @return the user's interaction responses
     */
    List<AiInteractionResponse> listInteractions(UUID userId);

    /**
     * Retrieves a single AI interaction owned by the specified user.
     *
     * @param userId        the ID of the interaction owner
     * @param interactionId the ID of the interaction to retrieve
     * @return the interaction response
     */
    AiInteractionResponse getInteraction(UUID userId, UUID interactionId);

    /**
     * Soft-deletes an AI interaction owned by the specified user.
     *
     * @param userId        the ID of the interaction owner
     * @param interactionId the ID of the interaction to delete
     */
    void deleteInteraction(UUID userId, UUID interactionId);
}
