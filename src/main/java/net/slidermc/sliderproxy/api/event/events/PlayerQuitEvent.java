package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

/**
 * 玩家退出代理事件
 * 在玩家断开连接时触发
 * 
 * 注意：此事件不能被取消，因为连接已经断开
 */
public class PlayerQuitEvent extends PlayerEvent {
    public PlayerQuitEvent(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("PlayerQuitEvent cannot be cancelled - player has already disconnected");
    }
}
