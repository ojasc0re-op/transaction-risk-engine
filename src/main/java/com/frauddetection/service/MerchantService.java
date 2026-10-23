package com.frauddetection.service;

import com.frauddetection.entity.Merchant;
import com.frauddetection.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public Optional<Merchant> findByName(String name) {
        return merchantRepository.findByNameIgnoreCase(name);
    }

    public boolean isHighRiskMerchant(String merchantName) {
        return merchantRepository.findByNameIgnoreCase(merchantName)
                .map(m -> m.getRiskLevel() == Merchant.MerchantRiskLevel.HIGH)
                .orElse(false);
    }

    @Transactional
    public Merchant save(Merchant merchant) {
        return merchantRepository.save(merchant);
    }
}
