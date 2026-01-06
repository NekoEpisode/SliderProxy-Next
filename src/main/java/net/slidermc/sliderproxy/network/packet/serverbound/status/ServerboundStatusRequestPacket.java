package net.slidermc.sliderproxy.network.packet.serverbound.status;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.RunningData;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.status.ClientboundStatusResponsePacket;

import java.util.ArrayList;
import java.util.List;

public class ServerboundStatusRequestPacket implements IMinecraftPacket {
    public ServerboundStatusRequestPacket() {}

    @Override
    public void read(ByteBuf byteBuf) {}

    @Override
    public void write(ByteBuf byteBuf) {}

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        List<ClientboundStatusResponsePacket.PlayerInfo> playerInfos = new ArrayList<>();

        for (ProxiedPlayer player : PlayerManager.getInstance().getAllPlayers()) {
            if (player.getClientInformation().isAllowServerListings()) {
                playerInfos.add(new ClientboundStatusResponsePacket.PlayerInfo(player.getGameProfile().uuid(), player.getName()));
            }
        }

        ClientboundStatusResponsePacket responsePacket = new ClientboundStatusResponsePacket(
                RunningData.configuration.getInt("proxy.max-players"),
                PlayerManager.getInstance().getPlayerCount(),
                playerInfos,
                RunningData.configuration.getString("proxy.motd"),
                false,
                "SliderProxy 1.21.8",
                772
        );
        ctx.writeAndFlush(responsePacket);
        return HandleResult.UNFORWARD;
    }
}
