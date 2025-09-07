package net.slidermc.sliderproxy.api.config;

import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class YamlConfiguration {
    private static final Logger log = LoggerFactory.getLogger(YamlConfiguration.class);
    private Map<String, Object> configMap;
    private static final ThreadLocal<Yaml> YAML_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        DumperOptions options = new DumperOptions();
        options.setIndent(4);
        options.setExplicitStart(false);
        options.setExplicitEnd(false);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setLineBreak(DumperOptions.LineBreak.UNIX);
        return new Yaml(options);
    });

    @SuppressWarnings("unchecked")
    public YamlConfiguration(InputStream stream) {
        Yaml YAML = YAML_THREAD_LOCAL.get();
        Object loaded = YAML.load(stream);
        // 处理空配置文件的情况
        configMap = loaded instanceof Map ? new ConcurrentHashMap<>((Map<String, Object>) loaded) : new ConcurrentHashMap<>();
    }

    public YamlConfiguration(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public YamlConfiguration() {
        this.configMap = new ConcurrentHashMap<>();
    }

    // 类型转换方法
    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int defaultValue) {
        Object obj = get(path);
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String getString(String path) {
        return getString(path, null);
    }

    public String getString(String path, String defaultValue) {
        Object obj = get(path);
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj != null) {
            return obj.toString();
        }
        return defaultValue;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        Object obj = get(path);
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof String) {
            return Boolean.parseBoolean((String) obj);
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue() != 0;
        }
        return defaultValue;
    }

    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    public double getDouble(String path, double defaultValue) {
        Object obj = get(path);
        if (obj instanceof Double) {
            return (Double) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 设置一个值到指定的路径（不自动创建中间路径）
     */
    public boolean set(String path, Object value) {
        return set(path, value, false);
    }

    /**
     * 设置一个值到指定的路径
     */
    @SuppressWarnings("unchecked")
    public boolean set(String path, Object value, boolean createPath) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        if (configMap == null) {
            configMap = new ConcurrentHashMap<>();
        }

        String[] keys = path.split("\\.");
        if (keys.length == 0) {
            return false;
        }

        Map<String, Object> current = configMap;

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (key.isEmpty()) {
                continue;
            }

            Object obj = current.get(key);

            if (obj instanceof Map) {
                current = (Map<String, Object>) obj;
            } else if (createPath) {
                if (obj != null) {
                    return false;
                }
                Map<String, Object> newMap = new ConcurrentHashMap<>();
                current.put(key, newMap);
                current = newMap;
            } else {
                return false;
            }
        }

        String lastKey = keys[keys.length - 1];
        if (lastKey.isEmpty()) {
            return false;
        }

        current.put(lastKey, value);
        return true;
    }

    /**
     * 获取指定路径的值
     */
    @SuppressWarnings("unchecked")
    public Object get(String path) {
        if (path == null || path.isEmpty() || configMap == null) {
            return null;
        }

        String[] keys = path.split("\\.");
        if (keys.length == 0) {
            return null;
        }

        Map<String, Object> current = configMap;

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (key.isEmpty()) {
                continue;
            }

            Object obj = current.get(key);
            if (obj instanceof Map) {
                current = (Map<String, Object>) obj;
            } else {
                return null;
            }
        }

        String lastKey = keys[keys.length - 1];
        return current.get(lastKey);
    }

    public boolean contains(String path) {
        return get(path) != null;
    }

    @SuppressWarnings("unchecked")
    public Object remove(String path) {
        if (path == null || path.isEmpty() || configMap == null) {
            return null;
        }

        String[] keys = path.split("\\.");
        if (keys.length == 0) {
            return null;
        }

        Map<String, Object> current = configMap;

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (key.isEmpty()) {
                continue;
            }

            Object obj = current.get(key);
            if (obj instanceof Map) {
                current = (Map<String, Object>) obj;
            } else {
                return null;
            }
        }

        String lastKey = keys[keys.length - 1];
        return current.remove(lastKey);
    }

    public Map<String, Object> getConfigMap() {
        return new HashMap<>(configMap); // 返回副本以保证线程安全
    }

    public void clear() {
        if (configMap != null) {
            configMap.clear();
        }
    }

    /**
     * 保存配置到文件
     */
    public void save(File file) {
        Yaml yaml = YAML_THREAD_LOCAL.get();
        try (FileWriter writer = new FileWriter(file)) {
            yaml.dump(configMap, writer);
        }catch (IOException e) {
            log.error(TranslateManager.translate("sliderproxy.config.save.error", e));
        }
    }
}