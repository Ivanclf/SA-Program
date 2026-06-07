package com.sa.promotion.domain.event.service;

import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.entity.EventLog;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.event.repository.EventLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("事件日志服务测试")
class EventLogServiceTest {

    private EventLogService eventLogService;

    @Mock
    private EventLogRepository eventLogRepository;

    private final Map<String, EventLog> mockStore = new HashMap<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventLogService = new EventLogService(eventLogRepository);
        mockStore.clear();

        // save
        doAnswer(inv -> {
            EventLog log = inv.getArgument(0);
            mockStore.put(log.getEventId(), log);
            return null;
        }).when(eventLogRepository).save(any(EventLog.class));

        // findById
        when(eventLogRepository.findById(anyString()))
            .thenAnswer(inv -> Optional.ofNullable(mockStore.get(inv.getArgument(0).toString())));

        // findByPromotionId
        when(eventLogRepository.findByPromotionId(anyString())).thenAnswer(inv -> {
            List<EventLog> result = new ArrayList<>();
            for (EventLog log : mockStore.values()) {
                if (inv.getArgument(0).toString().equals(log.getPromotionId())) {
                    result.add(log);
                }
            }
            return result;
        });

        // findByEventType
        when(eventLogRepository.findByEventType(anyString())).thenAnswer(inv -> {
            List<EventLog> result = new ArrayList<>();
            for (EventLog log : mockStore.values()) {
                if (inv.getArgument(0).toString().equals(log.getEventType().getCode())) {
                    result.add(log);
                }
            }
            return result;
        });

        // findAll
        when(eventLogRepository.findAll()).thenAnswer(inv -> new ArrayList<>(mockStore.values()));

        // deleteAll
        doAnswer(inv -> { mockStore.clear(); return null; }).when(eventLogRepository).deleteAll();
    }

    @Test
    @DisplayName("record - 成功记录事件日志")
    void testRecordValid() {
        Event event = createEvent(EventType.E_CREATE_DRAFT);
        EventLog log = eventLogService.record(event);

        assertNotNull(log);
        assertEquals(event.getEventId(), log.getEventId());
    }

    @Test
    @DisplayName("record - null事件抛出异常")
    void testRecordNullEvent() {
        assertThrows(IllegalArgumentException.class, () -> eventLogService.record(null));
    }

    @Test
    @DisplayName("queryByEventId - 找到")
    void testQueryByEventIdFound() {
        Event event = createEvent(EventType.E_CREATE_DRAFT);
        eventLogService.record(event);
        assertNotNull(eventLogService.queryByEventId(event.getEventId()));
    }

    @Test
    @DisplayName("queryByEventId - 未找到返回null")
    void testQueryByEventIdNotFound() {
        assertNull(eventLogService.queryByEventId("non-existent"));
    }

    @Test
    @DisplayName("queryByPromotionId - 返回列表")
    void testQueryByPromotionId() {
        eventLogService.record(createEvent(EventType.E_CREATE_DRAFT));
        eventLogService.record(createEvent(EventType.E_SUBMIT_AUDIT));
        assertEquals(2, eventLogService.queryByPromotionId("test-promotion-id").size());
    }

    @Test
    @DisplayName("queryByEventType - 按类型查询")
    void testQueryByEventType() {
        eventLogService.record(createEvent(EventType.E_CREATE_DRAFT));
        eventLogService.record(createEvent(EventType.E_CREATE_DRAFT));
        eventLogService.record(createEvent(EventType.E_SUBMIT_AUDIT));
        assertEquals(2, eventLogService.queryByEventType("E_CREATE_DRAFT").size());
    }

    @Test
    @DisplayName("getAllEventLogs - 返回全部")
    void testGetAllEventLogs() {
        eventLogService.record(createEvent(EventType.E_CREATE_DRAFT));
        assertEquals(1, eventLogService.getAllEventLogs().size());
    }

    @Test
    @DisplayName("replay - 成功回放")
    void testReplay() {
        Event event = createEvent(EventType.E_CREATE_DRAFT);
        eventLogService.record(event);
        Event replayed = eventLogService.replay(event.getEventId());
        assertNotNull(replayed);
        assertEquals(event.getEventType(), replayed.getEventType());
    }

    @Test
    @DisplayName("replay - 不存在抛出异常")
    void testReplayNotFound() {
        assertThrows(IllegalArgumentException.class,
            () -> eventLogService.replay("non-existent"));
    }

    @Test
    @DisplayName("replayAllForPromotion - 回放全部")
    void testReplayAllForPromotion() {
        eventLogService.record(createEvent(EventType.E_CREATE_DRAFT));
        eventLogService.record(createEvent(EventType.E_SUBMIT_AUDIT));
        List<Event> events = eventLogService.replayAllForPromotion("test-promotion-id");
        assertEquals(2, events.size());
    }

    private Event createEvent(EventType eventType) {
        return Event.builder()
            .eventId("test-event-id-" + UUID.randomUUID().toString().substring(0, 8))
            .eventType(eventType)
            .promotionId("test-promotion-id")
            .operator("test-user")
            .eventTime(LocalDateTime.now())
            .build();
    }
}
