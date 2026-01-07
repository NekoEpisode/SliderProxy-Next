package net.slidermc.sliderproxy.network.connection;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.packet.clientbound.play.ClientboundSystemChatPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家连接管理 - 维护客户端与代理之间的连接状态
 * <p>
 * 架构说明：
 * - 上游状态（客户端 ↔ 代理）由 PlayerConnection 管理
 * - 下游状态（代理 ↔ 服务器）由各 MinecraftNettyClient 自主管理
 */
public class PlayerConnection {
    private static final Logger log = LoggerFactory.getLogger(PlayerConnection.class);
    private final Channel upstreamChannel;
    private volatile ProtocolState upstreamInboundProtocolState;
    private volatile ProtocolState upstreamOutboundProtocolState;

    // 下游 Channel 引用（用于快速访问）
    private volatile Channel downstreamChannel;

    public static final AttributeKey<PlayerConnection> KEY = AttributeKey.valueOf("player_connection");

    public PlayerConnection(@NotNull Channel upstreamChannel) {
        this.upstreamChannel = upstreamChannel;
        // 将 PlayerConnection 绑定到 Channel
        upstreamChannel.attr(KEY).set(this);
    }

    /**
     * 从 Channel 获取绑定的 PlayerConnection
     * @param channel 通道
     * @return 绑定的 PlayerConnection，可能为null
     */
    @Nullable
    public static PlayerConnection fromChannel(Channel channel) {
        return channel.attr(KEY).get();
    }

    /**
     * 获得上游通道（客户端 ↔ 代理）
     * @return 上游通道
     */
    public @NotNull Channel getUpstreamChannel() {
        return upstreamChannel;
    }

    /**
     * 获得下游通道（代理 ↔ 目标服务器）
     * @return 下游通道，可能为null（未连接下游时）
     */
    public @Nullable Channel getDownstreamChannel() {
        return downstreamChannel;
    }

    /**
     * 设置下游通道（代理 ↔ 目标服务器）
     * @param downstreamChannel 下游通道
     */
    public void setDownstreamChannel(Channel downstreamChannel) {
        this.downstreamChannel = downstreamChannel;
    }

    /**
     * 获得上游入站协议状态（代理接收客户端数据时使用的状态）
     * 注意：这是代理维护的状态，表示代理认为应该用什么状态解析客户端发来的数据
     * @return 上游入站协议状态
     */
    public ProtocolState getUpstreamInboundProtocolState() {
        return upstreamInboundProtocolState;
    }

    /**
     * 获得上游出站协议状态（代理发送数据给客户端时使用的状态）
     * 注意：这是代理维护的状态，表示代理准备用什么状态编码发送给客户端的数据
     * @return 上游出站协议状态
     */
    public ProtocolState getUpstreamOutboundProtocolState() {
        return upstreamOutboundProtocolState;
    }

    /**
     * 获得下游入站协议状态（代理接收服务器数据时使用的状态）
     * 注意：下游状态现在由 MinecraftNettyClient 自主管理
     * @return 下游入站协议状态，如果没有活跃的下游连接则返回 null
     * @deprecated 请直接从 MinecraftNettyClient 获取状态
     */
    @Deprecated
    public @Nullable ProtocolState getDownstreamInboundProtocolState() {
        if (downstreamChannel == null) return null;
        MinecraftNettyClient client = MinecraftNettyClient.fromChannel(downstreamChannel);
        return client != null ? client.getInboundProtocolState() : null;
    }

    /**
     * 获得下游出站协议状态（代理发送数据给服务器时使用的状态）
     * 注意：下游状态现在由 MinecraftNettyClient 自主管理
     * @return 下游出站协议状态，如果没有活跃的下游连接则返回 null
     * @deprecated 请直接从 MinecraftNettyClient 获取状态
     */
    @Deprecated
    public @Nullable ProtocolState getDownstreamOutboundProtocolState() {
        if (downstreamChannel == null) return null;
        MinecraftNettyClient client = MinecraftNettyClient.fromChannel(downstreamChannel);
        return client != null ? client.getOutboundProtocolState() : null;
    }

    /**
     * 设置上游入站协议状态（代理接收客户端数据时使用的状态）
     * @param upstreamInboundProtocolState 上游入站协议状态
     */
    public void setUpstreamInboundProtocolState(@NotNull ProtocolState upstreamInboundProtocolState) {
        this.upstreamInboundProtocolState = upstreamInboundProtocolState;
    }

    /**
     * 设置上游出站协议状态（代理发送数据给客户端时使用的状态）
     * @param upstreamOutboundProtocolState 上游出站协议状态
     */
    public void setUpstreamOutboundProtocolState(@NotNull ProtocolState upstreamOutboundProtocolState) {
        this.upstreamOutboundProtocolState = upstreamOutboundProtocolState;
        if (upstreamOutboundProtocolState == ProtocolState.PLAY) {
            ProxiedPlayer player = PlayerManager.getInstance().getPlayerByConnection(this);
            if (player != null && !player.getNeedSendChatPackets().isEmpty()) {
                log.debug("准备将缓存消息包发送给玩家 {}", player.getName());
                for (ClientboundSystemChatPacket packet : player.getNeedSendChatPackets()) {
                    player.sendPacket(packet);
                }
                player.getNeedSendChatPackets().clear();
            }
        }
    }
}