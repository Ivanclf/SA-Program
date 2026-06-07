package com.sa.promotion.domain.sku.repository;

import com.sa.promotion.domain.sku.entity.Sku;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * SKU仓储接口 (MyBatis Mapper)
 */
@Mapper
public interface SkuRepository {
    void save(Sku sku);
    void update(Sku sku);
    void delete(String skuId);
    Optional<Sku> findBySkuId(String skuId);
    List<Sku> findAll();
    List<Sku> findBySkuIds(List<String> skuIds);
    boolean exists(String skuId);
}
