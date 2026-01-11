package net.slidermc.sliderproxy.api.player.connectionrequest;

import io.netty.channel.Channel;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.ServerConnectedEvent;
import net.slidermc.sliderproxy.api.event.events.ServerSwitchEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.packet.clientbound.play.ClientboundStartConfigurationPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 服务器切换请求 - 处理玩家在服务器间的切换
 * 
 * 新流程（下游优先）：
 * 1. 先创建新的下游连接并完成登录（到 CONFIGURATION 阶段）
 * 2. 下游成功后，再让客户端进入 Configuration
 * 3. 如果下游失败，客户端保持在 PLAY 状态，发送错误消息
 * 4. 旧的下游连接在新连接成功后才断开
 */
public class ServerSwitchRequest extends ConnectRequest {
    private static final Logger log = LoggerFactory.getLogger(ServerSwitchRequest.class);
    private CompletableFuture<Void> configurationAckFuture;
    
    // 新的下游客户端（在切换成功前保持旧连接）
    private MinecraftNettyClient newDownstreamClient;

    public ServerSwitchRequest(ProxiedPlayer player, ProxiedServer targetServer) {
        super(player, targetServer, ConnectReason.SERVER_SWITCH);
    }

    @Override
    protected CompletableFuture<Void> preConnect() {
        // 触发服务器切换事件
        ProxiedServer from = player.getConnectedServer();
        ServerSwitchEvent event = new ServerSwitchEvent(player, from, targetServer);
        EventRegistry.callEvent(event);
        
        // 如果事件被取消，返回失败的 Future
        if (event.isCancelled()) {
            return CompletableFuture.failedFuture(new RuntimeException("Server switch cancelled by event"));
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> connectToTarget() {
        // 保存旧的下游客户端引用
        MinecraftNettyClient oldClient = player.getDownstreamClient();

        // 创建新的下游客户端（不断开旧连接）
        newDownstreamClient = new MinecraftNettyClient(targetServer.getAddress(), player);

        // 第一步：先连接新的下游服务器
        return newDownstreamClient.connectAsync()
                .thenCompose(v -> newDownstreamClient.loginAsync())
                .thenCompose(v -> {
                    // 暂停新下游的自动读取
                    newDownstreamClient.getChannel().config().setAutoRead(false);

                    configurationAckFuture = new CompletableFuture<>();
                    player.setStartConfigurationAckFuture(configurationAckFuture);

                    return CompletableFuture.runAsync(() -> {
                        player.getPlayerConnection().getUpstreamChannel().eventLoop().execute(() -> {
                            try {
                                // 断开旧的下游连接
                                if (oldClient != null) {
                                    oldClient.disconnect();
                                }

                                // 更新 player 的 downstreamClient 引用
                                player.setDownstreamClient(newDownstreamClient);

                                // 发送开始配置包给客户端
                                player.getPlayerConnection().getUpstreamChannel()
                                        .writeAndFlush(new ClientboundStartConfigurationPacket());
                                player.getPlayerConnection().setUpstreamOutboundProtocolState(ProtocolState.CONFIGURATION);

                            } catch (Exception e) {
                                configurationAckFuture.completeExceptionally(e);
                            }
                        });
                    });
                })
                .thenCompose(v -> configurationAckFuture)
                .thenAccept(v -> {
                    // 客户端已进入 Configuration，设置新的下游 channel 并恢复读取
                    Channel newChannel = newDownstreamClient.getChannel();
                    player.getPlayerConnection().setDownstreamChannel(newChannel);
                    PlayerManager.getInstance().updateDownstreamChannel(player, newChannel);

                    // 恢复新下游的自动读取
                    newChannel.config().setAutoRead(true);

                    updatePlayerConnection();
                    
                    // 触发服务器连接成功事件
                    EventRegistry.callEvent(new ServerConnectedEvent(player, targetServer));
                    
                    log.info(TranslateManager.translate("sliderproxy.network.server.switch.success", player.getName(), targetServer.getName()));
                });
    }

    @Override
    protected CompletableFuture<Void> postConnect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected void handleConnectFailure(Throwable throwable) {
        // 清理新的下游客户端（如果有）
        if (newDownstreamClient != null) {
            // 确保恢复 autoRead（虽然要断开了，但保持一致性）
            if (newDownstreamClient.getChannel() != null) {
                newDownstreamClient.getChannel().config().setAutoRead(true);
            }
            newDownstreamClient.disconnect();
            newDownstreamClient = null;
        }

        // 完成配置确认 Future（如果有）
        if (configurationAckFuture != null && !configurationAckFuture.isDone()) {
            configurationAckFuture.completeExceptionally(throwable);
        }

        log.error(TranslateManager.translate("sliderproxy.network.server.switch.failed", player.getName(), targetServer.getName(), throwable.getMessage()));

        // 客户端还在 PLAY 状态，发送错误消息即可
        player.sendMessage(TranslateManager.translate("sliderproxy.network.server.switch.kick", targetServer.getName()));
        player.sendMessage(TranslateManager.translate("sliderproxy.network.server.switch.kick.error", throwable.getMessage()));
    }

    /**
     * 当收到配置确认时调用此方法
     */
    public void onConfigurationAck() {
        if (configurationAckFuture != null && !configurationAckFuture.isDone()) {
            configurationAckFuture.complete(null);
        }
    }
}