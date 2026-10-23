package com.frauddetection.repository;

import com.frauddetection.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByName(String name);
    Optional<Merchant> findByNameIgnoreCase(String name);
}
