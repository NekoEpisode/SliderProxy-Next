package net.slidermc.sliderproxy.translate;

import com.google.gson.Gson;
import io.leangen.geantyref.TypeToken;
import net.slidermc.sliderproxy.utils.GsonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 翻译管理器 - 管理所有语言的翻译映射
 */
public final class TranslateManager {
    private static String currentLanguage = "zh_cn"; // 默认语言：简体中文
    private static final String fallbackLanguage = "zh_cn";

    private static final Map<String, Translate> translators = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(TranslateManager.class);

    private TranslateManager() {
        // 私有构造，防止实例化
    }

    /**
     * 注册新语言或获取现有语言
     * @param languageCode 语言代码，如 "zh_cn", "en_us"
     * @return 对应的翻译器
     */
    @NotNull
    public static Translate registerLanguage(@NotNull String languageCode) {
        return translators.computeIfAbsent(languageCode, code -> {
            log.info("注册新语言: {}", code);
            return new Translate();
        });
    }

    /**
     * 从JSON文件加载翻译到指定语言
     * @param languageCode 语言代码
     * @param resourcePath 资源文件路径
     * @return 是否加载成功
     */
    public static boolean loadFromResource(@NotNull String languageCode, @NotNull String resourcePath) {
        Translate translator = registerLanguage(languageCode);

        try (InputStream inputStream = TranslateManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                log.warn("资源文件不存在: {}", resourcePath);
                return false;
            }

            Gson gson = GsonUtils.getGson();
            Map<String, String> translations = gson.fromJson(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() {}.getType()
            );

            translator.putAll(translations);
            log.info("为语言 {} 加载了 {} 条翻译 from {}", languageCode, translations.size(), resourcePath);
            return true;

        } catch (IOException e) {
            log.error("加载翻译文件失败: {}", resourcePath, e);
            return false;
        }
    }

    /**
     * 获取指定语言的翻译器
     * @param languageCode 语言代码
     * @return 翻译器，如果不存在则创建新的
     */
    @NotNull
    public static Translate getTranslator(@NotNull String languageCode) {
        return registerLanguage(languageCode);
    }

    /**
     * 获取所有已注册的语言代码
     * @return 语言代码列表
     */
    @NotNull
    public static List<String> getRegisteredLanguages() {
        return new ArrayList<>(translators.keySet());
    }

    /**
     * 检查语言是否已注册
     * @param languageCode 语言代码
     * @return 是否已注册
     */
    public static boolean isLanguageRegistered(@NotNull String languageCode) {
        return translators.containsKey(languageCode);
    }

    /**
     * 移除语言注册
     * @param languageCode 语言代码
     * @return 是否移除成功
     */
    public static boolean unregisterLanguage(@NotNull String languageCode) {
        Translate removed = translators.remove(languageCode);
        if (removed != null) {
            log.info("移除语言: {}", languageCode);
            return true;
        }
        return false;
    }

    /**
     * 直接翻译文本 (使用指定语言)
     * @param languageCode 语言代码
     * @param key 翻译键
     * @return 翻译结果，如果不存在返回null
     */
    @Nullable
    public static String translateWithLang(@NotNull String languageCode, @NotNull String key) {
        Translate translator = translators.get(languageCode);
        return translator != null ? translator.translate(key) : null;
    }

    /**
     * 直接翻译文本 (使用当前语言)
     * @param key 翻译键
     * @return 翻译结果，如果不存在返回null
     */
    @Nullable
    public static String translate(@NotNull String key) {
        return translateWithLang(currentLanguage, key);
    }

    /**
     * 直接翻译文本并格式化参数 (使用当前语言)
     * @param key 翻译键
     * @param args 格式化参数
     * @return 格式化后的翻译结果，如果不存在返回null
     */
    @Nullable
    public static String translate(@NotNull String key, Object... args) {
        return translateWithLang(currentLanguage, key, args);
    }

    /**
     * 直接翻译文本并格式化参数 (使用指定语言)
     * @param languageCode 语言代码
     * @param key 翻译键
     * @param args 格式化参数
     * @return 格式化后的翻译结果，如果不存在返回null
     */
    @Nullable
    public static String translateWithLang(@NotNull String languageCode, @NotNull String key, Object... args) {
        String template = translateWithLang(languageCode, key);
        if (template == null) return null;

        for (int i = 0; i < args.length; i++) {
            String placeholder = i == 0 ? "%s%" : "%s" + i + "%";
            template = template.replace(placeholder, String.valueOf(args[i]));
        }
        return template;
    }

    /**
     * 直接翻译文本带默认值并格式化参数 (使用指定语言)
     * @param languageCode 语言代码
     * @param key 翻译键
     * @param defaultValue 默认值
     * @param args 格式化参数
     * @return 格式化后的翻译结果或默认值
     */
    @NotNull
    public static String translateWithLangOrDefault(@NotNull String languageCode, @NotNull String key, @NotNull String defaultValue, Object... args) {
        String template = translateWithLangOrDefault(languageCode, key, defaultValue);

        for (int i = 0; i < args.length; i++) {
            String placeholder = i == 0 ? "%s%" : "%s" + i + "%";
            template = template.replace(placeholder, String.valueOf(args[i]));
        }
        return template;
    }

    /**
     * 直接翻译文本带默认值 (使用指定语言)
     * @param languageCode 语言代码
     * @param key 翻译键
     * @param defaultValue 默认值
     * @return 翻译结果或默认值
     */
    @NotNull
    public static String translateWithLangOrDefault(@NotNull String languageCode, @NotNull String key, @NotNull String defaultValue) {
        String result = translateWithLang(languageCode, key);
        return result != null ? result : defaultValue;
    }

    /**
     * 获取当前使用的语言的语言代码
     * 如当前语言为null则返回默认回退语言
     * @return 当前语言代码
     */
    public static @NotNull String getCurrentLanguageCode() {
        if (currentLanguage == null) return fallbackLanguage;
        return currentLanguage;
    }

    /**
     * 设置当前语言
     * @param languageCode 语言代码
     */
    public static void setCurrentLanguage(@NotNull String languageCode) {
        if (!isLanguageRegistered(languageCode)) {
            return;
        }
        TranslateManager.currentLanguage = languageCode;
    }
}
