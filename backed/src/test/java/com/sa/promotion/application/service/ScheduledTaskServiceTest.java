package com.sa.promotion.application.service;

import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.event.service.EventBusService;
import com.sa.promotion.domain.event.service.EventLogService;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import com.sa.promotion.domain.promotion.repository.PromotionRepository;
import com.sa.promotion.domain.promotion.service.PromotionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("定时任务服务测试")
class ScheduledTaskServiceTest {

    private ScheduledTaskService scheduledTaskService;

    @Mock private PromotionDomainService promotionDomainService;
    @Mock private PromotionRepository promotionRepository;
    @Mock private EventBusService eventBusService;
    @Mock private EventLogService eventLogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduledTaskService = new ScheduledTaskService(
            promotionDomainService, promotionRepository, eventBusService, eventLogService);
    }

    // ========== auto-activate tests ==========

    @Test
    @DisplayName("autoActivatePromotions - 应该激活的活动被激活")
    void testAutoActivatePromotions() {
        Promotion p = Promotion.builder()
            .promotionId("promo-001").name("待激活活动")
            .status(PromotionStatus.INIT).auditStatus(AuditStatus.PASSED)
            .stime(LocalDateTime.now().minusHours(1))  // 开始时间已过
            .etime(LocalDateTime.now().plusDays(7))
            .creator("u001").ctime(LocalDateTime.now())
            .build();

        when(promotionRepository.findAll()).thenReturn(List.of(p));
        when(promotionDomainService.goOnline(any(), anyString())).thenReturn(
            Event.builder().eventId("evt-001").eventType(EventType.E_ACTIVE_ONLINE)
                .promotionId("promo-001").build());

        scheduledTaskService.autoActivatePromotions();

        verify(promotionDomainService).goOnline(any(Promotion.class), eq("SYSTEM"));
        verify(promotionRepository).update(any(Promotion.class));
        verify(eventBusService).publish(any(Event.class));
        verify(eventLogService).record(any(Event.class));
    }

    @Test
    @DisplayName("autoActivatePromotions - 未到开始时间不激活")
    void testAutoActivateNotYet() {
        Promotion p = Promotion.builder()
            .promotionId("promo-002").name("未到时间的活动")
            .status(PromotionStatus.INIT).auditStatus(AuditStatus.PASSED)
            .stime(LocalDateTime.now().plusDays(1))  // 明天才开始
            .etime(LocalDateTime.now().plusDays(7))
            .creator("u001").ctime(LocalDateTime.now())
            .build();

        when(promotionRepository.findAll()).thenReturn(List.of(p));

        scheduledTaskService.autoActivatePromotions();

        verify(promotionDomainService, never()).goOnline(any(), anyString());
    }

    @Test
    @DisplayName("autoActivatePromotions - 非INIT状态不激活")
    void testAutoActivateWrongStatus() {
        Promotion p = Promotion.builder()
            .promotionId("promo-003").name("草稿活动")
            .status(PromotionStatus.DRAFT).auditStatus(AuditStatus.WAITING)
            .stime(LocalDateTime.now().minusHours(1))
            .etime(LocalDateTime.now().plusDays(7))
            .creator("u001").ctime(LocalDateTime.now())
            .build();

        when(promotionRepository.findAll()).thenReturn(List.of(p));

        scheduledTaskService.autoActivatePromotions();

        verify(promotionDomainService, never()).goOnline(any(), anyString());
    }

    @Test
    @DisplayName("autoActivatePromotions - 空列表不报错")
    void testAutoActivateEmptyList() {
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> scheduledTaskService.autoActivatePromotions());
        verify(promotionDomainService, never()).goOnline(any(), anyString());
    }

    // ========== auto-expire tests ==========

    @Test
    @DisplayName("autoExpirePromotions - 已过期的活动被标记为过期")
    void testAutoExpirePromotions() {
        Promotion p = Promotion.builder()
            .promotionId("promo-004").name("已过期活动")
            .status(PromotionStatus.ONLINE).auditStatus(AuditStatus.PASSED)
            .stime(LocalDateTime.now().minusDays(7))
            .etime(LocalDateTime.now().minusHours(1))  // 结束时间已过
            .creator("u001").ctime(LocalDateTime.now())
            .build();

        when(promotionRepository.findAll()).thenReturn(List.of(p));
        when(promotionDomainService.expire(any())).thenReturn(
            Event.builder().eventId("evt-002").eventType(EventType.E_ACTIVE_EXPIRE)
                .promotionId("promo-004").build());

        scheduledTaskService.autoExpirePromotions();

        verify(promotionDomainService).expire(any(Promotion.class));
        verify(promotionRepository).update(any(Promotion.class));
        verify(eventBusService).publish(any(Event.class));
    }

    @Test
    @DisplayName("autoExpirePromotions - 未过期的不处理")
    void testAutoExpireNotYet() {
        Promotion p = Promotion.builder()
            .promotionId("promo-005").name("进行中活动")
            .status(PromotionStatus.ONLINE).auditStatus(AuditStatus.PASSED)
            .stime(LocalDateTime.now().minusDays(1))
            .etime(LocalDateTime.now().plusDays(7))  // 还没结束
            .creator("u001").ctime(LocalDateTime.now())
            .build();

        when(promotionRepository.findAll()).thenReturn(List.of(p));

        scheduledTaskService.autoExpirePromotions();

        verify(promotionDomainService, never()).expire(any());
    }

    @Test
    @DisplayName("autoExpirePromotions - 非ONLINE状态不处理")
    void testAutoExpireWrongStatus() {
        Promotion p = Promotion.builder()
            .promotionId("promo-006").name("已下线活动")
            .status(PromotionStatus.OFFLINE).auditStatus(AuditStatus.CANCELLED)
            .stime(LocalDateTime.now().minusDays(7))
            .etime(LocalDateTime.now().minusHours(1))
            .creator("u001").ctime(LocalDateTime.now())
            .build();

        when(promotionRepository.findAll()).thenReturn(List.of(p));

        scheduledTaskService.autoExpirePromotions();

        verify(promotionDomainService, never()).expire(any());
    }

    // ========== manual check tests ==========

    @Test
    @DisplayName("manualActivateCheck - 返回激活数量")
    void testManualActivateCheck() {
        Promotion p1 = Promotion.builder()
            .promotionId("promo-001").status(PromotionStatus.INIT).auditStatus(AuditStatus.PASSED)
            .stime(LocalDateTime.now().minusHours(1)).etime(LocalDateTime.now().plusDays(7))
            .creator("u001").ctime(LocalDateTime.now()).build();
        Promotion p2 = Promotion.builder()
            .promotionId("promo-002").status(PromotionStatus.DRAFT).auditStatus(AuditStatus.WAITING)
            .stime(LocalDateTime.now().minusHours(1)).etime(LocalDateTime.now().plusDays(7))
            .creator("u001").ctime(LocalDateTime.now()).build();

        when(promotionRepository.findAll()).thenReturn(Arrays.asList(p1, p2));
        when(promotionDomainService.goOnline(any(), anyString())).thenReturn(
            Event.builder().eventId("e1").eventType(EventType.E_ACTIVE_ONLINE).build());

        int count = scheduledTaskService.manualActivateCheck();

        assertEquals(1, count);
    }

    @Test
    @DisplayName("manualExpireCheck - 返回过期数量")
    void testManualExpireCheck() {
        Promotion p1 = Promotion.builder()
            .promotionId("promo-001").status(PromotionStatus.ONLINE).auditStatus(AuditStatus.PASSED)
            .stime(LocalDateTime.now().minusDays(7)).etime(LocalDateTime.now().minusHours(1))
            .creator("u001").ctime(LocalDateTime.now()).build();
        Promotion p2 = Promotion.builder()
            .promotionId("promo-002").status(PromotionStatus.ONLINE).auditStatus(AuditStatus.PASSED)
            .stime(LocalDateTime.now().minusDays(1)).etime(LocalDateTime.now().plusDays(7))
            .creator("u001").ctime(LocalDateTime.now()).build();

        when(promotionRepository.findAll()).thenReturn(Arrays.asList(p1, p2));
        when(promotionDomainService.expire(any())).thenReturn(
            Event.builder().eventId("e2").eventType(EventType.E_ACTIVE_EXPIRE).build());

        int count = scheduledTaskService.manualExpireCheck();

        assertEquals(1, count);
    }
}
