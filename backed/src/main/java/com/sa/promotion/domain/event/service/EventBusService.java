package com.sa.promotion.domain.event.service;

import com.sa.promotion.domain.event.entity.Event;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件总线服务 - 事件生产与分发
 *
 * DDD设计说明：
 * - 作为事件驱动架构的核心基础设施
 * - 负责事件的生产、分发和路由
 * - 解耦事件生产者和消费者
 *
 * 事件驱动架构：
 * - 领域服务产生Event对象后，通过EventBusService发布
 * - EventBusService负责将事件路由到正确的消费者
 * - 支持同步事件处理，后续可扩展为异步消息队列（如RabbitMQ、Kafka）
 *
 * 当前实现：
 * - 基于内存的同步事件总线
 * - 支持按事件类型订阅和处理
 * - 订阅者以EventType.code为key注册
 */
@Service
public class EventBusService {

    /**
     * 事件处理器注册表
     * Key: 事件类型代码（如 "E_CREATE_DRAFT"）
     * Value: 注册的处理器列表（支持一个事件类型多个处理器）
     */
    private final Map<String, List<EventHandler>> handlers = new ConcurrentHashMap<>();

    /**
     * 发布事件
     * 将事件分发给所有注册的对应类型处理器
     *
     * @param event 事件对象
     */
    public void publish(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        // 1. 路由事件到对应的处理器
        route(event);

        // 2. 记录日志（实际项目中使用Logger）
        System.out.println("Event published: " + event.getEventType().getCode()
            + " for promotion: " + event.getPromotionId());
    }

    /**
     * 路由事件到对应的处理器
     * 根据事件类型查找注册的处理器并依次调用
     *
     * @param event 事件对象
     */
    public void route(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        String eventTypeCode = event.getEventType().getCode();
        List<EventHandler> eventHandlers = handlers.get(eventTypeCode);

        if (eventHandlers != null && !eventHandlers.isEmpty()) {
            for (EventHandler handler : eventHandlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    // 单个处理器异常不影响其他处理器
                    System.err.println("Error handling event " + eventTypeCode + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * 订阅事件
     * 注册事件处理器，当对应类型的事件发布时会被调用
     *
     * @param eventType 事件类型代码（如 "E_CREATE_DRAFT"）
     * @param handler 事件处理器
     */
    public void subscribe(String eventType, EventHandler handler) {
        if (eventType == null || eventType.isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Event handler cannot be null");
        }

        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    /**
     * 取消订阅
     *
     * @param eventType 事件类型代码
     * @param handler 要移除的处理器
     */
    public void unsubscribe(String eventType, EventHandler handler) {
        if (eventType == null || handler == null) {
            return;
        }

        List<EventHandler> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            if (eventHandlers.isEmpty()) {
                handlers.remove(eventType);
            }
        }
    }

    /**
     * 获取注册的处理器数量（用于测试验证）
     *
     * @param eventType 事件类型代码
     * @return 处理器数量
     */
    public int getHandlerCount(String eventType) {
        List<EventHandler> eventHandlers = handlers.get(eventType);
        return eventHandlers != null ? eventHandlers.size() : 0;
    }

    /**
     * 清空所有注册的处理器（仅用于测试）
     */
    public void clearAll() {
        handlers.clear();
    }

    /**
     * 事件处理器接口
     */
    @FunctionalInterface
    public interface EventHandler {
        void handle(Event event);
    }
}
