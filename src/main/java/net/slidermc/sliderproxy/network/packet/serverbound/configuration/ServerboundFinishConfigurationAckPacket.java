package net.slidermc.sliderproxy.network.packet.serverbound.configuration;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerboundFinishConfigurationAckPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundFinishConfigurationAckPacket.class);

    public ServerboundFinishConfigurationAckPacket() {}

    @Override
    public void read(ByteBuf byteBuf) {}

    @Override
    public void write(ByteBuf byteBuf) {}

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        PlayerConnection connection = ctx.channel().attr(PlayerConnection.KEY).get();

        if (connection.getDownstreamChannel() != null) {
            Channel downstreamChannel = connection.getDownstreamChannel();

            if (downstreamChannel.eventLoop().inEventLoop()) {
                downstreamChannel.writeAndFlush(this).addListener(future -> {
                    if (future.isSuccess()) {
                        updateProtocolStates(connection);
                    } else {
                        log.error("Failed to send FinishConfigurationAck to downstream", future.cause());
                        ctx.channel().close();
                    }
                });
            } else {
                downstreamChannel.eventLoop().execute(() -> {
                    downstreamChannel.writeAndFlush(this).addListener(future -> {
                        if (future.isSuccess()) {
                            updateProtocolStates(connection);
                        } else {
                            log.error("Failed to send FinishConfigurationAck to downstream", future.cause());
                            ctx.channel().close();
                        }
                    });
                });
            }
        } else {
            updateProtocolStates(connection);
        }

        return HandleResult.UNFORWARD;
    }

    private void updateProtocolStates(PlayerConnection connection) {
        connection.setUpstreamInboundProtocolState(ProtocolState.PLAY);
        connection.setUpstreamOutboundProtocolState(ProtocolState.PLAY);
        connection.setDownstreamInboundProtocolState(ProtocolState.PLAY);
        connection.setDownstreamOutboundProtocolState(ProtocolState.PLAY);
    }
}
