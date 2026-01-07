package net.slidermc.sliderproxy.api.event;

import net.slidermc.sliderproxy.api.plugin.SliderProxyPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 插件事件注册器
 * 为插件提供带生命周期管理的事件注册功能
 * 插件卸载时会自动清理注册的所有监听器
 */
public class PluginEventRegistry {
    private static final Logger log = LoggerFactory.getLogger(PluginEventRegistry.class);

    // 插件 -> 该插件注册的监听器列表
    private static final Map<SliderProxyPlugin, Set<Object>> pluginListeners = new WeakHashMap<>();

    /**
     * 为插件注册事件监听器
     * 插件卸载时会自动清理这些监听器
     *
     * @param plugin 插件实例
     * @param listener 监听器对象（自动扫描 @EventListener 方法）
     */
    public static void registerListener(SliderProxyPlugin plugin, Object listener) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        // 注册到全局事件总线
        EventRegistry.registerListener(listener);

        // 记录该插件注册的监听器
        pluginListeners.computeIfAbsent(plugin, k -> new HashSet<>()).add(listener);

        log.debug("插件 {} 注册了事件监听器: {}",
            plugin.getClass().getSimpleName(),
            listener.getClass().getSimpleName());
    }

    /**
     * 为插件注册多个监听器
     *
     * @param plugin 插件实例
     * @param listeners 监听器对象列表
     */
    public static void registerListeners(SliderProxyPlugin plugin, Object... listeners) {
        for (Object listener : listeners) {
            registerListener(plugin, listener);
        }
    }

    /**
     * 注销插件的所有事件监听器
     * 通常在插件 onDisable() 时自动调用
     *
     * @param plugin 插件实例
     */
    public static void unregisterAllListeners(SliderProxyPlugin plugin) {
        Set<Object> listeners = pluginListeners.remove(plugin);
        if (listeners != null) {
            for (Object listener : listeners) {
                EventRegistry.unregisterListener(listener);
            }
            log.info("插件 {} 的 {} 个事件监听器已注销",
                plugin.getClass().getSimpleName(),
                listeners.size());
        }
    }

    /**
     * 获取插件注册的监听器数量
     *
     * @param plugin 插件实例
     * @return 监听器数量
     */
    public static int getPluginListenerCount(SliderProxyPlugin plugin) {
        Set<Object> listeners = pluginListeners.get(plugin);
        return listeners != null ? listeners.size() : 0;
    }

    /**
     * 检查插件是否注册了监听器
     *
     * @param plugin 插件实例
     * @return 是否注册过监听器
     */
    public static boolean hasRegisteredListeners(SliderProxyPlugin plugin) {
        Set<Object> listeners = pluginListeners.get(plugin);
        return listeners != null && !listeners.isEmpty();
    }

    /**
     * 获取所有插件的监听器统计信息
     *
     * @return 插件名称 -> 监听器数量 的映射
     */
    public static Map<String, Integer> getPluginListenerStats() {
        Map<String, Integer> stats = new HashMap<>();
        pluginListeners.forEach((plugin, listeners) -> {
            stats.put(plugin.getClass().getSimpleName(), listeners.size());
        });
        return stats;
    }

    /**
     * 清空所有插件的监听器记录（主要用于测试）
     */
    public static void clearPluginListeners() {
        pluginListeners.clear();
    }
}
