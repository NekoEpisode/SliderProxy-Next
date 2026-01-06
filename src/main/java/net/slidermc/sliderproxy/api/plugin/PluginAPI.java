package net.slidermc.sliderproxy.api.plugin;

/**
 * 插件API接口，用于插件间通信
 * 插件可以通过实现此接口来提供API给其他插件使用
 */
public interface PluginAPI {

    /**
     * 获取API版本
     */
    String getAPIVersion();

    /**
     * 检查API是否兼容
     */
    boolean isCompatible(String version);
}
