package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

/**
 * 玩家进入配置阶段前事件
 * 在玩家进入配置阶段前触发
 * 
 * 注意：此事件不能被取消，因为涉及协议状态切换
 */
public class PlayerPreConfigurationEvent extends PlayerEvent{
    public PlayerPreConfigurationEvent(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("PlayerPreConfigurationEvent cannot be cancelled - protocol state transition cannot be reverted");
    }
}
