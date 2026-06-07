package com.sa.promotion.application.service;

import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.service.EventBusService;
import com.sa.promotion.domain.event.service.EventLogService;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.repository.PromotionRepository;
import com.sa.promotion.domain.promotion.service.PromotionDomainService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务服务 - APP-005
 *
 * 职责：
 * - 自动生效：扫描待生效活动（INIT + 审核通过 + 开始时间已到），自动上线
 * - 自动过期：扫描生效中活动（ONLINE + 结束时间已过），自动标记过期
 *
 * 调度配置：
 * - 自动生效检查：每分钟执行一次
 * - 自动过期检查：每分钟执行一次
 */
@Service
public class ScheduledTaskService {

    private static final String SYSTEM_OPERATOR = "SYSTEM";

    private final PromotionDomainService promotionDomainService;
    private final PromotionRepository promotionRepository;
    private final EventBusService eventBusService;
    private final EventLogService eventLogService;

    public ScheduledTaskService(PromotionDomainService promotionDomainService,
                                PromotionRepository promotionRepository,
                                EventBusService eventBusService,
                                EventLogService eventLogService) {
        this.promotionDomainService = promotionDomainService;
        this.promotionRepository = promotionRepository;
        this.eventBusService = eventBusService;
        this.eventLogService = eventLogService;
    }

    /**
     * 自动生效定时任务
     * 按配置间隔扫描所有待生效活动，将到达开始时间的活动自动上线
     * 配置项: promotion.scheduled.auto-activate-rate (默认 60000ms)
     */
    @Scheduled(fixedRateString = "${promotion.scheduled.auto-activate-rate:60000}")
    public void autoActivatePromotions() {
        List<Promotion> allPromotions = promotionRepository.findAll();
        int activatedCount = 0;

        for (Promotion promotion : allPromotions) {
            if (promotion.shouldAutoActivate()) {
                try {
                    Event event = promotionDomainService.goOnline(promotion, SYSTEM_OPERATOR);
                    promotionRepository.update(promotion);
                    publishAndRecord(event);
                    activatedCount++;
                } catch (Exception e) {
                    // 单个活动激活失败不影响其他活动
                    System.err.println("Failed to auto-activate promotion "
                        + promotion.getPromotionId() + ": " + e.getMessage());
                }
            }
        }

        if (activatedCount > 0) {
            System.out.println("Auto-activated " + activatedCount + " promotion(s)");
        }
    }

    /**
     * 自动过期定时任务
     * 按配置间隔扫描所有生效中活动，将已过结束时间的活动自动标记为过期
     * 配置项: promotion.scheduled.auto-expire-rate (默认 60000ms)
     */
    @Scheduled(fixedRateString = "${promotion.scheduled.auto-expire-rate:60000}")
    public void autoExpirePromotions() {
        List<Promotion> allPromotions = promotionRepository.findAll();
        int expiredCount = 0;

        for (Promotion promotion : allPromotions) {
            if (promotion.isExpired()) {
                try {
                    Event event = promotionDomainService.expire(promotion);
                    promotionRepository.update(promotion);
                    publishAndRecord(event);
                    expiredCount++;
                } catch (Exception e) {
                    System.err.println("Failed to auto-expire promotion "
                        + promotion.getPromotionId() + ": " + e.getMessage());
                }
            }
        }

        if (expiredCount > 0) {
            System.out.println("Auto-expired " + expiredCount + " promotion(s)");
        }
    }

    /**
     * 手动触发自动生效检查（用于测试和管理员手动操作）
     *
     * @return 激活的活动数量
     */
    public int manualActivateCheck() {
        List<Promotion> allPromotions = promotionRepository.findAll();
        int count = 0;

        for (Promotion promotion : allPromotions) {
            if (promotion.shouldAutoActivate()) {
                try {
                    Event event = promotionDomainService.goOnline(promotion, SYSTEM_OPERATOR);
                    promotionRepository.update(promotion);
                    publishAndRecord(event);
                    count++;
                } catch (Exception e) {
                    System.err.println("Failed to activate: " + promotion.getPromotionId());
                }
            }
        }
        return count;
    }

    /**
     * 手动触发自动过期检查（用于测试和管理员手动操作）
     *
     * @return 过期的活动数量
     */
    public int manualExpireCheck() {
        List<Promotion> allPromotions = promotionRepository.findAll();
        int count = 0;

        for (Promotion promotion : allPromotions) {
            if (promotion.isExpired()) {
                try {
                    Event event = promotionDomainService.expire(promotion);
                    promotionRepository.update(promotion);
                    publishAndRecord(event);
                    count++;
                } catch (Exception e) {
                    System.err.println("Failed to expire: " + promotion.getPromotionId());
                }
            }
        }
        return count;
    }

    // ========== 内部辅助方法 ==========

    private void publishAndRecord(Event event) {
        eventBusService.publish(event);
        eventLogService.record(event);
    }
}
