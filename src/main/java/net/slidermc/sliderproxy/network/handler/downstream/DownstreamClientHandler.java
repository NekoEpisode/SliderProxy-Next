package net.slidermc.sliderproxy.network.handler.downstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;
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
                    // 检查当前 channel 是否是活跃的下游 channel
                    // 如果不是（比如正在切换服务器时的新连接），不转发包
                    if (connection.getDownstreamChannel() != ctx.channel()) {
                        log.debug("跳过转发：当前 channel 不是活跃的下游 channel");
                        return;
                    }
                    
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
        // 从 channel 获取绑定的 MinecraftNettyClient
        MinecraftNettyClient client = MinecraftNettyClient.fromChannel(ctx.channel());
        if (client == null) {
            return;
        }
        
        ProxiedPlayer player = client.getBindPlayer();
        if (player == null) {
            return;
        }
        
        // 检查是否正在切换服务器
        if (player.isSwitchingServer()) {
            log.debug("Downstream channel for {} became inactive during a server switch (expected).", player.getName());
            return;
        }
        
        // 检查断开的是否是当前活跃的下游连接
        if (player.getPlayerConnection().getDownstreamChannel() != ctx.channel()) {
            log.debug("非活跃下游 channel 断开，忽略: 玩家={}", player.getName());
            return;
        }

        player.kick(TranslateManager.translate("sliderproxy.network.connection.kick.downstream"));
    }
}