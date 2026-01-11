package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

/**
 * 玩家配置阶段完成事件
 * 在玩家配置阶段完成时触发
 * 
 * 注意：此事件不能被取消，因为涉及协议状态切换
 */
public class PlayerConfigurationCompleteEvent extends PlayerEvent{
    public PlayerConfigurationCompleteEvent(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("PlayerConfigurationCompleteEvent cannot be cancelled - protocol state transition cannot be reverted");
    }
}
