package net.slidermc.sliderproxy.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface IMinecraftPacket {
    void read(ByteBuf byteBuf);
    void write(ByteBuf byteBuf);
    HandleResult handle(ChannelHandlerContext ctx);
}
