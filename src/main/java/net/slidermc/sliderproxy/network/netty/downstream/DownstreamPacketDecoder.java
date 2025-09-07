package net.slidermc.sliderproxy.network.netty.downstream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.NetworkPacketRegistry;
import net.slidermc.sliderproxy.network.packet.PacketDirection;
import net.slidermc.sliderproxy.network.packet.PacketInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.slidermc.sliderproxy.network.packet.PacketForwarder.forwardUnknownPacket;

public class DownstreamPacketDecoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(DownstreamPacketDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        try {
            byteBuf.markReaderIndex();

            int packetId = MinecraftProtocolHelper.readVarInt(byteBuf);
            ProxiedPlayer player = PlayerManager.getInstance().getPlayerByDownstreamChannel(channelHandlerContext.channel());
            if (player == null) {
                log.warn("未找到与通道关联的玩家");
                channelHandlerContext.channel().close();
                return;
            }
            PlayerConnection playerConnection = player.getPlayerConnection();
            ProtocolState state = playerConnection.getDownstreamInboundProtocolState();

            /*log.debug("📥 收到下游包: id=0x{}, state={}, player={}, remoteAddress={}",
                    Integer.toHexString(packetId),
                    state,
                    player.getName(),
                    channelHandlerContext.channel().remoteAddress());*/

            PacketInfo packetInfo = NetworkPacketRegistry.getInstance().getPacketInfo(
                    NetworkPacketRegistry.getInstance().getPacketClass(
                            PacketDirection.CLIENTBOUND, state, packetId
                    )
            );

            if (packetInfo == null) {
                /*log.debug("❓ 未知下游包: id=0x{}, state={}", Integer.toHexString(packetId), state);*/
                // 未知包处理
                if (state == ProtocolState.HANDSHAKE || state == ProtocolState.STATUS || state == ProtocolState.LOGIN) {
                    log.warn("在 {} 阶段收到未知包 ID: {}, 关闭连接", state, packetId);
                    channelHandlerContext.channel().close();
                    byteBuf.skipBytes(byteBuf.readableBytes());
                    return;
                }

                // 游戏阶段：转发未知包到上游客户端
                byteBuf.resetReaderIndex(); // 重置到包开始位置
                ByteBuf originalPacket = byteBuf.readRetainedSlice(byteBuf.readableBytes());
                forwardUnknownPacket(channelHandlerContext, originalPacket, PacketDirection.CLIENTBOUND);
                return;
            }

            // 包在当前协议阶段已实现
            IMinecraftPacket packet = NetworkPacketRegistry.getInstance().createPacket(PacketDirection.CLIENTBOUND, state, packetId);
            if (packet == null) {
                log.error("已在注册表中找到包，但未能正确实例化包对象");
                return;
            }
            packet.read(byteBuf);
            list.add(packet);
        } catch (Exception e) {
            log.error("Error while decoding downstream packet", e);
            channelHandlerContext.channel().close();
        }
    }
}
