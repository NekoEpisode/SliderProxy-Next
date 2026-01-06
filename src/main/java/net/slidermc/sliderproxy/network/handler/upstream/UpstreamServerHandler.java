package net.slidermc.sliderproxy.network.handler.upstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpstreamServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(UpstreamServerHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (!(msg instanceof IMinecraftPacket packet)) {
                super.channelRead(ctx, msg);
                return;
            }

            HandleResult result = packet.handle(ctx);
            if (result == HandleResult.FORWARD) {
                PlayerConnection connection = ctx.channel().attr(PlayerConnection.KEY).get();
                if (connection != null) {
                    Channel channel = connection.getDownstreamChannel();
                    if (channel != null) {
                        if (channel.eventLoop().inEventLoop()) {
                            channel.writeAndFlush(packet);
                        } else {
                            channel.eventLoop().execute(() -> {
                                channel.writeAndFlush(packet);
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while handling packet", e);
            ctx.channel().close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player != null) {
            log.info(TranslateManager.translate("sliderproxy.network.connection.disconnected", player.getName()));
            if (player.getDownstreamClient() != null) {
                player.getDownstreamClient().disconnect();
            }
            PlayerManager.getInstance().unregisterPlayer(player);
        }
    }
}
