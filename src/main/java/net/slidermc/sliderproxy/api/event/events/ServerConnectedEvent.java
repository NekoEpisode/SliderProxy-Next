package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;

/**
 * 玩家成功连接到服务器事件
 * 在玩家成功连接到服务器后触发
 * 
 * 注意：此事件不能被取消，因为连接已经建立
 */
public class ServerConnectedEvent extends PlayerEvent {
    private final ProxiedServer server;

    public ServerConnectedEvent(ProxiedPlayer player, ProxiedServer server) {
        super(player);
        this.server = server;
    }

    /**
     * 获取玩家连接的服务器
     * @return 连接的服务器
     */
    public ProxiedServer getServer() {
        return server;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("ServerConnectedEvent cannot be cancelled - connection has already been established");
    }
}
