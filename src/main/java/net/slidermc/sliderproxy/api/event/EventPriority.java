package net.slidermc.sliderproxy.api.event;

/**
 * 事件监听优先级
 * 用于控制事件监听器的执行顺序
 */
public enum EventPriority {
    /**
     * 最低优先级，最后执行
     */
    LOWEST(0),

    /**
     * 低优先级
     */
    LOW(1),

    /**
     * 正常优先级（默认）
     */
    NORMAL(2),

    /**
     * 高优先级
     */
    HIGH(3),

    /**
     * 最高优先级，最先执行
     */
    HIGHEST(4),

    /**
     * 监听器，用于在所有普通监听器之后执行
     * 通常用于监控或日志记录
     */
    MONITOR(5);

    private final int value;

    EventPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
