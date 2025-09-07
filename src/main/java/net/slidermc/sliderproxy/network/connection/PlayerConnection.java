package net.slidermc.sliderproxy.network.connection;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.slidermc.sliderproxy.network.ProtocolState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家连接管理 - 维护客户端与代理之间的连接状态
 * 注意：所有协议状态都是代理维护的状态，不是客户端或服务器的实际状态
 */
public class PlayerConnection {
    private static final Logger log = LoggerFactory.getLogger(PlayerConnection.class);
    private final Channel upstreamChannel;
    private volatile ProtocolState upstreamInboundProtocolState;
    private volatile ProtocolState upstreamOutboundProtocolState;

    private volatile Channel downstreamChannel;
    private volatile ProtocolState downstreamInboundProtocolState;
    private volatile ProtocolState downstreamOutboundProtocolState;

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
     * 注意：这是代理维护的状态，表示代理认为应该用什么状态解析服务器发来的数据
     * @return 下游入站协议状态
     */
    public ProtocolState getDownstreamInboundProtocolState() {
        return downstreamInboundProtocolState;
    }

    /**
     * 获得下游出站协议状态（代理发送数据给服务器时使用的状态）
     * 注意：这是代理维护的状态，表示代理准备用什么状态编码发送给服务器的数据
     * @return 下游出站协议状态
     */
    public ProtocolState getDownstreamOutboundProtocolState() {
        return downstreamOutboundProtocolState;
    }

    /**
     * 设置下游入站协议状态（代理接收服务器数据时使用的状态）
     * @param downstreamInboundProtocolState 下游入站协议状态
     */
    public void setDownstreamInboundProtocolState(@NotNull ProtocolState downstreamInboundProtocolState) {
        /*log.debug("设置下游入站状态: {} -> {} [调用栈: {}]",
                this.downstreamInboundProtocolState, downstreamInboundProtocolState,  // 修正：使用正确的变量
                Thread.currentThread().getStackTrace()[2]);*/
        this.downstreamInboundProtocolState = downstreamInboundProtocolState;
    }

    /**
     * 设置下游出站协议状态（代理发送数据给服务器时使用的状态）
     * @param downstreamOutboundProtocolState 下游出站协议状态
     */
    public void setDownstreamOutboundProtocolState(@NotNull ProtocolState downstreamOutboundProtocolState) {
        /*log.debug("设置下游出站状态: {} -> {} [调用栈: {}]",
                this.downstreamOutboundProtocolState, downstreamOutboundProtocolState,  // 修正
                Thread.currentThread().getStackTrace()[2]);*/
        this.downstreamOutboundProtocolState = downstreamOutboundProtocolState;
    }

    /**
     * 设置上游入站协议状态（代理接收客户端数据时使用的状态）
     * @param upstreamInboundProtocolState 上游入站协议状态
     */
    public void setUpstreamInboundProtocolState(@NotNull ProtocolState upstreamInboundProtocolState) {
        /*log.debug("设置上游入站状态: {} -> {} [调用栈: {}]",
                this.upstreamInboundProtocolState, upstreamInboundProtocolState,
                Thread.currentThread().getStackTrace()[2]);*/
        this.upstreamInboundProtocolState = upstreamInboundProtocolState;
    }

    /**
     * 设置上游出站协议状态（代理发送数据给客户端时使用的状态）
     * @param upstreamOutboundProtocolState 上游出站协议状态
     */
    public void setUpstreamOutboundProtocolState(@NotNull ProtocolState upstreamOutboundProtocolState) {
        /*log.debug("设置上游出站状态: {} -> {} [调用栈: {}]",
                this.upstreamOutboundProtocolState, upstreamOutboundProtocolState,  // 修正
                Thread.currentThread().getStackTrace()[2]);*/
        this.upstreamOutboundProtocolState = upstreamOutboundProtocolState;
    }
}