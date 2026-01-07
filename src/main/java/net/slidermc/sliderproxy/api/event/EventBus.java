package net.slidermc.sliderproxy.api.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件总线 - 负责事件的注册、分发和处理
 * 支持基于注解的监听器注册
 */
public class EventBus {
    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    // 事件监听器映射：事件类 -> 优先级分组 -> 监听器列表
    private final Map<Class<? extends Event>, Map<EventPriority, List<RegisteredListener>>> listeners;

    // 已注册的监听器对象集合，用于防止重复注册
    private final Set<Object> registeredHandlers;

    public EventBus() {
        this.listeners = new ConcurrentHashMap<>();
        this.registeredHandlers = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * 注册监听器对象
     * 自动扫描对象中所有带有 @EventListener 注解的方法
     *
     * @param listener 监听器对象
     */
    public void register(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        // 防止重复注册
        if (registeredHandlers.contains(listener)) {
            log.warn("Listener {} is already registered", listener.getClass().getName());
            return;
        }

        Class<?> clazz = listener.getClass();
        Method[] methods = clazz.getDeclaredMethods();

        boolean hasListener = false;

        for (Method method : methods) {
            EventListener annotation = method.getAnnotation(EventListener.class);
            if (annotation != null) {
                validateListenerMethod(method);

                Class<? extends Event> eventType = getEventType(method);
                EventPriority priority = annotation.priority();
                boolean ignoreCancelled = annotation.ignoreCancelled();

                RegisteredListener registeredListener = new RegisteredListener(
                    listener, method, priority, ignoreCancelled
                );

                listeners.computeIfAbsent(eventType, k -> new EnumMap<>(EventPriority.class))
                         .computeIfAbsent(priority, k -> new CopyOnWriteArrayList<>())
                         .add(registeredListener);

                hasListener = true;
                log.debug("Registered event listener: {}#{} for event {} with priority {}",
                    clazz.getSimpleName(), method.getName(), eventType.getSimpleName(), priority);
            }
        }

        if (hasListener) {
            registeredHandlers.add(listener);
            log.info("Registered event listener class: {}", clazz.getName());
        } else {
            log.warn("No @EventListener methods found in {}", clazz.getName());
        }
    }

    /**
     * 注销监听器对象
     * 移除该对象的所有监听器方法
     *
     * @param listener 监听器对象
     */
    public void unregister(Object listener) {
        if (listener == null || !registeredHandlers.contains(listener)) {
            return;
        }

        // 移除所有相关的监听器
        listeners.values().forEach(priorityMap -> {
            priorityMap.values().forEach(list -> {
                list.removeIf(rl -> rl.getListener() == listener);
            });
        });

        registeredHandlers.remove(listener);
        log.info("Unregistered event listener class: {}", listener.getClass().getName());
    }

    /**
     * 发布事件
     * 按优先级顺序调用所有匹配的监听器
     *
     * @param event 要发布的事件
     */
    public void callEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        Class<? extends Event> eventClass = event.getClass();
        Map<EventPriority, List<RegisteredListener>> priorityMap = listeners.get(eventClass);

        if (priorityMap == null || priorityMap.isEmpty()) {
            return;
        }

        // 按优先级顺序执行（从高到低）
        for (EventPriority priority : EventPriority.values()) {
            List<RegisteredListener> priorityListeners = priorityMap.get(priority);
            if (priorityListeners == null || priorityListeners.isEmpty()) {
                continue;
            }

            for (RegisteredListener listener : priorityListeners) {
                try {
                    // 检查是否忽略已取消的事件
                    if (event.isCancelled() && listener.isIgnoreCancelled()) {
                        continue;
                    }

                    listener.invoke(event);
                } catch (Exception e) {
                    log.error("Error calling event listener: {}#{}",
                        listener.getListener().getClass().getName(),
                        listener.getMethod().getName(), e);
                }
            }
        }
    }

    /**
     * 获取指定事件类型的所有监听器数量
     *
     * @param eventClass 事件类
     * @return 监听器数量
     */
    public int getListenerCount(Class<? extends Event> eventClass) {
        Map<EventPriority, List<RegisteredListener>> priorityMap = listeners.get(eventClass);
        if (priorityMap == null) {
            return 0;
        }
        return priorityMap.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    /**
     * 清空所有监听器
     */
    public void clear() {
        listeners.clear();
        registeredHandlers.clear();
        log.info("EventBus cleared");
    }

    /**
     * 获取所有已注册的监听器类
     */
    public Set<Object> getRegisteredListeners() {
        return Collections.unmodifiableSet(registeredHandlers);
    }

    // 私有辅助方法

    private void validateListenerMethod(Method method) {
        // 使用 trySetAccessible 替代 canAccess + setAccessible，避免某些情况下的访问问题
        if (!method.trySetAccessible()) {
            throw new IllegalArgumentException(
                "Cannot access event listener method: " + method);
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new IllegalArgumentException(
                "Event listener method must have exactly one parameter: " + method);
        }

        if (!Event.class.isAssignableFrom(parameterTypes[0])) {
            throw new IllegalArgumentException(
                "Event listener method parameter must be an Event subclass: " + method);
        }
    }

    private Class<? extends Event> getEventType(Method method) {
        @SuppressWarnings("unchecked")
        Class<? extends Event> eventType = (Class<? extends Event>) method.getParameterTypes()[0];
        return eventType;
    }

    /**
     * 内部类：注册的监听器包装器
     */
    private static class RegisteredListener {
        private final Object listener;
        private final Method method;
        private final EventPriority priority;
        private final boolean ignoreCancelled;

        public RegisteredListener(Object listener, Method method,
                                 EventPriority priority, boolean ignoreCancelled) {
            this.listener = listener;
            this.method = method;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
        }

        public Object getListener() {
            return listener;
        }

        public Method getMethod() {
            return method;
        }

        public boolean isIgnoreCancelled() {
            return ignoreCancelled;
        }

        public void invoke(Event event) throws Exception {
            method.invoke(listener, event);
        }
    }
}
