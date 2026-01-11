package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.event.Event;

/**
 * 代理初始化事件
 * 在代理服务器初始化时触发
 * 
 * 注意：此事件不能被取消，因为初始化已经开始
 */
public class ProxyInitializeEvent extends Event {
    public ProxyInitializeEvent() {
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("ProxyInitializeEvent cannot be cancelled - initialization has already started");
    }
}
