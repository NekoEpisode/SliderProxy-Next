package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;

/**
 * 玩家切换服务器事件
 * 在玩家从一个服务器切换到另一个服务器时触发
 */
public class ServerSwitchEvent extends PlayerEvent {
    private final ProxiedServer from;
    private ProxiedServer to;

    public ServerSwitchEvent(ProxiedPlayer player, ProxiedServer from, ProxiedServer to) {
        super(player);
        this.from = from;
        this.to = to;
    }

    /**
     * 获取玩家切换前的服务器
     * @return 原服务器，如果是首次连接则为 null
     */
    public ProxiedServer getFrom() {
        return from;
    }

    /**
     * 获取玩家切换后的目标服务器
     * @return 目标服务器
     */
    public ProxiedServer getTo() {
        return to;
    }

    /**
     * 设置玩家切换的目标服务器
     * @param to 新的目标服务器
     */
    public void setTo(ProxiedServer to) {
        this.to = to;
    }
}
