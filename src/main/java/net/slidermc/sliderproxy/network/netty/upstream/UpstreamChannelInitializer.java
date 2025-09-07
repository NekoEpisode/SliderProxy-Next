package net.slidermc.sliderproxy.network.netty.upstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.handler.upstream.UpstreamServerHandler;
import net.slidermc.sliderproxy.network.netty.FrameDecoder;
import net.slidermc.sliderproxy.network.netty.PacketEncoder;
import net.slidermc.sliderproxy.network.packet.PacketDirection;

public class UpstreamChannelInitializer extends ChannelInitializer<Channel> {
    @Override
    protected void initChannel(Channel channel) throws Exception {
        PlayerConnection connection = new PlayerConnection(channel);
        connection.setUpstreamInboundProtocolState(ProtocolState.HANDSHAKE);
        connection.setUpstreamOutboundProtocolState(ProtocolState.HANDSHAKE);

        channel.pipeline().addLast("frame-decoder", new FrameDecoder());
        channel.pipeline().addLast("packet-decoder", new UpstreamPacketDecoder());
        channel.pipeline().addLast("packet-encoder", new PacketEncoder(PacketDirection.CLIENTBOUND));
        channel.pipeline().addLast("proxy-handler", new UpstreamServerHandler());
    }
}
