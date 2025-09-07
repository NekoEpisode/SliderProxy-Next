package net.slidermc.sliderproxy.network.packet.clientbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ClientboundStartConfigurationPacket implements IMinecraftPacket {
    public ClientboundStartConfigurationPacket() {}

    @Override
    public void read(ByteBuf byteBuf) {}

    @Override
    public void write(ByteBuf byteBuf) {}

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        return HandleResult.FORWARD;
    }
}
