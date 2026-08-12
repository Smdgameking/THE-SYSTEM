package com.thesystem.security;

import com.thesystem.modules.auth.entity.Role;
import com.thesystem.modules.auth.entity.User;
import com.thesystem.modules.auth.entity.UserRole;
import com.thesystem.modules.auth.repository.RoleRepository;
import com.thesystem.modules.auth.repository.UserRepository;
import com.thesystem.modules.auth.repository.UserRoleRepository;
import com.thesystem.modules.user.entity.UserProfile;
import com.thesystem.modules.user.repository.UserProfileRepository;
import com.thesystem.modules.user.service.UserService;
import com.thesystem.security.service.JwtTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityIntegrationTest {

    private static final String TEST_SECRET = "test-secret-change-me-in-production-use-a-long-random-string-at-least-256-bits";
    private static final SecretKey DIFFERENT_SIGNING_KEY = Keys.hmacShaKeyFor(
            "a-different-secret-key-that-is-long-enough-for-hmac-sha-256".getBytes(StandardCharsets.UTF_8)
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @MockBean
    private UserService userService;

    private UUID testUserId;
    private String testUserEmail;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUserEmail = "test-" + UUID.randomUUID() + "@example.com";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(testUserEmail);
        user.setPasswordHash("hashed-password");
        user.setEmailVerified(true);
        userRepository.save(user);
        testUserId = user.getId();

        Role role = roleRepository.findByNameAndDeletedAtIsNull("USER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setId(UUID.randomUUID());
                    newRole.setName("USER");
                    newRole.setDescription("Default user role");
                    return roleRepository.save(newRole);
                });

        UserRole userRole = new UserRole();
        userRole.setUserId(testUserId);
        userRole.setRoleId(role.getId());
        userRoleRepository.save(userRole);

        UserProfile profile = new UserProfile();
        profile.setId(testUserId);
        profile.setUserId(testUserId);
        profile.setAccountStatus("ACTIVE");
        userProfileRepository.save(profile);
    }

    @Test
    void shouldAuthenticateWithValidAccessToken() throws Exception {
        String accessToken = jwtTokenService.generateAccessToken(testUserId, testUserEmail);

        com.thesystem.modules.user.dto.UserProfileResponse mockResponse = new com.thesystem.modules.user.dto.UserProfileResponse(
                testUserId, testUserId, "testuser", "Test User", null, null, null, null, null, "ACTIVE",
                java.time.Instant.now(), java.time.Instant.now(), java.time.Instant.now()
        );
        when(userService.getMyProfile(testUserId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenMalformedJwt() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer malformed-token-12345"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenExpiredJwt() throws Exception {
        String expiredToken = Jwts.builder()
                .subject(testUserId.toString())
                .claim("email", testUserEmail)
                .claim("type", "access")
                .issuedAt(new Date(System.currentTimeMillis() - 3600000))
                .expiration(new Date(System.currentTimeMillis() - 1800000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenInvalidSignature() throws Exception {
        String invalidToken = Jwts.builder()
                .subject(testUserId.toString())
                .claim("email", testUserEmail)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .signWith(DIFFERENT_SIGNING_KEY)
                .compact();

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenRefreshTokenUsedAsAccessToken() throws Exception {
        String refreshToken = jwtTokenService.generateRefreshToken(testUserId);

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenValidTokenButUserNotFound() throws Exception {
        String validToken = jwtTokenService.generateAccessToken(UUID.randomUUID(), "nonexistent@example.com");

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowPublicEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/register"))
                .andExpect(status().is4xxClientError());
    }
}
