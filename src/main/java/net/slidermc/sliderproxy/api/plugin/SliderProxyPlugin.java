package net.slidermc.sliderproxy.api.plugin;

import net.slidermc.sliderproxy.api.event.PluginEventRegistry;
import org.slf4j.Logger;

/**
 * 插件基类，所有插件都应继承此类
 */
public abstract class SliderProxyPlugin {

    protected Logger logger;

    /**
     * 内部方法：设置插件的Logger
     */
    final void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * 获取插件的Logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * 插件加载时调用（仅一次）
     */
    public void onLoad() {}

    /**
     * 插件启用时调用
     */
    public abstract void onEnable();

    /**
     * 插件禁用时调用
     */
    public abstract void onDisable();

    /**
     * 插件卸载时调用
     */
    public void onUnLoad() {}

    /**
     * 插件重载时调用（默认实现：禁用后重新启用）
     */
    public void onReload() {
        onDisable();
        onEnable();
    }

    /**
     * 为插件注册事件监听器
     * 插件卸载时会自动清理
     *
     * @param listener 监听器对象
     */
    protected void registerListener(Object listener) {
        PluginEventRegistry.registerListener(this, listener);
    }

    /**
     * 为插件注册多个事件监听器
     *
     * @param listeners 监听器对象列表
     */
    protected void registerListeners(Object... listeners) {
        PluginEventRegistry.registerListeners(this, listeners);
    }

    /**
     * 注销插件的所有事件监听器
     * 通常不需要手动调用，插件禁用时会自动清理
     */
    protected void unregisterAllListeners() {
        PluginEventRegistry.unregisterAllListeners(this);
    }
}
