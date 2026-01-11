package net.slidermc.sliderproxy.network.packet.serverbound.configuration;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
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
                        updateProtocolStates(connection, downstreamChannel);
                    } else {
                        log.error("Failed to send FinishConfigurationAck to downstream", future.cause());
                        ctx.channel().close();
                    }
                });
            } else {
                downstreamChannel.eventLoop().execute(() -> {
                    downstreamChannel.writeAndFlush(this).addListener(future -> {
                        if (future.isSuccess()) {
                            updateProtocolStates(connection, downstreamChannel);
                        } else {
                            log.error("Failed to send FinishConfigurationAck to downstream", future.cause());
                            ctx.channel().close();
                        }
                    });
                });
            }
        } else {
            // 没有下游连接时，只更新上游状态
            connection.setUpstreamInboundProtocolState(ProtocolState.PLAY);
            connection.setUpstreamOutboundProtocolState(ProtocolState.PLAY);
        }

        return HandleResult.UNFORWARD;
    }

    private void updateProtocolStates(PlayerConnection connection, Channel downstreamChannel) {
        // 触发 PlayerConfigurationCompleteEvent
        net.slidermc.sliderproxy.api.player.ProxiedPlayer player = 
            net.slidermc.sliderproxy.api.player.PlayerManager.getInstance().getPlayerByUpstreamChannel(connection.getUpstreamChannel());
        if (player != null) {
            net.slidermc.sliderproxy.api.event.events.PlayerConfigurationCompleteEvent configCompleteEvent = 
                new net.slidermc.sliderproxy.api.event.events.PlayerConfigurationCompleteEvent(player);
            net.slidermc.sliderproxy.api.event.EventRegistry.callEvent(configCompleteEvent);
            
            // 触发 PlayerLoginCompleteEvent
            net.slidermc.sliderproxy.api.event.events.PlayerLoginCompleteEvent loginCompleteEvent = 
                new net.slidermc.sliderproxy.api.event.events.PlayerLoginCompleteEvent(player);
            net.slidermc.sliderproxy.api.event.EventRegistry.callEvent(loginCompleteEvent);
        }
        
        // 更新上游状态（PlayerConnection 管理）
        connection.setUpstreamInboundProtocolState(ProtocolState.PLAY);
        connection.setUpstreamOutboundProtocolState(ProtocolState.PLAY);
        
        // 更新下游状态（MinecraftNettyClient 自主管理）
        MinecraftNettyClient client = MinecraftNettyClient.fromChannel(downstreamChannel);
        if (client != null) {
            client.setInboundProtocolState(ProtocolState.PLAY);
            client.setOutboundProtocolState(ProtocolState.PLAY);
        }
    }
}
