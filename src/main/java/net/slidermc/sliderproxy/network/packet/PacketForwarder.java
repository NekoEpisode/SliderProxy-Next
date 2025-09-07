package net.slidermc.sliderproxy.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketForwarder {

    private static final Logger log = LoggerFactory.getLogger(PacketForwarder.class);

    /**
     * 转发未知包，自动处理压缩和长度重计算
     */
    public static void forwardUnknownPacket(ChannelHandlerContext ctx, ByteBuf originalPacket, PacketDirection direction) {
        try {
            Channel targetChannel = getTargetChannel(ctx, direction);
            if (targetChannel == null || !targetChannel.isActive()) {
                originalPacket.release();
                return;
            }

            // 重新构建完整的Minecraft包（带长度前缀）
            ByteBuf newPacket = ctx.alloc().buffer();
            try {
                // 写入VarInt长度前缀
                MinecraftProtocolHelper.writeVarInt(newPacket, originalPacket.readableBytes());
                // 写入原始包内容
                newPacket.writeBytes(originalPacket);

                // 发送到目标通道（会自动经过压缩编码器等处理）
                targetChannel.writeAndFlush(newPacket.retain());
            } finally {
                newPacket.release();
                originalPacket.release();
            }

        } catch (Exception e) {
            log.error("转发未知包失败", e);
            originalPacket.release();
        }
    }

    private static Channel getTargetChannel(ChannelHandlerContext ctx, PacketDirection direction) {
        PlayerConnection connection = ctx.channel().attr(PlayerConnection.KEY).get();
        if (connection == null) return null;

        return switch (direction) {
            case SERVERBOUND -> connection.getDownstreamChannel(); // 客户端→代理，转发到服务器
            case CLIENTBOUND -> connection.getUpstreamChannel();   // 服务器→代理，转发到客户端
        };
    }

    /**
     * 直接转发原始ByteBuf（不重新包装）
     */
    public static void forwardRawPacket(ChannelHandlerContext ctx, ByteBuf rawPacket, PacketDirection direction) {
        try {
            Channel targetChannel = getTargetChannel(ctx, direction);
            if (targetChannel != null && targetChannel.isActive()) {
                targetChannel.writeAndFlush(rawPacket.retain());
            }
        } finally {
            rawPacket.release();
        }
    }
}