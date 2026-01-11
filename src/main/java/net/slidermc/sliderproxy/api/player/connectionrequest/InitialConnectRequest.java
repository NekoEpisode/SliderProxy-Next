package net.slidermc.sliderproxy.api.player.connectionrequest;

import io.netty.channel.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlayerJoinEvent;
import net.slidermc.sliderproxy.api.event.events.ServerConnectedEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundLoginSuccessPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 首次连接请求 - 处理玩家初始登录流程
 * 
 * 流程：
 * 1. 先连接下游服务器并完成登录（到 CONFIGURATION 阶段）
 * 2. 下游成功后，再发送 LoginSuccess 给客户端
 * 3. 如果下游失败，断开客户端连接并显示错误消息
 */
public class InitialConnectRequest extends ConnectRequest {
    private static final Logger log = LoggerFactory.getLogger(InitialConnectRequest.class);

    public InitialConnectRequest(ProxiedPlayer player, ProxiedServer targetServer) {
        super(player, targetServer, ConnectReason.INITIAL_CONNECT);
    }

    @Override
    protected CompletableFuture<Void> preConnect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> connectToTarget() {
        // 创建下游连接
        player.createDownstreamClient(targetServer);

        // 先连接下游服务器
        return player.getDownstreamClient().connectAsync()
                .thenCompose(v -> player.getDownstreamClient().loginAsync())
                .thenAccept(v -> {
                    // 设置下游 channel 并更新 PlayerManager 映射
                    Channel downstreamChannel = player.getDownstreamClient().getChannel();
                    player.getPlayerConnection().setDownstreamChannel(downstreamChannel);
                    PlayerManager.getInstance().updateDownstreamChannel(player, downstreamChannel);

                    updatePlayerConnection();
                    
                    // 触发服务器连接成功事件
                    EventRegistry.callEvent(new ServerConnectedEvent(player, targetServer));
                    
                    log.info(TranslateManager.translate("sliderproxy.network.server.connection.success", player.getName(), targetServer.getName()));
                });
    }

    @Override
    protected CompletableFuture<Void> postConnect() {
        // 下游连接成功后，发送登录成功包给客户端
        return CompletableFuture.runAsync(() -> {
            player.getPlayerConnection().getUpstreamChannel().eventLoop().execute(() -> {
                try {
                    Channel ch = player.getPlayerConnection().getUpstreamChannel();

                    // 发送登录成功包
                    ClientboundLoginSuccessPacket loginSuccessPacket = new ClientboundLoginSuccessPacket(
                            player.getGameProfile().uuid(),
                            player.getGameProfile().name(),
                            List.of()
                    );
                    ch.writeAndFlush(loginSuccessPacket);

                    // 切换上游到配置状态
                    player.getPlayerConnection().setUpstreamOutboundProtocolState(ProtocolState.CONFIGURATION);
                    
                    // 触发玩家加入事件
                    EventRegistry.callEvent(new PlayerJoinEvent(player));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    @Override
    protected void handleConnectFailure(Throwable throwable) {
        // 断开下游客户端（如果有）
        if (player.getDownstreamClient() != null) {
            player.getDownstreamClient().disconnect();
        }

        log.error(TranslateManager.translate("sliderproxy.network.server.connection.failed", player.getName(), targetServer.getName(), throwable.getMessage()));

        player.kick(Component.text(Objects.requireNonNull(TranslateManager.translate("sliderproxy.network.server.connection.kick.default", targetServer.getName(), throwable.getMessage()))).color(NamedTextColor.RED));
    }
}