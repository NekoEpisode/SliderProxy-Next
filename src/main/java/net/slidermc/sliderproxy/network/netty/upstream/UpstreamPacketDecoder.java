package net.slidermc.sliderproxy.network.netty.upstream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.NetworkPacketRegistry;
import net.slidermc.sliderproxy.network.packet.PacketDirection;
import net.slidermc.sliderproxy.network.packet.PacketInfo;
import net.slidermc.sliderproxy.network.packet.clientbound.ClientboundDisconnectPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.slidermc.sliderproxy.network.packet.PacketForwarder.forwardUnknownPacket;

public class UpstreamPacketDecoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(UpstreamPacketDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        try {
            byteBuf.markReaderIndex();

            int packetId = MinecraftProtocolHelper.readVarInt(byteBuf);
            PlayerConnection playerConnection = channelHandlerContext.channel().attr(PlayerConnection.KEY).get();
            ProtocolState state = playerConnection.getUpstreamInboundProtocolState();

            log.debug("📥 收到上游包: id=0x{}, state={}, downstreamConnected={}, remoteAddress={}",
                    Integer.toHexString(packetId),
                    state,
                    playerConnection.getDownstreamChannel() != null,
                    channelHandlerContext.channel().remoteAddress());

            PacketInfo packetInfo = NetworkPacketRegistry.getInstance().getPacketInfo(
                    NetworkPacketRegistry.getInstance().getPacketClass(
                            PacketDirection.SERVERBOUND, state, packetId
                    )
            );

            if (packetInfo == null) {
                /*;log.debug("❓ 未知上游包: id=0x{}, state={}", Integer.toHexString(packetId), state);*/
                // 未知包处理
                if (state == ProtocolState.HANDSHAKE || state == ProtocolState.STATUS || state == ProtocolState.LOGIN) {
                    log.warn("在 {} 阶段收到未知包 ID: {}, 关闭连接", state, packetId);
                    channelHandlerContext.writeAndFlush(new ClientboundDisconnectPacket("Unknown packet ID: " + packetId));
                    channelHandlerContext.channel().close();
                    byteBuf.skipBytes(byteBuf.readableBytes());
                    return;
                }

                // 游戏阶段：转发未知包到下游服务器
                byteBuf.resetReaderIndex(); // 重置到包开始位置
                ByteBuf originalPacket = byteBuf.readRetainedSlice(byteBuf.readableBytes());
                forwardUnknownPacket(channelHandlerContext, originalPacket, PacketDirection.SERVERBOUND);
                return;
            }

            // 包在当前协议阶段已实现
            IMinecraftPacket packet = NetworkPacketRegistry.getInstance().createPacket(PacketDirection.SERVERBOUND, state, packetId);
            if (packet == null) {
                log.error("已在注册表中找到包，但未能正确实例化包对象");
                return;
            }
            packet.read(byteBuf);
            list.add(packet);
        } catch (Exception e) {
            log.error("Error while decoding packet", e);
            channelHandlerContext.writeAndFlush(new ClientboundDisconnectPacket("Internal server error: " + e));
            channelHandlerContext.channel().close();
        }
    }
}
