package net.slidermc.sliderproxy.api.plugin;

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
}
