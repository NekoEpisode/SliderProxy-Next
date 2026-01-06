package net.slidermc.sliderproxy.api.plugin;

import java.io.File;

/**
 * 插件管理器持有类，提供全局访问
 */
public class PluginManagerHolder {
    private static PluginManager instance;

    /**
     * 初始化插件管理器
     */
    public static void initialize(File pluginsFolder) {
        if (instance == null) {
            instance = new PluginManager(pluginsFolder);
        }
    }

    /**
     * 获取插件管理器实例
     */
    public static PluginManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PluginManager not initialized");
        }
        return instance;
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * 关闭并清理
     */
    public static void shutdown() {
        if (instance != null) {
            instance.disablePlugins();
            instance = null;
        }
    }
}
