package com.frauddetection.service;

import com.frauddetection.dto.request.LoginRequest;
import com.frauddetection.dto.request.RegisterRequest;
import com.frauddetection.dto.response.AuthResponse;
import com.frauddetection.entity.AuditLog;
import com.frauddetection.entity.Role;
import com.frauddetection.entity.User;
import com.frauddetection.exception.DuplicateResourceException;
import com.frauddetection.repository.RoleRepository;
import com.frauddetection.repository.UserRepository;
import com.frauddetection.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuditService auditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordEncoder,
                authenticationManager,
                jwtUtil,
                userDetailsService,
                auditService
        );
    }

    @Test
    @DisplayName("Successful registration saves user, hashes password, and issues JWT")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "Secret123!", "New User");
        Role role = Role.builder().id(1L).name(Role.RoleName.ROLE_USER).build();
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("newuser")
                .email("new@example.com")
                .passwordHash("encodedHash")
                .fullName("New User")
                .roles(Set.of(role))
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Secret123!")).thenReturn("encodedHash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("newuser")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("mock.jwt.token");
        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.roles()).contains("ROLE_USER");

        verify(userRepository).save(any(User.class));
        verify(auditService).logSimple(eq(AuditLog.AuditEventType.USER_REGISTERED), eq("newuser"), any());
    }

    @Test
    @DisplayName("Registration throws DuplicateResourceException when username already exists")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("existinguser", "test@example.com", "password", "Name");
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Successful login verifies credentials, generates token, and writes audit event")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("testuser", "password");
        Role role = Role.builder().id(1L).name(Role.RoleName.ROLE_USER).build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .roles(Set.of(role))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("login.jwt.token");

        AuthResponse response = authService.login(request, "127.0.0.1");

        assertThat(response.token()).isEqualTo("login.jwt.token");
        assertThat(response.username()).isEqualTo("testuser");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(auditService).log(eq(AuditLog.AuditEventType.USER_LOGIN), eq("testuser"), any(), any(), any(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("Failed login throws BadCredentialsException and writes audit event")
    void testLoginFailureLogsAudit() {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        User user = User.builder().id(UUID.randomUUID()).username("testuser").build();

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);

        verify(auditService).log(eq(AuditLog.AuditEventType.USER_LOGIN_FAILED), eq("testuser"), any(), any(), any(), eq("127.0.0.1"));
    }
}
