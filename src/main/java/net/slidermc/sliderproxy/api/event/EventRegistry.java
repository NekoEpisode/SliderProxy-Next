package net.slidermc.sliderproxy.api.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 事件注册器 - 提供全局的事件管理功能
 * 单例模式，整个代理服务器共享一个事件总线
 */
public class EventRegistry {
    private static final Logger log = LoggerFactory.getLogger(EventRegistry.class);
    private static volatile EventRegistry instance;

    private final EventBus eventBus;

    private EventRegistry() {
        this.eventBus = new EventBus();
        log.info("EventRegistry initialized");
    }

    /**
     * 获取单例实例
     *
     * @return EventRegistry 实例
     */
    public static EventRegistry getInstance() {
        if (instance == null) {
            synchronized (EventRegistry.class) {
                if (instance == null) {
                    instance = new EventRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * 注册事件监听器
     * 自动扫描对象中所有带有 @EventListener 注解的方法
     *
     * @param listener 监听器对象
     */
    public static void registerListener(Object listener) {
        getInstance().eventBus.register(listener);
    }

    /**
     * 注销事件监听器
     *
     * @param listener 监听器对象
     */
    public static void unregisterListener(Object listener) {
        getInstance().eventBus.unregister(listener);
    }

    /**
     * 发布事件
     *
     * @param event 要发布的事件
     */
    public static void callEvent(Event event) {
        getInstance().eventBus.callEvent(event);
    }

    /**
     * 获取事件总线
     *
     * @return 事件总线实例
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * 获取指定事件的监听器数量
     *
     * @param eventClass 事件类
     * @return 监听器数量
     */
    public static int getListenerCount(Class<? extends Event> eventClass) {
        return getInstance().eventBus.getListenerCount(eventClass);
    }

    /**
     * 清空所有监听器（主要用于测试）
     */
    public static void clear() {
        getInstance().eventBus.clear();
    }
}

