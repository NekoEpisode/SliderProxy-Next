package net.slidermc.sliderproxy.api.player;

import net.kyori.adventure.text.Component;
import net.slidermc.sliderproxy.api.command.CommandSender;
import net.slidermc.sliderproxy.api.player.connectionrequest.ConnectRequest;
import net.slidermc.sliderproxy.api.player.connectionrequest.InitialConnectRequest;
import net.slidermc.sliderproxy.api.player.connectionrequest.ServerSwitchRequest;
import net.slidermc.sliderproxy.api.player.data.ClientInformation;
import net.slidermc.sliderproxy.api.player.data.GameProfile;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.play.ClientboundSystemChatPacket;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ProxiedPlayer implements CommandSender {
    private static final Logger log = LoggerFactory.getLogger(ProxiedPlayer.class);

    private final GameProfile gameProfile;
    private final ClientInformation clientInformation = new ClientInformation();
    private final PlayerConnection playerConnection;
    private volatile MinecraftNettyClient downstreamClient = null;
    private volatile ProxiedServer connectedServer = null;

    // 用于管理连接请求的状态
    private final AtomicReference<ConnectRequest> currentConnectRequest = new AtomicReference<>();
    private volatile CompletableFuture<Void> startConfigurationAckFuture;

    public ProxiedPlayer(GameProfile gameProfile, PlayerConnection playerConnection) {
        this.gameProfile = gameProfile;
        this.playerConnection = playerConnection;
    }

    @Override
    public void sendMessage(String message) {
        sendMessage(message, false);
    }

    public void sendMessage(String message, boolean actionbar) {
        sendMessage(Component.text(message), actionbar);
    }

    public void sendMessage(Component component) {
        sendMessage(component, false);
    }

    public void sendMessage(Component component, boolean actionbar) {
        sendPacket(new ClientboundSystemChatPacket(component, actionbar));
    }

    /**
     * 连接到服务器 - 统一的连接入口
     */
    public CompletableFuture<Void> connectTo(ProxiedServer server) {
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("目标服务器不能为空"));
        }

        // 确定连接类型
        ConnectRequest request;
        if (connectedServer == null) {
            // 首次连接
            request = new InitialConnectRequest(this, server);
        } else {
            // 服务器切换
            if (connectedServer.equals(server)) {
                return CompletableFuture.completedFuture(null);
            }
            request = new ServerSwitchRequest(this, server);
        }

        // 设置当前连接请求
        currentConnectRequest.set(request);

        // 执行连接请求
        return request.execute().whenComplete((result, throwable) -> {
            // 清除当前连接请求
            currentConnectRequest.compareAndSet(request, null);
        });
    }

    /**
     * 创建下游客户端
     */
    public void createDownstreamClient(ProxiedServer server) {
        if (downstreamClient != null) {
            downstreamClient.disconnect();
        }
        downstreamClient = new MinecraftNettyClient(server.getAddress(), this);
    }

    /**
     * 处理配置确认 - 用于服务器切换流程
     */
    public void handleConfigurationAck() {
        ConnectRequest request = currentConnectRequest.get();
        if (request instanceof ServerSwitchRequest switchRequest) {
            switchRequest.onConfigurationAck();
        }
    }

    /**
     * 检查是否正在切换服务器
     */
    public boolean isSwitchingServer() {
        ConnectRequest request = currentConnectRequest.get();
        return request != null && request.getReason() == ConnectRequest.ConnectReason.SERVER_SWITCH;
    }

    public void sendPacket(IMinecraftPacket packet) {
        playerConnection.getUpstreamChannel().writeAndFlush(packet);
    }

    public void kick(String reason) {
        // 关闭连接
        playerConnection.getUpstreamChannel().close();
        if (playerConnection.getDownstreamChannel() != null) {
            playerConnection.getDownstreamChannel().close();
            playerConnection.setDownstreamChannel(null);
        }

        // 断开下游客户端
        if (downstreamClient != null) {
            downstreamClient.disconnect();
        }

        // 从服务器移除玩家
        if (connectedServer != null) {
            connectedServer.getConnectedPlayers().remove(this);
        }
    }

    // Getters and setters
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

    public void setDownstreamClient(MinecraftNettyClient downstreamClient) {
        this.downstreamClient = downstreamClient;
    }

    public CompletableFuture<Void> getStartConfigurationAckFuture() {
        return startConfigurationAckFuture;
    }

    public void setStartConfigurationAckFuture(CompletableFuture<Void> startConfigurationAckFuture) {
        this.startConfigurationAckFuture = startConfigurationAckFuture;
    }

    public ConnectRequest getCurrentConnectRequest() {
        return currentConnectRequest.get();
    }

    public ClientInformation getClientInformation() {
        return clientInformation;
    }
}