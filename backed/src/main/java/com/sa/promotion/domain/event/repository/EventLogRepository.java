package com.sa.promotion.domain.event.repository;

import com.sa.promotion.domain.event.entity.EventLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 事件日志仓储接口 (MyBatis Mapper)
 */
@Mapper
public interface EventLogRepository {
    void save(EventLog eventLog);
    Optional<EventLog> findById(String eventId);
    List<EventLog> findByPromotionId(String promotionId);
    List<EventLog> findByEventType(String eventTypeCode);
    List<EventLog> findAll();
    void deleteAll();
}
