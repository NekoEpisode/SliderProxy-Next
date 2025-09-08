package net.slidermc.sliderproxy.api.player.connectionrequest;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 连接请求抽象类，定义了连接流程的基本步骤
 */
public abstract class ConnectRequest {
    private static final Logger log = LoggerFactory.getLogger(ConnectRequest.class);

    protected final ProxiedPlayer player;
    protected final ProxiedServer targetServer;
    protected final ConnectReason reason;

    public ConnectRequest(ProxiedPlayer player, ProxiedServer targetServer, ConnectReason reason) {
        this.player = player;
        this.targetServer = targetServer;
        this.reason = reason;
    }

    /**
     * 执行连接请求
     */
    public final CompletableFuture<Void> execute() {
        log.info("开始执行连接请求: 玩家={}, 目标服务器={}, 原因={}",
                player.getName(), targetServer.getName(), reason);

        return preConnect()
                .thenCompose(v -> connectToTarget())
                .thenCompose(v -> postConnect())
                .exceptionally(throwable -> {
                    handleConnectFailure(throwable);
                    return null;
                });
    }

    /**
     * 连接前的准备工作
     */
    protected abstract CompletableFuture<Void> preConnect();

    /**
     * 连接到目标服务器
     */
    protected CompletableFuture<Void> connectToTarget() {
        // 断开现有的下游连接
        if (player.getDownstreamClient() != null) {
            player.getDownstreamClient().disconnect();
        }

        // 创建新的下游连接
        player.createDownstreamClient(targetServer);
        return player.getDownstreamClient().connectAsync()
                .thenCompose(v -> player.getDownstreamClient().loginAsync())
                .thenAccept(v -> {
                    log.info("成功连接到目标服务器: 玩家={}, 服务器={}",
                            player.getName(), targetServer.getName());

                    // 更新玩家的连接状态
                    updatePlayerConnection();
                });
    }

    /**
     * 连接后的处理工作
     */
    protected abstract CompletableFuture<Void> postConnect();

    /**
     * 处理连接失败
     */
    protected void handleConnectFailure(Throwable throwable) {
        log.error("连接失败: 玩家={}, 目标服务器={}, 错误={}",
                player.getName(), targetServer.getName(), throwable.getMessage());

        if (reason == ConnectReason.INITIAL_CONNECT) {
            player.kick("无法连接到服务器: " + throwable.getMessage());
        } else {
            player.sendMessage("切换服务器失败: " + throwable.getMessage());
        }
    }

    /**
     * 更新玩家连接状态
     */
    protected void updatePlayerConnection() {
        // 从旧服务器移除玩家
        if (player.getConnectedServer() != null) {
            player.getConnectedServer().getConnectedPlayers().remove(player);
        }

        // 添加到新服务器
        player.setConnectedServer(targetServer);
        targetServer.getConnectedPlayers().add(player);
    }

    public ProxiedServer getTargetServer() {
        return targetServer;
    }

    public ConnectReason getReason() {
        return reason;
    }

    /**
     * 连接原因枚举
     */
    public enum ConnectReason {
        INITIAL_CONNECT,    // 首次连接
        SERVER_SWITCH,      // 服务器切换
        RECONNECT          // 重连
    }
}