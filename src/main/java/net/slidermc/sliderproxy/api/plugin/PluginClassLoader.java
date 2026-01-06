package net.slidermc.sliderproxy.api.plugin;

import net.slidermc.sliderproxy.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * 插件类加载器，实现插件间的类隔离和依赖访问控制
 */
public class PluginClassLoader extends URLClassLoader {
    private static final Logger log = LoggerFactory.getLogger(PluginClassLoader.class);

    private final PluginDescription description;
    private final PluginManager pluginManager;
    private final Set<String> loadedClasses = new HashSet<>();

    // 允许访问的包（API包）
    private static final Set<String> ALLOWED_PACKAGES = Set.of(
        "net.slidermc.sliderproxy.api",
        "net.slidermc.sliderproxy.api.plugin",
        "net.slidermc.sliderproxy.api.server",
        "net.slidermc.sliderproxy.api.config",
        "net.slidermc.sliderproxy.api.player"
    );

    public PluginClassLoader(URL[] urls, PluginDescription description, PluginManager pluginManager) {
        super(urls, Main.class.getClassLoader());
        this.description = description;
        this.pluginManager = pluginManager;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 同步加载类，避免并发问题
        synchronized (getClassLoadingLock(name)) {
            // 首先检查是否已经加载
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
            }

            // 1. 允许访问的API包 - 委托给父加载器
            if (isAllowedPackage(name)) {
                return super.loadClass(name, resolve);
            }

            // 2. 插件自身的类 - 优先从本插件加载
            if (name.startsWith(description.getMain().substring(0, description.getMain().lastIndexOf('.')))) {
                try {
                    Class<?> clazz = findClass(name);
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    loadedClasses.add(name);
                    return clazz;
                } catch (ClassNotFoundException e) {
                    // 继续尝试其他方式
                }
            }

            // 3. 依赖插件的类 - 检查依赖插件是否已加载
            for (String depend : description.getDepends()) {
                PluginContainer dependPlugin = pluginManager.getPlugin(depend);
                if (dependPlugin != null) {
                    try {
                        // 尝试从依赖插件的类加载器加载
                        Class<?> clazz = dependPlugin.getClassLoader().loadClass(name);
                        if (resolve) {
                            resolveClass(clazz);
                        }
                        return clazz;
                    } catch (ClassNotFoundException e) {
                        // 继续尝试下一个依赖
                    }
                }
            }

            // 4. 软依赖插件的类
            for (String softDepend : description.getSoftDepends()) {
                if (pluginManager.isPluginLoaded(softDepend)) {
                    PluginContainer softDependPlugin = pluginManager.getPlugin(softDepend);
                    try {
                        Class<?> clazz = softDependPlugin.getClassLoader().loadClass(name);
                        if (resolve) {
                            resolveClass(clazz);
                        }
                        return clazz;
                    } catch (ClassNotFoundException e) {
                        // 忽略软依赖加载失败
                    }
                }
            }

            // 5. 本插件JAR中的类
            try {
                Class<?> clazz = findClass(name);
                if (resolve) {
                    resolveClass(clazz);
                }
                loadedClasses.add(name);
                return clazz;
            } catch (ClassNotFoundException e) {
                // 继续尝试父加载器
            }

            // 6. 系统类加载器（最后手段）
            return super.loadClass(name, resolve);
        }
    }

    @Override
    public URL getResource(String name) {
        // 优先从本插件查找
        URL resource = findResource(name);
        if (resource != null) {
            return resource;
        }

        // 从依赖插件查找
        for (String depend : description.getDepends()) {
            PluginContainer dependPlugin = pluginManager.getPlugin(depend);
            if (dependPlugin != null) {
                resource = dependPlugin.getClassLoader().getResource(name);
                if (resource != null) {
                    return resource;
                }
            }
        }

        // 从软依赖插件查找
        for (String softDepend : description.getSoftDepends()) {
            if (pluginManager.isPluginLoaded(softDepend)) {
                PluginContainer softDependPlugin = pluginManager.getPlugin(softDepend);
                resource = softDependPlugin.getClassLoader().getResource(name);
                if (resource != null) {
                    return resource;
                }
            }
        }

        // 委托给父加载器
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // 这里可以实现更复杂的资源合并逻辑
        return super.getResources(name);
    }

    /**
     * 检查包是否允许访问（API包）
     */
    private boolean isAllowedPackage(String className) {
        for (String allowed : ALLOWED_PACKAGES) {
            if (className.startsWith(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取已加载的类列表（用于调试）
     */
    public Set<String> getLoadedClasses() {
        return new HashSet<>(loadedClasses);
    }

    /**
     * 获取插件描述
     */
    public PluginDescription getDescription() {
        return description;
    }
}
