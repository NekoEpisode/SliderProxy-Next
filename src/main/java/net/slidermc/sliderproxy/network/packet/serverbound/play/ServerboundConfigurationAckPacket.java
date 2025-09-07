package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

import java.util.Objects;

public class ServerboundConfigurationAckPacket implements IMinecraftPacket {
    public ServerboundConfigurationAckPacket() {}

    @Override
    public void read(ByteBuf byteBuf) {}

    @Override
    public void write(ByteBuf byteBuf) {}

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        System.out.println("handle");
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player != null) {
            System.out.println("player not null");
            if (!player.getSwitchServerQueue().isEmpty()) {
                System.out.println("switch!");
                // 切换服务器
                player.getPlayerConnection().setUpstreamInboundProtocolState(ProtocolState.CONFIGURATION);
                if (player.getStartConfigurationAckFuture() != null) {
                    player.getStartConfigurationAckFuture().complete(null);
                    player.setStartConfigurationAckFuture(null);
                }
                return HandleResult.UNFORWARD;
            }
        }
        return HandleResult.FORWARD;
    }
}
