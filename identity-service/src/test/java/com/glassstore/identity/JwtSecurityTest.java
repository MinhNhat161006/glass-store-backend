package com.glassstore.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glassstore.identity.dto.LoginRequest;
import com.glassstore.identity.dto.RefreshTokenRequest;
import com.glassstore.identity.entity.Role;
import com.glassstore.identity.entity.User;
import com.glassstore.identity.repository.RoleRepository;
import com.glassstore.identity.repository.UserRepository;
import com.glassstore.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class JwtSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("Khách hàng").build()));

        sampleUser = userRepository.findByEmail("jwt_test_user@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("jwt_test_user")
                        .email("jwt_test_user@example.com")
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .fullName("JWT Test User")
                        .status("ACTIVE")
                        .failedAttempts(0)
                        .roles(new HashSet<>(Collections.singletonList(customerRole)))
                        .build()));
    }

    @Test
    @DisplayName("1. JWT AccessToken hợp lệ truy cập được endpoint /api/v1/auth/me")
    void testValidJwtAccessEndpointMe() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(sampleUser);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("jwt_test_user@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("JWT Test User"));
    }

    @Test
    @DisplayName("2. Request không có token hoặc token không hợp lệ bị từ chối HTTP 401")
    void testInvalidJwtRejected() throws Exception {
        // Không có header
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        // Token giả mạo
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-jwt-signature-token-12345"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Làm mới token thành công qua /api/v1/auth/refresh")
    void testRefreshToken() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshToken(sampleUser);
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("4. Đăng xuất qua /api/v1/auth/logout")
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));
    }
}

