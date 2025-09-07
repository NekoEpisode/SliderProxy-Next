package net.slidermc.sliderproxy.api.player;

import net.slidermc.sliderproxy.api.command.CommandSender;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.play.ClientboundStartConfigurationPacket;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ProxiedPlayer implements CommandSender {
    private static final Logger log = LoggerFactory.getLogger(ProxiedPlayer.class);
    private final GameProfile gameProfile;
    private final PlayerConnection playerConnection;
    private MinecraftNettyClient downstreamClient = null;
    private ProxiedServer connectedServer = null;

    public ProxiedPlayer(GameProfile gameProfile, PlayerConnection playerConnection) {
        this.gameProfile = gameProfile;
        this.playerConnection = playerConnection;
    }

    @Override
    public void sendMessage(String message) {
        // TODO: 等待实现SystemChat包
    }

    public CompletableFuture<Void> connectTo(ProxiedServer server) {
        if (connectedServer != null) {
            connectedServer.getConnectedPlayers().remove(this);
            playerConnection.getUpstreamChannel().writeAndFlush(new ClientboundStartConfigurationPacket());
        }
        if (downstreamClient != null) downstreamClient.disconnect();

        downstreamClient = new MinecraftNettyClient(server.getAddress(), this);
        return downstreamClient.connectAsync()
                .thenCompose(v -> downstreamClient.loginAsync())
                .thenAccept(v -> {
                    log.info("下游服务器连接成功，玩家: {}", gameProfile.name());
                    this.connectedServer = server;
                    server.getConnectedPlayers().add(this);
                })
                .exceptionally(throwable -> {
                    log.error("下游服务器连接失败: {}", throwable.getMessage());
                    kick("下游服务器连接失败");
                    return null;
                });
    }

    public void sendPacket(IMinecraftPacket packet) {
        playerConnection.getUpstreamChannel().writeAndFlush(packet);
    }

    public void kick(String reason) {
        // playerConnection.getUpstreamChannel().writeAndFlush(new ClientboundDisconnectPacket(reason));
        playerConnection.getUpstreamChannel().close();
        if (playerConnection.getDownstreamChannel() != null) {
            playerConnection.getDownstreamChannel().close();
            playerConnection.setDownstreamChannel(null);
        }
    }

    @Override
    public String getName() {
        return gameProfile.name();
    }

    public GameProfile getGameProfile() {
        return gameProfile;
    }

    public PlayerConnection getPlayerConnection() {
        return playerConnection;
    }

    public @Nullable ProxiedServer getConnectedServer() {
        return connectedServer;
    }

    public void setConnectedServer(ProxiedServer connectedServer) {
        this.connectedServer = connectedServer;
    }

    public MinecraftNettyClient getDownstreamClient() {
        return downstreamClient;
    }
}
