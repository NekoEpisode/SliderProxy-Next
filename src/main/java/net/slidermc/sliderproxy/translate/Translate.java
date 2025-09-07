package net.slidermc.sliderproxy.translate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 翻译器实例 - 管理特定语言的翻译映射
 */
public class Translate {
    private final Map<String, String> translateMap = new ConcurrentHashMap<>();

    /**
     * 根据key获取翻译内容
     * @param key 翻译key
     * @return 翻译内容，如果不存在返回null
     */
    @Nullable
    public String translate(@NotNull String key) {
        return translateMap.get(key);
    }

    /**
     * 根据key获取翻译内容，带默认值
     * @param key 翻译key
     * @param defaultValue 默认值
     * @return 翻译内容或默认值
     */
    @NotNull
    public String translateOrDefault(@NotNull String key, @NotNull String defaultValue) {
        String result = translateMap.get(key);
        return result != null ? result : defaultValue;
    }

    /**
     * 添加或更新翻译内容
     * @param key 翻译key
     * @param content 翻译内容
     * @return 之前的值，如果之前不存在返回null
     */
    @Nullable
    public String putTranslate(@NotNull String key, @NotNull String content) {
        return translateMap.put(key, content);
    }

    /**
     * 添加翻译内容（仅当key不存在时）
     * @param key 翻译key
     * @param content 翻译内容
     * @return 是否添加成功
     */
    public boolean addTranslate(@NotNull String key, @NotNull String content) {
        return translateMap.putIfAbsent(key, content) == null;
    }

    /**
     * 移除翻译内容
     * @param key 翻译key
     * @return 被移除的值，如果不存在返回null
     */
    @Nullable
    public String removeTranslate(@NotNull String key) {
        return translateMap.remove(key);
    }

    /**
     * 批量添加翻译内容
     * @param map 翻译映射
     */
    public void putAll(@NotNull Map<String, String> map) {
        translateMap.putAll(map);
    }

    /**
     * 清空所有翻译内容
     */
    public void clear() {
        translateMap.clear();
    }

    /**
     * 获取翻译数量
     * @return 翻译条目数量
     */
    public int size() {
        return translateMap.size();
    }

    /**
     * 检查是否包含指定key
     * @param key 翻译key
     * @return 是否包含
     */
    public boolean containsKey(@NotNull String key) {
        return translateMap.containsKey(key);
    }

    /**
     * 获取所有翻译键
     * @return 键的集合
     */
    @NotNull
    public Set<String> keySet() {
        return translateMap.keySet();
    }

    /**
     * 获取所有翻译条目
     * @return 翻译映射的只读视图
     */
    @NotNull
    public Map<String, String> getAllTranslations() {
        return new HashMap<>(translateMap);
    }
}