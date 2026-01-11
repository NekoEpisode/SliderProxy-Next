package net.slidermc.sliderproxy.api.event.events;

import net.kyori.adventure.text.Component;
import net.slidermc.sliderproxy.api.event.Event;

/**
 * 代理服务器 Ping 事件
 * 在客户端查询服务器状态时触发
 */
public class ProxyPingEvent extends Event {
    private Component description;
    private int maxPlayers;
    private int onlinePlayers;
    private String version;
    private int protocol;
    private String favicon;

    public ProxyPingEvent(Component description, int maxPlayers, int onlinePlayers, 
                         String version, int protocol, String favicon) {
        this.description = description;
        this.maxPlayers = maxPlayers;
        this.onlinePlayers = onlinePlayers;
        this.version = version;
        this.protocol = protocol;
        this.favicon = favicon;
    }

    /**
     * 获取服务器描述（MOTD）
     * @return 服务器描述
     */
    public Component getDescription() {
        return description;
    }

    /**
     * 设置服务器描述（MOTD）
     * @param description 服务器描述
     */
    public void setDescription(Component description) {
        this.description = description;
    }

    /**
     * 获取最大玩家数
     * @return 最大玩家数
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * 设置最大玩家数
     * @param maxPlayers 最大玩家数
     */
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    /**
     * 获取在线玩家数
     * @return 在线玩家数
     */
    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    /**
     * 设置在线玩家数
     * @param onlinePlayers 在线玩家数
     */
    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }

    /**
     * 获取版本名称
     * @return 版本名称
     */
    public String getVersion() {
        return version;
    }

    /**
     * 设置版本名称
     * @param version 版本名称
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * 获取协议版本号
     * @return 协议版本号
     */
    public int getProtocol() {
        return protocol;
    }

    /**
     * 设置协议版本号
     * @param protocol 协议版本号
     */
    public void setProtocol(int protocol) {
        this.protocol = protocol;
    }

    /**
     * 获取服务器图标（Base64 编码）
     * @return 服务器图标
     */
    public String getFavicon() {
        return favicon;
    }

    /**
     * 设置服务器图标（Base64 编码）
     * @param favicon 服务器图标
     */
    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        if (cancelled) {
            throw new UnsupportedOperationException("ProxyPingEvent cannot be cancelled. You can modify the ping response data instead.");
        }
    }
}
