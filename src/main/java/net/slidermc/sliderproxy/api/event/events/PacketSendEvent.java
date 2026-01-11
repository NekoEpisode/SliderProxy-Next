package net.slidermc.sliderproxy.api.event.events;

import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.event.Event;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

/**
 * 数据包发送事件
 * 在代理发送数据包时触发（无论是发送给客户端还是服务器）
 */
public class PacketSendEvent extends Event {
    private final ProxiedPlayer player;
    private IMinecraftPacket packet;
    private final Direction direction;
    private final ProtocolState protocolState;
    private final ChannelHandlerContext ctx;

    public PacketSendEvent(ProxiedPlayer player, IMinecraftPacket packet, Direction direction,
                          ProtocolState protocolState, ChannelHandlerContext ctx) {
        this.player = player;
        this.packet = packet;
        this.direction = direction;
        this.protocolState = protocolState;
        this.ctx = ctx;
    }

    /**
     * 获取发送数据包的玩家
     * @return 玩家对象，可能为 null（如在握手阶段）
     */
    public ProxiedPlayer getPlayer() {
        return player;
    }

    /**
     * 获取要发送的数据包
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
     * @return 方向（发送给客户端或服务器）
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
     * 数据包的方向
     */
    public enum Direction {
        /** 发送给客户端（上游） */
        TO_CLIENT,
        /** 发送给服务器（下游） */
        TO_SERVER
    }
}
