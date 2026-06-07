package com.sa.promotion.domain.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.entity.EventLog;
import com.sa.promotion.domain.event.repository.EventLogRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件日志服务 - 事件持久化和管理
 *
 * 数据访问：通过注入的 EventLogRepository（MyBatis Mapper 代理）操作 event_log 表
 */
@Service
public class EventLogService {

    private final EventLogRepository eventLogRepository;
    private final ObjectMapper objectMapper;

    public EventLogService(EventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 记录事件日志
     */
    public EventLog record(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        String paramsJson = convertParamsToJson(event.getParams());
        EventLog eventLog = EventLog.fromEvent(event, paramsJson);

        eventLogRepository.save(eventLog);
        return eventLog;
    }

    public EventLog queryByEventId(String eventId) {
        return eventLogRepository.findById(eventId).orElse(null);
    }

    public List<EventLog> queryByPromotionId(String promotionId) {
        return eventLogRepository.findByPromotionId(promotionId);
    }

    public List<EventLog> queryByEventType(String eventTypeCode) {
        return eventLogRepository.findByEventType(eventTypeCode);
    }

    public List<EventLog> getAllEventLogs() {
        return eventLogRepository.findAll();
    }

    /**
     * 回放单个事件
     */
    public Event replay(String eventId) {
        EventLog eventLog = eventLogRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        return rebuildEvent(eventLog);
    }

    /**
     * 回放某活动的全部事件（按时间顺序）
     */
    public List<Event> replayAllForPromotion(String promotionId) {
        List<EventLog> logs = eventLogRepository.findByPromotionId(promotionId);
        logs.sort((l1, l2) -> l1.getEventTime().compareTo(l2.getEventTime()));

        List<Event> events = new ArrayList<>();
        for (EventLog log : logs) {
            events.add(rebuildEvent(log));
        }
        return events;
    }

    private Event rebuildEvent(EventLog log) {
        Map<String, Object> params = new HashMap<>();
        if (log.getParams() != null && !log.getParams().isEmpty()) {
            try {
                params = objectMapper.readValue(log.getParams(),
                    new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                // 解析失败时使用空 Map，不影响主流程
                System.err.println("Failed to parse event params: " + e.getMessage());
            }
        }

        return Event.builder()
            .eventId(log.getEventId())
            .eventType(log.getEventType())
            .promotionId(log.getPromotionId())
            .prevActivityStatus(log.getPrevActivityStatus())
            .prevAuditStatus(log.getPrevAuditStatus())
            .operator(log.getOperator())
            .eventTime(log.getEventTime())
            .params(params)
            .build();
    }

    private String convertParamsToJson(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize event params: " + e.getMessage());
            return "{}";
        }
    }
}
