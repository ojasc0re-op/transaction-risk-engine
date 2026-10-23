package com.frauddetection.repository;

import com.frauddetection.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.avgTransactionAmount = :avg, u.totalTransactions = :total WHERE u.id = :userId")
    void updateTransactionStats(@Param("userId") UUID userId,
                                @Param("avg") BigDecimal avg,
                                @Param("total") int total);
}
