package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.event.Event;

/**
 * 代理关闭事件
 * 在代理服务器关闭时触发
 * 
 * 注意：此事件不能被取消，因为关闭流程已经开始
 */
public class ProxyShutdownEvent extends Event {
    public ProxyShutdownEvent() {
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("ProxyShutdownEvent cannot be cancelled - shutdown has already started");
    }
}
