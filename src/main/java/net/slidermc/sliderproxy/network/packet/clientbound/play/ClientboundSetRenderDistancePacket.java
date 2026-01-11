package net.slidermc.sliderproxy.network.packet.clientbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientboundSetRenderDistancePacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundSetRenderDistancePacket.class);
    private int distance;

    public ClientboundSetRenderDistancePacket() {}

    public ClientboundSetRenderDistancePacket(int distance) {
        this.distance = distance;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.distance = MinecraftProtocolHelper.readVarInt(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeVarInt(byteBuf, distance);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        return HandleResult.FORWARD;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }
}
