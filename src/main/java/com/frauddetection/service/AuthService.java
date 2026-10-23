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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found in database"));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .roles(Set.of(userRole))
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered successfully: {}", saved.getUsername());

        auditService.logSimple(AuditLog.AuditEventType.USER_REGISTERED, saved.getUsername(), saved.getId());

        UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        Set<String> roles = saved.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.of(token, saved.getUsername(), saved.getEmail(), saved.getId(), roles);
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {}", request.username());
            userRepository.findByUsername(request.username()).ifPresent(u ->
                    auditService.log(AuditLog.AuditEventType.USER_LOGIN_FAILED,
                            request.username(), u.getId(), null, null, ipAddress));
            throw e;
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        log.info("User logged in: {}", user.getUsername());
        auditService.log(AuditLog.AuditEventType.USER_LOGIN,
                user.getUsername(), user.getId(), null, null, ipAddress);

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.of(token, user.getUsername(), user.getEmail(), user.getId(), roles);
    }
}
