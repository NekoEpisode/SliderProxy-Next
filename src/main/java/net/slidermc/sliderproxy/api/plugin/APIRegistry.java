package net.slidermc.sliderproxy.api.plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API注册表，用于插件间共享API
 */
public class APIRegistry {
    private static final APIRegistry instance = new APIRegistry();

    private final Map<String, Object> apiRegistry = new ConcurrentHashMap<>();
    private final Map<String, String> apiVersions = new ConcurrentHashMap<>();

    private APIRegistry() {}

    public static APIRegistry getInstance() {
        return instance;
    }

    /**
     * 注册API
     */
    public void registerAPI(String name, Object api, String version) {
        apiRegistry.put(name, api);
        apiVersions.put(name, version);
    }

    /**
     * 获取API
     */
    public <T> T getAPI(String name, Class<T> type) {
        Object api = apiRegistry.get(name);
        if (type.isInstance(api)) {
            return type.cast(api);
        }
        return null;
    }

    /**
     * 检查API是否存在
     */
    public boolean hasAPI(String name) {
        return apiRegistry.containsKey(name);
    }

    /**
     * 获取API版本
     */
    public String getAPIVersion(String name) {
        return apiVersions.get(name);
    }

    /**
     * 注销API
     */
    public void unregisterAPI(String name) {
        apiRegistry.remove(name);
        apiVersions.remove(name);
    }

    /**
     * 清空所有API
     */
    public void clear() {
        apiRegistry.clear();
        apiVersions.clear();
    }
}
