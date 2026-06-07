package com.sa.promotion.domain.event.service;

import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.enums.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事件总线服务单元测试
 *
 * 测试覆盖：
 * 1. publish - 发布事件并分发给订阅者
 * 2. route - 路由事件到处理器
 * 3. subscribe - 订阅事件
 * 4. unsubscribe - 取消订阅
 * 5. 异常处理和边界条件
 */
@DisplayName("事件总线服务测试")
class EventBusServiceTest {

    private EventBusService eventBusService;

    @BeforeEach
    void setUp() {
        eventBusService = new EventBusService();
    }

    // ========== publish测试 ==========

    @Test
    @DisplayName("publish - 成功发布事件")
    void testPublishValid() {
        Event event = createEvent(EventType.E_CREATE_DRAFT);

        assertDoesNotThrow(() -> {
            eventBusService.publish(event);
        });
    }

    @Test
    @DisplayName("publish - null事件抛出异常")
    void testPublishNullEvent() {
        assertThrows(IllegalArgumentException.class, () -> {
            eventBusService.publish(null);
        });
    }

    @Test
    @DisplayName("publish - 发布事件时已注册的处理器被调用")
    void testPublishCallsSubscribedHandler() {
        final boolean[] called = {false};
        final Event[] receivedEvent = {null};

        EventBusService.EventHandler handler = event -> {
            called[0] = true;
            receivedEvent[0] = event;
        };

        eventBusService.subscribe(EventType.E_CREATE_DRAFT.getCode(), handler);
        Event event = createEvent(EventType.E_CREATE_DRAFT);
        eventBusService.publish(event);

        assertTrue(called[0], "Handler should be called when event is published");
        assertNotNull(receivedEvent[0]);
        assertEquals(event.getEventId(), receivedEvent[0].getEventId());
    }

    @Test
    @DisplayName("publish - 发布事件时不匹配类型的处理器不被调用")
    void testPublishDoesNotCallDifferentTypeHandler() {
        final boolean[] called = {false};

        EventBusService.EventHandler handler = event -> called[0] = true;
        eventBusService.subscribe(EventType.E_SUBMIT_AUDIT.getCode(), handler);

        Event event = createEvent(EventType.E_CREATE_DRAFT);
        eventBusService.publish(event);

        assertFalse(called[0], "Handler for different event type should not be called");
    }

    @Test
    @DisplayName("publish - 多个处理器都被调用")
    void testPublishCallsMultipleHandlers() {
        final int[] callCount = {0};

        EventBusService.EventHandler handler1 = event -> callCount[0]++;
        EventBusService.EventHandler handler2 = event -> callCount[0]++;

        eventBusService.subscribe(EventType.E_CREATE_DRAFT.getCode(), handler1);
        eventBusService.subscribe(EventType.E_CREATE_DRAFT.getCode(), handler2);

        Event event = createEvent(EventType.E_CREATE_DRAFT);
        eventBusService.publish(event);

        assertEquals(2, callCount[0]);
    }

    // ========== route测试 ==========

    @Test
    @DisplayName("route - 成功路由事件到对应处理器")
    void testRouteValid() {
        Event event = createEvent(EventType.E_SUBMIT_AUDIT);

        assertDoesNotThrow(() -> {
            eventBusService.route(event);
        });
    }

    @Test
    @DisplayName("route - null事件抛出异常")
    void testRouteNullEvent() {
        assertThrows(IllegalArgumentException.class, () -> {
            eventBusService.route(null);
        });
    }

    // ========== subscribe测试 ==========

    @Test
    @DisplayName("subscribe - 成功订阅事件")
    void testSubscribeValid() {
        EventBusService.EventHandler handler = event -> {};

        assertDoesNotThrow(() -> {
            eventBusService.subscribe(EventType.E_CREATE_DRAFT.getCode(), handler);
        });

        assertEquals(1, eventBusService.getHandlerCount(EventType.E_CREATE_DRAFT.getCode()));
    }

    @Test
    @DisplayName("subscribe - null事件类型抛出异常")
    void testSubscribeNullEventType() {
        EventBusService.EventHandler handler = event -> {};

        assertThrows(IllegalArgumentException.class, () -> {
            eventBusService.subscribe(null, handler);
        });
    }

    @Test
    @DisplayName("subscribe - null处理器抛出异常")
    void testSubscribeNullHandler() {
        assertThrows(IllegalArgumentException.class, () -> {
            eventBusService.subscribe(EventType.E_CREATE_DRAFT.getCode(), null);
        });
    }

    // ========== unsubscribe测试 ==========

    @Test
    @DisplayName("unsubscribe - 成功取消订阅")
    void testUnsubscribeValid() {
        EventBusService.EventHandler handler = event -> {};
        eventBusService.subscribe(EventType.E_CREATE_DRAFT.getCode(), handler);

        eventBusService.unsubscribe(EventType.E_CREATE_DRAFT.getCode(), handler);

        assertEquals(0, eventBusService.getHandlerCount(EventType.E_CREATE_DRAFT.getCode()));
    }

    @Test
    @DisplayName("unsubscribe - 取消订阅后处理器不再被调用")
    void testUnsubscribeStopsHandler() {
        final boolean[] called = {false};
        EventBusService.EventHandler handler = event -> called[0] = true;

        eventBusService.subscribe(EventType.E_CREATE_DRAFT.getCode(), handler);
        eventBusService.unsubscribe(EventType.E_CREATE_DRAFT.getCode(), handler);

        Event event = createEvent(EventType.E_CREATE_DRAFT);
        eventBusService.publish(event);

        assertFalse(called[0]);
    }

    @Test
    @DisplayName("unsubscribe - null参数不抛出异常")
    void testUnsubscribeNullParams() {
        assertDoesNotThrow(() -> {
            eventBusService.unsubscribe(null, null);
        });
    }

    // ========== clearAll测试 ==========

    @Test
    @DisplayName("clearAll - 清空所有处理器")
    void testClearAll() {
        eventBusService.subscribe(EventType.E_CREATE_DRAFT.getCode(), event -> {});
        eventBusService.subscribe(EventType.E_SUBMIT_AUDIT.getCode(), event -> {});

        eventBusService.clearAll();

        assertEquals(0, eventBusService.getHandlerCount(EventType.E_CREATE_DRAFT.getCode()));
        assertEquals(0, eventBusService.getHandlerCount(EventType.E_SUBMIT_AUDIT.getCode()));
    }

    // ========== 辅助方法 ==========

    private Event createEvent(EventType eventType) {
        return Event.builder()
            .eventId("test-event-id")
            .eventType(eventType)
            .promotionId("test-promotion-id")
            .operator("test-user")
            .eventTime(LocalDateTime.now())
            .build();
    }
}
