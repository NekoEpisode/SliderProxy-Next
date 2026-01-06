package net.slidermc.sliderproxy.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.NetworkPacketRegistry;
import net.slidermc.sliderproxy.network.packet.PacketDirection;
import net.slidermc.sliderproxy.network.packet.PacketInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketEncoder extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PacketEncoder.class);
    private final PacketDirection direction;

    /**
     * @param direction The direction packets are flowing TO.
     * CLIENTBOUND for upstream (Proxy -> Client).
     * SERVERBOUND for downstream (Proxy -> Server).
     */
    public PacketEncoder(PacketDirection direction) {
        this.direction = direction;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof IMinecraftPacket packet)) {
            super.write(ctx, msg, promise);
            return;
        }

        ProtocolState state;
        if (direction == PacketDirection.CLIENTBOUND) {
            // 上游方向：从 PlayerConnection 获取状态
            PlayerConnection connection = PlayerConnection.fromChannel(ctx.channel());
            if (connection == null) {
                promise.setFailure(new IllegalStateException("No PlayerConnection found for upstream channel"));
                ctx.close();
                return;
            }
            state = connection.getUpstreamOutboundProtocolState();
        } else {
            // 下游方向：从 MinecraftNettyClient 获取状态
            MinecraftNettyClient client = MinecraftNettyClient.fromChannel(ctx.channel());
            if (client == null) {
                promise.setFailure(new IllegalStateException("No MinecraftNettyClient found for downstream channel"));
                ctx.close();
                return;
            }
            state = client.getOutboundProtocolState();
        }

        if (state == null) {
            promise.setFailure(new IllegalStateException("Cannot determine protocol state for encoding packet: " + packet.getClass().getName()));
            ctx.close();
            return;
        }

        PacketInfo info = packetInfoOf(packet, direction, state);
        if (info == null) {
            promise.setFailure(new IllegalStateException("Unregistered packet for state " + state + ": " + packet.getClass().getName()));
            ctx.close();
            return;
        }
        int packetId = info.packetId();

        ByteBuf payload = ctx.alloc().buffer();
        ByteBuf withId = ctx.alloc().buffer();
        ByteBuf finalBuf = ctx.alloc().buffer();
        try {
            packet.write(payload);
            MinecraftProtocolHelper.writeVarInt(withId, packetId);
            withId.writeBytes(payload);
            MinecraftProtocolHelper.writeVarInt(finalBuf, withId.readableBytes());
            finalBuf.writeBytes(withId);

            ctx.write(finalBuf, promise);
            finalBuf = null;
        } finally {
            if (payload != null) payload.release();
            if (withId != null) withId.release();
            if (finalBuf != null) finalBuf.release();
        }
    }

    private PacketInfo packetInfoOf(IMinecraftPacket packet, PacketDirection direction, ProtocolState state) {
        NetworkPacketRegistry registry = NetworkPacketRegistry.getInstance();
        PacketInfo info = registry.getPacketInfo(direction, state, packet.getClass());
        if (info == null) {
            log.error("Unregistered packet object for Direction: {}, State: {}: {}", direction, state, packet.getClass().getName());
        }
        return info;
    }
}