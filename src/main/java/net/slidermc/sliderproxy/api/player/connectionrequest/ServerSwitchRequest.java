package net.slidermc.sliderproxy.api.player.connectionrequest;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.packet.clientbound.play.ClientboundStartConfigurationPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 服务器切换请求 - 处理玩家在服务器间的切换
 */
public class ServerSwitchRequest extends ConnectRequest {
    private static final Logger log = LoggerFactory.getLogger(ServerSwitchRequest.class);
    private CompletableFuture<Void> configurationAckFuture;

    public ServerSwitchRequest(ProxiedPlayer player, ProxiedServer targetServer) {
        super(player, targetServer, ConnectReason.SERVER_SWITCH);
    }

    @Override
    protected CompletableFuture<Void> preConnect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> connectToTarget() {
        if (player.getDownstreamClient() != null) {
            player.getDownstreamClient().disconnect();
        }

        configurationAckFuture = new CompletableFuture<>();
        player.setStartConfigurationAckFuture(configurationAckFuture);

        return CompletableFuture.runAsync(() -> {
                    player.getPlayerConnection().getUpstreamChannel().eventLoop().execute(() -> {
                        try {
                            log.debug("发送开始配置包: 玩家={}", player.getName());

                            player.getPlayerConnection().getUpstreamChannel()
                                    .writeAndFlush(new ClientboundStartConfigurationPacket());

                            player.getPlayerConnection().setUpstreamOutboundProtocolState(ProtocolState.CONFIGURATION);

                        } catch (Exception e) {
                            log.error("发送配置包失败: 玩家={}", player.getName(), e);
                            configurationAckFuture.completeExceptionally(e);
                        }
                    });
                }).thenCompose(v -> {
                    log.debug("等待配置确认: 玩家={}", player.getName());
                    return configurationAckFuture;
                }).thenCompose(v -> {
                    log.debug("收到配置确认，重置协议状态并连接新服务器: 玩家={}", player.getName());
                    player.getPlayerConnection().setDownstreamOutboundProtocolState(ProtocolState.HANDSHAKE);
                    player.getPlayerConnection().setDownstreamInboundProtocolState(ProtocolState.HANDSHAKE);

                    player.createDownstreamClient(targetServer);
                    return player.getDownstreamClient().connectAsync();
                }).thenCompose(v -> player.getDownstreamClient().loginAsync())
                .thenAccept(v -> {
                    log.info("成功连接到目标服务器: 玩家={}, 服务器={}",
                            player.getName(), targetServer.getName());

                    updatePlayerConnection();
                });
    }

    @Override
    protected CompletableFuture<Void> postConnect() {
        log.info("服务器切换完成: 玩家={}, 新服务器={}",
                player.getName(), targetServer.getName());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected void handleConnectFailure(Throwable throwable) {
        if (configurationAckFuture != null && !configurationAckFuture.isDone()) {
            configurationAckFuture.completeExceptionally(throwable);
        }
        super.handleConnectFailure(throwable);
    }

    /**
     * 当收到配置确认时调用此方法
     */
    public void onConfigurationAck() {
        if (configurationAckFuture != null && !configurationAckFuture.isDone()) {
            log.debug("收到配置确认: 玩家={}", player.getName());
            configurationAckFuture.complete(null);
        }
    }
}