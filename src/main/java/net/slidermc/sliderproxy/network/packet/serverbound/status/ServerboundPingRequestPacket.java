package net.slidermc.sliderproxy.network.packet.serverbound.status;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.status.ClientboundPongResponsePacket;

public class ServerboundPingRequestPacket implements IMinecraftPacket {
    private long timestamp;

    public ServerboundPingRequestPacket() {}

    public ServerboundPingRequestPacket(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.timestamp = byteBuf.readLong();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeLong(this.timestamp);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        // 返回Pong包
        ctx.channel().writeAndFlush(new ClientboundPongResponsePacket(this.timestamp));
        return HandleResult.UNFORWARD;
    }
}
