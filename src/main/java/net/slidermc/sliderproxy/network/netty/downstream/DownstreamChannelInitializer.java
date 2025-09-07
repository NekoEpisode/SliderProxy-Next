package net.slidermc.sliderproxy.network.netty.downstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.handler.downstream.DownstreamClientHandler;
import net.slidermc.sliderproxy.network.netty.FrameDecoder;
import net.slidermc.sliderproxy.network.netty.PacketEncoder;
import net.slidermc.sliderproxy.network.packet.PacketDirection;

public class DownstreamChannelInitializer extends ChannelInitializer<Channel> {
    private final ProxiedPlayer player;

    public DownstreamChannelInitializer(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.attr(PlayerConnection.KEY).set(player.getPlayerConnection());
        PlayerManager.getInstance().updateDownstreamChannel(player, ch);

        ch.pipeline().addLast("frame-decoder", new FrameDecoder());
        ch.pipeline().addLast("packet-decoder", new DownstreamPacketDecoder());
        ch.pipeline().addLast("packet-encoder", new PacketEncoder(PacketDirection.SERVERBOUND));
        ch.pipeline().addLast("client-handler", new DownstreamClientHandler());
    }
}
