package net.slidermc.sliderproxy.network.netty.downstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.handler.downstream.DownstreamClientHandler;
import net.slidermc.sliderproxy.network.netty.FrameDecoder;
import net.slidermc.sliderproxy.network.netty.PacketEncoder;
import net.slidermc.sliderproxy.network.packet.PacketDirection;

public class DownstreamChannelInitializer extends ChannelInitializer<Channel> {
    private final ProxiedPlayer player;
    private final MinecraftNettyClient client;

    public DownstreamChannelInitializer(ProxiedPlayer player, MinecraftNettyClient client) {
        this.player = player;
        this.client = client;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        // 绑定 PlayerConnection 和 MinecraftNettyClient 到 Channel
        ch.attr(PlayerConnection.KEY).set(player.getPlayerConnection());
        ch.attr(MinecraftNettyClient.KEY).set(client);
        
        // 注意：不在这里更新 PlayerManager 的映射
        // 由调用方在适当时机（连接成功后）调用 PlayerManager.updateDownstreamChannel

        ch.pipeline().addLast("frame-decoder", new FrameDecoder());
        ch.pipeline().addLast("packet-decoder", new DownstreamPacketDecoder());
        ch.pipeline().addLast("packet-encoder", new PacketEncoder(PacketDirection.SERVERBOUND));
        ch.pipeline().addLast("client-handler", new DownstreamClientHandler());
    }
}
