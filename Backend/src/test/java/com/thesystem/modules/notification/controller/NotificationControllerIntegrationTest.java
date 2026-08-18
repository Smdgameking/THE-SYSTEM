package com.thesystem.modules.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.notification.dto.NotificationResponse;
import com.thesystem.modules.notification.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        static NotificationService notificationService = Mockito.mock(NotificationService.class);

        @Bean
        NotificationService notificationService() {
            return notificationService;
        }
    }

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null)
        );
    }

    private NotificationResponse response(UUID id) {
        return new NotificationResponse(
                id, UUID.randomUUID(), "TASK_COMPLETED", "Task completed", "Test task completed",
                "TASK", UUID.randomUUID(), null, false, null,
                Instant.now(), Instant.now(), null, null, null
        );
    }

    @Test
    void shouldListNotifications() throws Exception {
        Mockito.when(TestConfig.notificationService.getNotifications(any(UUID.class)))
                .thenReturn(List.of(response(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("TASK_COMPLETED"));
    }

    @Test
    void shouldGetNotificationById() throws Exception {
        UUID notificationId = UUID.randomUUID();
        Mockito.when(TestConfig.notificationService.getNotification(any(UUID.class), eq(notificationId)))
                .thenReturn(response(notificationId));

        mockMvc.perform(get("/api/v1/notifications/" + notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(notificationId.toString()));
    }

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        UUID notificationId = UUID.randomUUID();
        Mockito.when(TestConfig.notificationService.markAsRead(any(UUID.class), eq(notificationId)))
                .thenReturn(response(notificationId));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldMarkAllNotificationsAsRead() throws Exception {
        Mockito.doNothing().when(TestConfig.notificationService).markAllAsRead(any(UUID.class));

        mockMvc.perform(patch("/api/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldDeleteNotification() throws Exception {
        UUID notificationId = UUID.randomUUID();
        Mockito.doNothing().when(TestConfig.notificationService).deleteNotification(any(UUID.class), eq(notificationId));

        mockMvc.perform(delete("/api/v1/notifications/" + notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldReturnNotFoundForMissingNotification() throws Exception {
        UUID notificationId = UUID.randomUUID();
        Mockito.when(TestConfig.notificationService.getNotification(any(UUID.class), eq(notificationId)))
                .thenThrow(new com.thesystem.common.exception.BusinessException("NOT_FOUND", "Notification not found"));

        mockMvc.perform(get("/api/v1/notifications/" + notificationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
