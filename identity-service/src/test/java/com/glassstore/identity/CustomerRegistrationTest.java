package com.glassstore.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glassstore.identity.dto.CustomerRegisterRequest;
import com.glassstore.identity.entity.User;
import com.glassstore.identity.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("1. Đăng ký khách hàng thành công: Tài khoản ACTIVE, role ROLE_CUSTOMER, mã hóa BCrypt, không có OTP")
    void testRegisterCustomerSuccess() throws Exception {
        CustomerRegisterRequest request = CustomerRegisterRequest.builder()
                .fullName("Nguyen Van A")
                .email("test_reg_success@example.com")
                .password("Password123!")
                .phoneNumber("0912345678")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("test_reg_success@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        // Kiểm tra trong database
        User saved = userRepository.findByEmail("test_reg_success@example.com").orElse(null);
        assertNotNull(saved);
        assertEquals("ACTIVE", saved.getStatus());
        assertTrue(passwordEncoder.matches("Password123!", saved.getPasswordHash()));
        assertFalse(saved.getPasswordHash().contains("Password123!"));
        assertEquals(0, saved.getFailedAttempts());
        assertNull(saved.getLockoutExpiration());
    }

    @Test
    @DisplayName("2. Đăng ký email trùng: Trả lỗi HTTP 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        CustomerRegisterRequest request1 = CustomerRegisterRequest.builder()
                .fullName("Nguyen Van A")
                .email("duplicate_email@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Đăng ký lại với cùng email
        CustomerRegisterRequest request2 = CustomerRegisterRequest.builder()
                .fullName("Nguyen Van B")
                .email("duplicate_email@example.com")
                .password("Password456!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Email 'duplicate_email@example.com' đã được đăng ký trong hệ thống!"));
    }

    @Test
    @DisplayName("3. Đăng ký với email không hợp lệ: Trả lỗi HTTP 400 Bad Request")
    void testRegisterInvalidEmail() throws Exception {
        CustomerRegisterRequest request = CustomerRegisterRequest.builder()
                .fullName("Nguyen Van A")
                .email("invalid-email-format")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("4. Đăng ký với mật khẩu quá ngắn (< 6 ký tự): Trả lỗi HTTP 400 Bad Request")
    void testRegisterShortPassword() throws Exception {
        CustomerRegisterRequest request = CustomerRegisterRequest.builder()
                .fullName("Nguyen Van A")
                .email("short_pass@example.com")
                .password("123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}

