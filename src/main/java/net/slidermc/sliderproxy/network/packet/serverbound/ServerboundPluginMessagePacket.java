package net.slidermc.sliderproxy.network.packet.serverbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ServerboundPluginMessagePacket implements IMinecraftPacket {
    private String identifier;
    private byte[] data;

    @Override
    public void read(ByteBuf byteBuf) {
        this.identifier = MinecraftProtocolHelper.readString(byteBuf);
        int remainingBytes = byteBuf.readableBytes();
        if (remainingBytes > 32767) {
            throw new RuntimeException("Plugin message data too large: " + remainingBytes + " bytes");
        }
        this.data = new byte[remainingBytes];
        byteBuf.readBytes(this.data);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, identifier);
        byteBuf.writeBytes(data);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        return HandleResult.FORWARD;
    }
}
