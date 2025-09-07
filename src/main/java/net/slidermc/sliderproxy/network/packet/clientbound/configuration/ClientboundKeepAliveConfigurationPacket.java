package net.slidermc.sliderproxy.network.packet.clientbound.configuration;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ClientboundKeepAliveConfigurationPacket implements IMinecraftPacket {
    private long keepAliveId;

    public ClientboundKeepAliveConfigurationPacket() {}

    public ClientboundKeepAliveConfigurationPacket(long keepAliveId) {
        this.keepAliveId = keepAliveId;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.keepAliveId = byteBuf.readLong();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeLong(this.keepAliveId);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        return HandleResult.UNFORWARD;
    }
}
