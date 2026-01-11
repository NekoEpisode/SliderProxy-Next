package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;

/**
 * 玩家从服务器断开连接事件
 * 在玩家从服务器断开连接时触发
 * 
 * 注意：此事件不能被取消，因为连接已经断开
 */
public class ServerDisconnectEvent extends PlayerEvent {
    private final ProxiedServer server;

    public ServerDisconnectEvent(ProxiedPlayer player, ProxiedServer server) {
        super(player);
        this.server = server;
    }

    /**
     * 获取玩家断开连接的服务器
     * @return 断开连接的服务器
     */
    public ProxiedServer getServer() {
        return server;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("ServerDisconnectEvent cannot be cancelled - connection has already been closed");
    }
}
