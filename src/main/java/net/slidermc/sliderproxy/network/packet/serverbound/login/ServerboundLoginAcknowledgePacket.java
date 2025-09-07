package net.slidermc.sliderproxy.network.packet.serverbound.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerboundLoginAcknowledgePacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundLoginAcknowledgePacket.class);

    public ServerboundLoginAcknowledgePacket() {}

    @Override
    public void read(ByteBuf byteBuf) {}

    @Override
    public void write(ByteBuf byteBuf) {}

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        PlayerConnection connection = ctx.channel().attr(PlayerConnection.KEY).get();
        connection.setUpstreamInboundProtocolState(ProtocolState.CONFIGURATION);
        return HandleResult.UNFORWARD;
    }
}
