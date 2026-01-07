package net.slidermc.sliderproxy.api.event;

/**
 * 事件基类
 * 所有事件都应该继承此类
 */
public abstract class Event {
    private boolean cancelled;

    public Event() {
        this.cancelled = false;
    }

    /**
     * 检查事件是否已被取消
     * @return 如果事件被取消则返回 true
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * 设置事件是否被取消
     * @param cancelled 是否取消
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * 获取事件的名称
     * @return 事件名称，默认为类名
     */
    public String getEventName() {
        return this.getClass().getSimpleName();
    }
}
