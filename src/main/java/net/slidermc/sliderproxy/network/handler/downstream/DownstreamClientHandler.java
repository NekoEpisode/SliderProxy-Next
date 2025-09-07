package net.slidermc.sliderproxy.network.handler.downstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownstreamClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DownstreamClientHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (!(msg instanceof IMinecraftPacket packet)) {
                super.channelRead(ctx, msg);
                return;
            }

            HandleResult result = packet.handle(ctx);
            if (result == HandleResult.FORWARD) {
                PlayerConnection connection = ctx.channel().attr(PlayerConnection.KEY).get();
                if (connection != null) {
                    Channel channel = connection.getUpstreamChannel();
                    // 确保在目标 Channel 的 EventLoop 中执行
                    if (channel.eventLoop().inEventLoop()) {
                        channel.writeAndFlush(packet);
                    } else {
                        channel.eventLoop().execute(() -> {
                            channel.writeAndFlush(packet);
                        });
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while handling downstream packet", e);
            ctx.channel().close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByDownstreamChannel(ctx.channel());
        if (player != null) {
            player.kick("下游服务器断开了连接");
        }
    }
}
