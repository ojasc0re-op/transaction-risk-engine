package com.frauddetection.config;

import com.frauddetection.entity.Merchant;
import com.frauddetection.entity.Role;
import com.frauddetection.entity.User;
import com.frauddetection.repository.MerchantRepository;
import com.frauddetection.repository.RoleRepository;
import com.frauddetection.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Initializes seed data when running in "dev" profile (in-memory H2 mode)
 * so the application is completely functional out-of-the-box without PostgreSQL.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Dev profile active: Initializing in-memory seed data...");

        // 1. Roles
        Role roleUser = roleRepository.save(Role.builder().name(Role.RoleName.ROLE_USER).build());
        Role roleAnalyst = roleRepository.save(Role.builder().name(Role.RoleName.ROLE_ANALYST).build());
        Role roleAdmin = roleRepository.save(Role.builder().name(Role.RoleName.ROLE_ADMIN).build());

        String defaultPass = passwordEncoder.encode("password");

        // 2. Users
        userRepository.save(User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .username("admin")
                .email("admin@frauddetect.dev")
                .passwordHash(defaultPass)
                .fullName("System Admin")
                .avgTransactionAmount(new BigDecimal("500.00"))
                .totalTransactions(50)
                .roles(Set.of(roleAdmin, roleUser))
                .build());

        userRepository.save(User.builder()
                .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .username("analyst")
                .email("analyst@frauddetect.dev")
                .passwordHash(defaultPass)
                .fullName("Fraud Analyst")
                .avgTransactionAmount(new BigDecimal("250.00"))
                .totalTransactions(20)
                .roles(Set.of(roleAnalyst, roleUser))
                .build());

        userRepository.save(User.builder()
                .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .username("john_doe")
                .email("john@example.com")
                .passwordHash(defaultPass)
                .fullName("John Doe")
                .avgTransactionAmount(new BigDecimal("150.00"))
                .totalTransactions(30)
                .roles(Set.of(roleUser))
                .build());

        userRepository.save(User.builder()
                .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .username("jane_smith")
                .email("jane@example.com")
                .passwordHash(defaultPass)
                .fullName("Jane Smith")
                .avgTransactionAmount(new BigDecimal("300.00"))
                .totalTransactions(15)
                .roles(Set.of(roleUser))
                .build());

        // 3. Merchants
        merchantRepository.save(Merchant.builder().name("Amazon").riskLevel(Merchant.MerchantRiskLevel.LOW).category("E-COMMERCE").build());
        merchantRepository.save(Merchant.builder().name("Walmart").riskLevel(Merchant.MerchantRiskLevel.LOW).category("RETAIL").build());
        merchantRepository.save(Merchant.builder().name("TechStore Pro").riskLevel(Merchant.MerchantRiskLevel.MEDIUM).category("ELECTRONICS").build());
        merchantRepository.save(Merchant.builder().name("Crypto Exchange XYZ").riskLevel(Merchant.MerchantRiskLevel.HIGH).category("CRYPTOCURRENCY").build());
        merchantRepository.save(Merchant.builder().name("Online Casino Royal").riskLevel(Merchant.MerchantRiskLevel.HIGH).category("GAMBLING").build());
        merchantRepository.save(Merchant.builder().name("Quick Cash Advance").riskLevel(Merchant.MerchantRiskLevel.HIGH).category("FINANCIAL").build());

        log.info("Seed data successfully loaded: users, roles, and merchants ready.");
    }
}
