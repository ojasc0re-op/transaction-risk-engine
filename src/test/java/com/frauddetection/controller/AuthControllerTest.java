package com.frauddetection.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.dto.request.LoginRequest;
import com.frauddetection.dto.request.RegisterRequest;
import com.frauddetection.dto.response.AuthResponse;
import com.frauddetection.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register returns 201 CREATED with AuthResponse")
    void testRegisterEndpoint() throws Exception {
        RegisterRequest request = new RegisterRequest("johndoe", "john@example.com", "Password123!", "John Doe");
        AuthResponse response = AuthResponse.of("jwt.token.abc", "johndoe", "john@example.com", UUID.randomUUID(), Set.of("ROLE_USER"));

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.token.abc"))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login returns 200 OK with JWT token")
    void testLoginEndpoint() throws Exception {
        LoginRequest request = new LoginRequest("johndoe", "Password123!");
        AuthResponse response = AuthResponse.of("jwt.login.token", "johndoe", "john@example.com", UUID.randomUUID(), Set.of("ROLE_USER"));

        when(authService.login(any(LoginRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.login.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid email returns 400 Bad Request")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest badRequest = new RegisterRequest("jd", "not-an-email", "short", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }
}
