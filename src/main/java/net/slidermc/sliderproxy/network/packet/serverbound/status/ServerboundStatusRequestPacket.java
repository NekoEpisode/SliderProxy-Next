package net.slidermc.sliderproxy.network.packet.serverbound.status;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.status.ClientboundStatusResponsePacket;

import java.util.List;

public class ServerboundStatusRequestPacket implements IMinecraftPacket {
    public ServerboundStatusRequestPacket() {}

    @Override
    public void read(ByteBuf byteBuf) {}

    @Override
    public void write(ByteBuf byteBuf) {}

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ClientboundStatusResponsePacket responsePacket = new ClientboundStatusResponsePacket(20, 0, List.of(), "SliderProxy - Next!", false, "1.21.8", 772);
        ctx.writeAndFlush(responsePacket);
        return HandleResult.UNFORWARD;
    }
}
