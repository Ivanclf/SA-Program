package com.sa.promotion.domain.promotion.repository;

import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.entity.PromotionSku;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 活动仓储接口 (MyBatis Mapper)
 */
@Mapper
public interface PromotionRepository {
    void save(Promotion promotion);
    void update(Promotion promotion);
    void delete(String promotionId);
    Optional<Promotion> findById(String promotionId);
    List<Promotion> findAll();
    boolean exists(String promotionId);

    // PromotionSku 关联操作
    void insertPromotionSku(PromotionSku promotionSku);
    void deletePromotionSkus(String promotionId);
}
