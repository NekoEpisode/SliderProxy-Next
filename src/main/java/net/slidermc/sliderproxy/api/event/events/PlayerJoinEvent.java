package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

/**
 * 玩家加入代理事件
 * 在玩家成功连接到代理服务器时触发
 * 
 * 注意：此事件不能被取消，因为玩家已经完成连接
 */
public class PlayerJoinEvent extends PlayerEvent {
    public PlayerJoinEvent(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("PlayerJoinEvent cannot be cancelled - player has already joined");
    }
}
