package com.thesystem.security;

import com.thesystem.modules.auth.entity.RefreshToken;
import com.thesystem.modules.auth.entity.Role;
import com.thesystem.modules.auth.entity.User;
import com.thesystem.modules.auth.entity.UserRole;
import com.thesystem.modules.auth.repository.RefreshTokenRepository;
import com.thesystem.modules.auth.repository.RoleRepository;
import com.thesystem.modules.auth.repository.UserRepository;
import com.thesystem.modules.auth.repository.UserRoleRepository;
import com.thesystem.security.service.JwtTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
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
    private RefreshTokenRepository refreshTokenRepository;

    private UUID testUserId;
    private String testUserEmail;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void shouldAuthenticateWithValidAccessToken() throws Exception {
        String accessToken = jwtTokenService.generateAccessToken(testUserId, testUserEmail);

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertThat(content).contains("success");
                    assertThat(content).contains(testUserId.toString());
                });
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
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
    }
}
