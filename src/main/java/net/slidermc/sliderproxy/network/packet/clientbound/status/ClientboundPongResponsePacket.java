package net.slidermc.sliderproxy.network.packet.clientbound.status;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ClientboundPongResponsePacket implements IMinecraftPacket {
    private long timestamp;

    public ClientboundPongResponsePacket() {}

    public ClientboundPongResponsePacket(long timestamp) {
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
        return HandleResult.UNFORWARD;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
