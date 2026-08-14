package com.thesystem.modules.task.repository;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskVisibility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:taskrepo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Transactional
class TaskRepositoryIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void shouldGenerateIdWhenSavingNewTask() {
        Task task = new Task();
        task.setUserId(UUID.randomUUID());
        task.setTitle("Generated ID Task");
        task.setStatus(TaskStatus.DRAFT);
        task.setPriority(TaskPriority.NORMAL);
        task.setVisibility(TaskVisibility.PRIVATE);

        Task saved = taskRepository.saveAndFlush(task);

        assertThat(saved.getId()).isNotNull();
        Task found = taskRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Generated ID Task");
        assertThat(found.getUserId()).isEqualTo(task.getUserId());
    }
}
