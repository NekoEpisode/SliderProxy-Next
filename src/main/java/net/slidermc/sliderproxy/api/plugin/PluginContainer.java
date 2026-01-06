package net.slidermc.sliderproxy.api.plugin;

/**
 * 插件容器，包含插件的描述信息、实例和类加载器
 */
public class PluginContainer {
    private final PluginDescription description;
    private final SliderProxyPlugin plugin;
    private final PluginClassLoader classLoader;

    public PluginContainer(PluginDescription description, SliderProxyPlugin plugin, PluginClassLoader classLoader) {
        this.description = description;
        this.plugin = plugin;
        this.classLoader = classLoader;
    }

    public PluginDescription getDescription() {
        return description;
    }

    public SliderProxyPlugin getPlugin() {
        return plugin;
    }

    public PluginClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * 获取插件的主类
     */
    public Class<?> getPluginClass() {
        return plugin.getClass();
    }

    /**
     * 获取插件名称
     */
    public String getName() {
        return description.getName();
    }

    /**
     * 获取插件版本
     */
    public String getVersion() {
        return description.getVersion();
    }
}
