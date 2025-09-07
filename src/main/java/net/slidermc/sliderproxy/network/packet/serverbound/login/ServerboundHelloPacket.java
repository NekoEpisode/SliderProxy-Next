package net.slidermc.sliderproxy.network.packet.serverbound.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.RunningData;
import net.slidermc.sliderproxy.api.player.GameProfile;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.api.server.ServerManager;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundLoginSuccessPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public class ServerboundHelloPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundHelloPacket.class);
    private String username;
    private UUID uuid;

    public ServerboundHelloPacket() {}

    public ServerboundHelloPacket(String username, UUID uuid) {
        this.username = username;
        this.uuid = uuid;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.username = MinecraftProtocolHelper.readString(byteBuf);
        this.uuid = MinecraftProtocolHelper.readUUID(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, this.username);
        MinecraftProtocolHelper.writeUUID(byteBuf, this.uuid);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        PlayerConnection connection = ctx.channel().attr(PlayerConnection.KEY).get();

        ProxiedPlayer player = new ProxiedPlayer(new GameProfile(username, uuid), connection);
        PlayerManager.getInstance().registerPlayer(player);

        String defaultServerName = RunningData.configuration.getString("proxy.default-server", "lobby");
        ProxiedServer defaultServer = ServerManager.getInstance().getServer(defaultServerName);

        player.connectTo(defaultServer).thenRun(() -> {
            connection.getUpstreamChannel().eventLoop().execute(() -> {
                ClientboundLoginSuccessPacket loginSuccessPacket = new ClientboundLoginSuccessPacket(this.uuid, this.username, List.of());
                connection.getUpstreamChannel().writeAndFlush(loginSuccessPacket);

                connection.setUpstreamOutboundProtocolState(ProtocolState.CONFIGURATION);
            });
        });

        return HandleResult.UNFORWARD;
    }
}
