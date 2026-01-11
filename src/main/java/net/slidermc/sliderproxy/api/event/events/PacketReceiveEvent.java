package net.slidermc.sliderproxy.api.event.events;

import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.event.Event;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

/**
 * 数据包接收事件
 * 在代理接收到数据包时触发（无论是来自客户端还是服务器）
 */
public class PacketReceiveEvent extends Event {
    private final ProxiedPlayer player;
    private IMinecraftPacket packet;
    private final Direction direction;
    private final ProtocolState protocolState;
    private final ChannelHandlerContext ctx;
    private boolean forwarded = true;

    public PacketReceiveEvent(ProxiedPlayer player, IMinecraftPacket packet, Direction direction, 
                             ProtocolState protocolState, ChannelHandlerContext ctx) {
        this.player = player;
        this.packet = packet;
        this.direction = direction;
        this.protocolState = protocolState;
        this.ctx = ctx;
    }

    /**
     * 获取接收数据包的玩家
     * @return 玩家对象，可能为 null（如在握手阶段）
     */
    public ProxiedPlayer getPlayer() {
        return player;
    }

    /**
     * 获取接收到的数据包
     * @return 数据包对象
     */
    public IMinecraftPacket getPacket() {
        return packet;
    }

    /**
     * 设置数据包（可以替换为其他数据包）
     * @param packet 新的数据包
     */
    public void setPacket(IMinecraftPacket packet) {
        this.packet = packet;
    }

    /**
     * 获取数据包的方向
     * @return 方向（来自客户端或服务器）
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * 获取当前的协议状态
     * @return 协议状态
     */
    public ProtocolState getProtocolState() {
        return protocolState;
    }

    /**
     * 获取 Netty 的 ChannelHandlerContext
     * @return ChannelHandlerContext
     */
    public ChannelHandlerContext getContext() {
        return ctx;
    }

    /**
     * 检查数据包是否会被转发
     * @return 如果数据包会被转发则返回 true
     */
    public boolean isForwarded() {
        return forwarded;
    }

    /**
     * 设置数据包是否转发
     * @param forwarded 是否转发
     */
    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    /**
     * 数据包的方向
     */
    public enum Direction {
        /** 来自客户端（上游） */
        FROM_CLIENT,
        /** 来自服务器（下游） */
        FROM_SERVER
    }
}
