package com.thesystem.modules.ai.repository;

import com.thesystem.modules.ai.entity.AiInteraction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:airepo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Transactional
class AiInteractionRepositoryIntegrationTest {

    @Autowired
    private AiInteractionRepository aiInteractionRepository;

    private AiInteraction interaction(UUID userId, String message) {
        AiInteraction interaction = new AiInteraction();
        interaction.setUserId(userId);
        interaction.setMessage(message);
        interaction.setResponse("Response to " + message);
        interaction.setProvider("mock-provider");
        return interaction;
    }

    @Test
    void shouldGenerateIdWhenSavingNewInteraction() {
        UUID userId = UUID.randomUUID();
        AiInteraction saved = aiInteractionRepository.saveAndFlush(interaction(userId, "Hello AI"));

        assertThat(saved.getId()).isNotNull();
        AiInteraction found = aiInteractionRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getMessage()).isEqualTo("Hello AI");
        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.getResponse()).isEqualTo("Response to Hello AI");
    }

    @Test
    void shouldFilterSoftDeletedInteractionsFromUserQueries() {
        UUID userId = UUID.randomUUID();
        AiInteraction kept = aiInteractionRepository.saveAndFlush(interaction(userId, "Kept interaction"));
        AiInteraction deleted = aiInteractionRepository.saveAndFlush(interaction(userId, "Deleted interaction"));
        deleted.setDeletedAt(Instant.now());
        aiInteractionRepository.saveAndFlush(deleted);

        List<AiInteraction> active = aiInteractionRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        assertThat(active).extracting(AiInteraction::getMessage).containsExactly("Kept interaction");
        assertThat(active).doesNotContain(deleted);
    }

    @Test
    void shouldScopeQueriesByUserId() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        aiInteractionRepository.saveAndFlush(interaction(userA, "A interaction"));
        aiInteractionRepository.saveAndFlush(interaction(userB, "B interaction"));

        List<AiInteraction> userAInteractions = aiInteractionRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userA);

        assertThat(userAInteractions).extracting(AiInteraction::getMessage).containsExactly("A interaction");
    }

    @Test
    void shouldFindOwnedInteractionOnly() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        AiInteraction aInteraction = aiInteractionRepository.saveAndFlush(interaction(userA, "A interaction"));
        aiInteractionRepository.saveAndFlush(interaction(userB, "B interaction"));

        Optional<AiInteraction> owned = aiInteractionRepository.findByIdAndUserIdAndDeletedAtIsNull(aInteraction.getId(), userA);
        Optional<AiInteraction> foreign = aiInteractionRepository.findByIdAndUserIdAndDeletedAtIsNull(aInteraction.getId(), userB);

        assertThat(owned).isPresent();
        assertThat(foreign).isEmpty();
    }
}
