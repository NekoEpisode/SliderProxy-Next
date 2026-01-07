package net.slidermc.sliderproxy.api.event;

import java.lang.annotation.*;

/**
 * 事件监听器注解
 * 用于标记方法为事件监听器
 * <p>
 * 使用示例：
 * <pre>
 * &#064;EventListener
 * public void onPlayerJoin(PlayerJoinEvent event) {
 *     // 处理事件
 * }
 *
 * &#064;EventListener(priority  = EventPriority.HIGH)
 * public void onChat(ChatEvent event) {
 *     // 高优先级监听
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {
    /**
     * 事件监听优先级
     * 值越小越先执行
     * @return 优先级，默认为 NORMAL
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * 是否忽略已取消的事件
     * @return true 表示忽略已取消的事件，默认为 false
     */
    boolean ignoreCancelled() default false;
}
