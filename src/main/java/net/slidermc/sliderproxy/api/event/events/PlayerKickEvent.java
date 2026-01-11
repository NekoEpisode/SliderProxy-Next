package net.slidermc.sliderproxy.api.event.events;

import net.kyori.adventure.text.Component;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;

/**
 * 玩家被踢出事件
 * 在玩家被服务器踢出时触发
 */
public class PlayerKickEvent extends PlayerEvent {
    private final ProxiedServer kickedFrom;
    private Component reason;
    private ProxiedServer redirectServer;

    public PlayerKickEvent(ProxiedPlayer player, ProxiedServer kickedFrom, Component reason) {
        super(player);
        this.kickedFrom = kickedFrom;
        this.reason = reason;
    }

    /**
     * 获取玩家被踢出的服务器
     * @return 被踢出的服务器
     */
    public ProxiedServer getKickedFrom() {
        return kickedFrom;
    }

    /**
     * 获取踢出原因
     * @return 踢出原因
     */
    public Component getReason() {
        return reason;
    }

    /**
     * 设置踢出原因
     * @param reason 踢出原因
     */
    public void setReason(Component reason) {
        this.reason = reason;
    }

    /**
     * 获取重定向服务器
     * @return 重定向服务器，如果为 null 则玩家会被踢出代理
     */
    public ProxiedServer getRedirectServer() {
        return redirectServer;
    }

    /**
     * 设置重定向服务器
     * @param redirectServer 重定向服务器，设置后玩家会被转移到该服务器而不是被踢出
     */
    public void setRedirectServer(ProxiedServer redirectServer) {
        this.redirectServer = redirectServer;
    }
}
