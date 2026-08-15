package com.thesystem.modules.memory.repository;

import com.thesystem.modules.memory.entity.Memory;
import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;
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
        "spring.datasource.url=jdbc:h2:mem:memoryrepo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Transactional
class MemoryRepositoryIntegrationTest {

    @Autowired
    private MemoryRepository memoryRepository;

    private Memory memory(UUID userId, String title, MemoryType type) {
        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setTitle(title);
        memory.setContent("Content for " + title);
        memory.setType(type);
        memory.setImportance(MemoryImportance.NORMAL);
        memory.setSource(MemorySource.MANUAL);
        return memory;
    }

    @Test
    void shouldGenerateIdWhenSavingNewMemory() {
        UUID userId = UUID.randomUUID();
        Memory saved = memoryRepository.saveAndFlush(memory(userId, "Generated ID Memory", MemoryType.NOTE));

        assertThat(saved.getId()).isNotNull();
        Memory found = memoryRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Generated ID Memory");
        assertThat(found.getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldFilterSoftDeletedMemoriesFromUserQueries() {
        UUID userId = UUID.randomUUID();
        Memory kept = memoryRepository.saveAndFlush(memory(userId, "Kept Memory", MemoryType.NOTE));
        Memory deleted = memoryRepository.saveAndFlush(memory(userId, "Deleted Memory", MemoryType.FACT));
        deleted.setDeletedAt(Instant.now());
        memoryRepository.saveAndFlush(deleted);

        List<Memory> active = memoryRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        assertThat(active).extracting(Memory::getTitle).containsExactly("Kept Memory");
        assertThat(active).doesNotContain(deleted);
    }

    @Test
    void shouldScopeQueriesByUserId() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        memoryRepository.saveAndFlush(memory(userA, "A Memory", MemoryType.NOTE));
        memoryRepository.saveAndFlush(memory(userB, "B Memory", MemoryType.FACT));

        List<Memory> userAMemories = memoryRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userA);

        assertThat(userAMemories).extracting(Memory::getTitle).containsExactly("A Memory");
    }

    @Test
    void shouldFindOwnedMemoryOnly() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        Memory aMemory = memoryRepository.saveAndFlush(memory(userA, "A Memory", MemoryType.NOTE));
        memoryRepository.saveAndFlush(memory(userB, "B Memory", MemoryType.NOTE));

        Optional<Memory> owned = memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(aMemory.getId(), userA);
        Optional<Memory> foreign = memoryRepository.findByIdAndUserIdAndDeletedAtIsNull(aMemory.getId(), userB);

        assertThat(owned).isPresent();
        assertThat(foreign).isEmpty();
    }

    @Test
    void shouldFilterByType() {
        UUID userId = UUID.randomUUID();
        memoryRepository.saveAndFlush(memory(userId, "Note Memory", MemoryType.NOTE));
        memoryRepository.saveAndFlush(memory(userId, "Fact Memory", MemoryType.FACT));

        List<Memory> facts = memoryRepository.findByUserIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(userId, MemoryType.FACT);

        assertThat(facts).extracting(Memory::getTitle).containsExactly("Fact Memory");
    }

    @Test
    void shouldSearchByKeywordInTitleAndContent() {
        UUID userId = UUID.randomUUID();
        memoryRepository.saveAndFlush(memory(userId, "Birthday Party", MemoryType.NOTE));
        memoryRepository.saveAndFlush(memory(userId, "Groceries", MemoryType.NOTE));

        List<Memory> results = memoryRepository.searchByUserIdAndKeyword(userId, "birthday");

        assertThat(results).extracting(Memory::getTitle).containsExactly("Birthday Party");
    }
}
