package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

/**
 * 玩家登录完成事件
 * 在玩家完成登录流程时触发
 * 
 * 注意：此事件不能被取消，因为登录已经完成
 */
public class PlayerLoginCompleteEvent extends PlayerEvent {
    public PlayerLoginCompleteEvent(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("PlayerLoginCompleteEvent cannot be cancelled - login has already completed");
    }
}
