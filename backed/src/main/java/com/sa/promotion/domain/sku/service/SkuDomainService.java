package com.sa.promotion.domain.sku.service;

import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.sku.entity.Sku;
import com.sa.promotion.domain.sku.repository.SkuRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SKU管理域服务 - SKU生命周期管理
 *
 * 数据访问：通过注入的 SkuRepository（MyBatis Mapper 代理）操作数据库
 */
@Service
public class SkuDomainService {

    private final SkuRepository skuRepository;

    public SkuDomainService(SkuRepository skuRepository) {
        this.skuRepository = skuRepository;
    }

    public Sku createSku(String skuName, BigDecimal originalPrice) {
        if (skuName == null || skuName.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU name cannot be empty");
        }
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Original price must be greater than zero");
        }

        Sku sku = Sku.builder()
            .skuId(UUID.randomUUID().toString())
            .skuName(skuName.trim())
            .originalPrice(originalPrice)
            .build();

        skuRepository.save(sku);
        return sku;
    }

    public Sku updateSku(String skuId, String skuName, BigDecimal originalPrice) {
        Sku sku = findBySkuId(skuId);
        if (sku == null) {
            throw new ResourceNotFoundException("SKU not found: " + skuId);
        }
        if (skuName != null && !skuName.trim().isEmpty()) {
            sku.setSkuName(skuName.trim());
        }
        if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            sku.setOriginalPrice(originalPrice);
        }
        skuRepository.update(sku);
        return sku;
    }

    public void deleteSku(String skuId) {
        if (findBySkuId(skuId) == null) {
            throw new ResourceNotFoundException("SKU not found: " + skuId);
        }
        skuRepository.delete(skuId);
    }

    public Sku findBySkuId(String skuId) {
        Optional<Sku> sku = skuRepository.findBySkuId(skuId);
        return sku.orElse(null);
    }

    public List<Sku> listAllSkus() {
        return skuRepository.findAll();
    }

    public List<Sku> batchQuerySkus(List<String> skuIds) {
        return skuRepository.findBySkuIds(skuIds);
    }

    public BigDecimal calculateDiscountedPrice(String skuId, BigDecimal discount) {
        Sku sku = findBySkuId(skuId);
        if (sku == null) {
            throw new IllegalArgumentException("SKU not found: " + skuId);
        }
        return sku.calculateDiscountedPrice(discount);
    }

    public boolean exists(String skuId) {
        return skuRepository.exists(skuId);
    }
}
