package net.slidermc.sliderproxy.api.plugin;

import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 插件管理器，负责插件的加载、依赖解析和生命周期管理
 */
public class PluginManager {
    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private final Map<String, PluginContainer> loadedPlugins = new LinkedHashMap<>();
    private final Map<String, PluginDescription> pluginDescriptions = new HashMap<>();
    private final File pluginsFolder;

    public PluginManager(File pluginsFolder) {
        this.pluginsFolder = pluginsFolder;
        if (!pluginsFolder.exists()) {
            pluginsFolder.mkdirs();
        }
    }

    /**
     * 创建配置好的YAML解析器，支持软依赖等字段映射
     */
    private Yaml createYaml() {
        // 自定义PropertyUtils来处理字段名映射
        PropertyUtils propertyUtils = new PropertyUtils() {
            @Override
            public Property getProperty(Class<?> type, String name) {
                // 映射YAML字段名到Java字段名
                if ("soft-depends".equals(name)) {
                    name = "softDepends";
                } else if ("api-version".equals(name)) {
                    name = "apiVersion";
                }
                return super.getProperty(type, name);
            }
        };
        propertyUtils.setSkipMissingProperties(true);

        Constructor constructor = new Constructor(PluginDescription.class, new LoaderOptions());
        constructor.setPropertyUtils(propertyUtils);
        return new Yaml(constructor);
    }

    /**
     * 加载所有插件
     */
    public void loadPlugins() {
        // 第一步：扫描所有插件并解析描述文件
        scanPlugins();

        // 第二步：解析依赖关系并排序
        List<PluginDescription> sortedPlugins = resolveDependencies();

        // 第三步：按顺序加载插件
        for (PluginDescription description : sortedPlugins) {
            loadPlugin(description);
        }

        if (!loadedPlugins.isEmpty()) {
            log.info(TranslateManager.translate("sliderproxy.plugins.loaded", loadedPlugins.size()));
        }
    }

    /**
     * 扫描插件文件夹，解析所有plugin.yml
     */
    private void scanPlugins() {
        File[] pluginFiles = pluginsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (pluginFiles == null) {
            return;
        }

        for (File jarFile : pluginFiles) {
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry pluginYmlEntry = jar.getJarEntry("plugin.yml");
                if (pluginYmlEntry == null) {
                    continue;
                }

                try (InputStream is = jar.getInputStream(pluginYmlEntry)) {
                    Yaml yaml = createYaml();
                    PluginDescription description = yaml.load(is);

                    if (description.getName() == null || description.getMain() == null) {
                        continue;
                    }

                    // 存储插件描述和对应的JAR文件
                    description.setMain(description.getMain()); // 确保主类名正确
                    pluginDescriptions.put(description.getName(), description);
                }
            } catch (Exception e) {
                log.error("插件加载失败: ", e);
            }
        }
    }

    /**
     * 解析依赖关系并返回排序后的插件列表
     */
    private List<PluginDescription> resolveDependencies() {
        List<PluginDescription> sorted = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        Set<String> unresolved = new HashSet<>(pluginDescriptions.keySet());

        while (!unresolved.isEmpty()) {
            boolean progress = false;

            for (String pluginName : unresolved) {
                PluginDescription desc = pluginDescriptions.get(pluginName);
                if (desc == null) continue;

                // 检查硬依赖是否已解析
                boolean canResolve = true;
                for (String depend : desc.getDepends()) {
                    if (!resolved.contains(depend) && pluginDescriptions.containsKey(depend)) {
                        canResolve = false;
                        break;
                    }
                    // 如果依赖不存在且不是软依赖，报错
                    if (!pluginDescriptions.containsKey(depend)) {
                        log.error("插件 {} 依赖的插件 {} 不存在", pluginName, depend);
                        canResolve = false;
                        break;
                    }
                }

                if (canResolve) {
                    sorted.add(desc);
                    resolved.add(pluginName);
                    unresolved.remove(pluginName);
                    progress = true;
                    break;
                }
            }

            if (!progress) {
                // 存在循环依赖或缺失依赖
                for (String pluginName : unresolved) {
                    PluginDescription desc = pluginDescriptions.get(pluginName);
                    log.error("插件 {} 无法解析依赖: {}", pluginName, desc.getDepends());
                }
                break;
            }
        }

        return sorted;
    }

    /**
     * 加载单个插件
     */
    private void loadPlugin(PluginDescription description) {
        try {
            // 查找对应的JAR文件
            File jarFile = findJarFile(description.getName());
            if (jarFile == null) {
                return;
            }

            // 创建插件类加载器
            PluginClassLoader classLoader = new PluginClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    description,
                    this
            );

            // 加载主类
            Class<?> mainClass = classLoader.loadClass(description.getMain());
            if (!SliderProxyPlugin.class.isAssignableFrom(mainClass)) {
                return;
            }

            // 创建插件实例
            SliderProxyPlugin pluginInstance = (SliderProxyPlugin) mainClass.getDeclaredConstructor().newInstance();

            // 为插件分配Logger - 使用net.slidermc.sliderproxy.api.plugin前缀以匹配log4j配置
            Logger pluginLogger = LoggerFactory.getLogger("net.slidermc.sliderproxy.api.plugin.Plugin." + description.getName());
            pluginInstance.setLogger(pluginLogger);

            // 创建插件容器
            PluginContainer container = new PluginContainer(description, pluginInstance, classLoader);

            // 调用 onLoad
            try {
                pluginInstance.onLoad();
            } catch (Exception e) {
                pluginLogger.error("onLoad() 执行失败", e);
            }

            // 存储插件
            loadedPlugins.put(description.getName(), container);

        } catch (Exception e) {
            // 插件加载失败，静默处理
        }
    }

    /**
     * 启用所有已加载的插件
     */
    public void enablePlugins() {
        for (PluginContainer container : loadedPlugins.values()) {
            try {
                container.getPlugin().onEnable();
            } catch (Exception e) {
                container.getPlugin().getLogger().error("启用失败", e);
            }
        }
    }

    /**
     * 禁用所有插件
     */
    public void disablePlugins() {
        // 按照依赖的反向顺序禁用
        List<PluginContainer> reverseOrder = new ArrayList<>(loadedPlugins.values());
        Collections.reverse(reverseOrder);

        for (PluginContainer container : reverseOrder) {
            try {
                container.getPlugin().onDisable();
            } catch (Exception e) {
                container.getPlugin().getLogger().error("禁用失败", e);
            }
        }
        loadedPlugins.clear();
    }

    /**
     * 重新加载插件
     */
    public void reloadPlugins() {
        disablePlugins();
        pluginDescriptions.clear();
        loadPlugins();
        enablePlugins();
    }

    /**
     * 根据插件名查找JAR文件
     */
    private File findJarFile(String pluginName) {
        File[] files = pluginsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return null;

        Yaml yaml = createYaml();

        for (File file : files) {
            try (JarFile jar = new JarFile(file)) {
                JarEntry entry = jar.getJarEntry("plugin.yml");
                if (entry != null) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        PluginDescription desc = yaml.load(is);
                        if (pluginName.equals(desc.getName())) {
                            return file;
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略无法读取的JAR文件
            }
        }
        return null;
    }

    /**
     * 获取已加载的插件
     */
    public PluginContainer getPlugin(String name) {
        return loadedPlugins.get(name);
    }

    /**
     * 检查插件是否已加载
     */
    public boolean isPluginLoaded(String name) {
        return loadedPlugins.containsKey(name);
    }

    /**
     * 获取所有已加载的插件
     */
    public Map<String, PluginContainer> getLoadedPlugins() {
        return Collections.unmodifiableMap(loadedPlugins);
    }
}
