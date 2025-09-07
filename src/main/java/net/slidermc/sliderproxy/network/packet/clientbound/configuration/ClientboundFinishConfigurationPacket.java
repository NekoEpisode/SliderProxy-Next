package net.slidermc.sliderproxy.network.packet.clientbound.configuration;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientboundFinishConfigurationPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundFinishConfigurationPacket.class);

    public ClientboundFinishConfigurationPacket() {}

    @Override
    public void read(ByteBuf byteBuf) {}

    @Override
    public void write(ByteBuf byteBuf) {}

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) { // 127.0.0.1:25565 1.21.8
        return HandleResult.FORWARD;
    }
}
