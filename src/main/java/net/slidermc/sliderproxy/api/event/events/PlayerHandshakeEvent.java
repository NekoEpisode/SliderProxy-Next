package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.event.Event;

/**
 * 玩家握手事件
 * 在玩家发送握手包时触发
 */
public class PlayerHandshakeEvent extends Event {
    private final String hostname;
    private final int port;
    private final int protocolVersion;
    private final int nextState;

    public PlayerHandshakeEvent(String hostname, int port, int protocolVersion, int nextState) {
        this.hostname = hostname;
        this.port = port;
        this.protocolVersion = protocolVersion;
        this.nextState = nextState;
    }

    /**
     * 获取客户端连接的主机名
     * @return 主机名
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * 获取客户端连接的端口
     * @return 端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取客户端的协议版本
     * @return 协议版本号
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * 获取下一个状态（1=状态查询，2=登录）
     * @return 下一个状态
     */
    public int getNextState() {
        return nextState;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        if (cancelled) {
            throw new UnsupportedOperationException("PlayerHandshakeEvent cannot be cancelled. The handshake has already been completed.");
        }
    }
}
