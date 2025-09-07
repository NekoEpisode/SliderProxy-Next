package net.slidermc.sliderproxy.network.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.netty.downstream.DownstreamChannelInitializer;
import net.slidermc.sliderproxy.network.packet.serverbound.handshake.ServerboundHandshakePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.login.ServerboundHelloPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class MinecraftNettyClient {
    private static final Logger log = LoggerFactory.getLogger(MinecraftNettyClient.class);
    private final InetSocketAddress address;
    private Channel channel;
    private EventLoopGroup group;
    private boolean connected = false;
    private final ProxiedPlayer bindPlayer;

    // 用于等待状态变化的 CompletableFuture
    private CompletableFuture<Void> loginFuture;

    public MinecraftNettyClient(InetSocketAddress address, ProxiedPlayer player) {
        this.address = address;
        this.bindPlayer = player;
    }

    public CompletableFuture<Void> connectAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                connect();
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 连接到指定地址服务器并自动设置玩家的PlayerConnection.downstreamChannel
     */
    private void connect() throws InterruptedException {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new DownstreamChannelInitializer(bindPlayer));

        ChannelFuture future = bootstrap.connect(address).sync();
        this.channel = future.channel();
        bindPlayer.getPlayerConnection().setDownstreamChannel(channel);

        // 设置初始协议状态为 HANDSHAKE
        bindPlayer.getPlayerConnection().setDownstreamInboundProtocolState(ProtocolState.HANDSHAKE);
        bindPlayer.getPlayerConnection().setDownstreamOutboundProtocolState(ProtocolState.HANDSHAKE);

        connected = true;
    }

    public CompletableFuture<Void> loginAsync() {
        if (!connected) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client not connected"));
        }

        // 确保在 Netty 的 EventLoop 线程中执行
        if (channel.eventLoop().inEventLoop()) {
            return doLogin();
        } else {
            CompletableFuture<Void> future = new CompletableFuture<>();
            channel.eventLoop().execute(() -> {
                try {
                    doLogin().thenAccept(v -> future.complete(null))
                            .exceptionally(e -> {
                                future.completeExceptionally(e);
                                return null;
                            });
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        }
    }

    private CompletableFuture<Void> doLogin() {
        if (bindPlayer.getPlayerConnection().getDownstreamInboundProtocolState() != ProtocolState.HANDSHAKE) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client not in HANDSHAKE state"));
        }

        loginFuture = new CompletableFuture<>();

        try {
            // 先发送握手包（在 HANDSHAKE 状态下）
            ServerboundHandshakePacket handshakePacket = new ServerboundHandshakePacket(772, address.getHostString(), (short) address.getPort(), 2);
            channel.writeAndFlush(handshakePacket).addListener(future -> {
                if (future.isSuccess()) {
                    // 握手包发送成功后，切换到 LOGIN 状态
                    bindPlayer.getPlayerConnection().setDownstreamInboundProtocolState(ProtocolState.LOGIN);
                    bindPlayer.getPlayerConnection().setDownstreamOutboundProtocolState(ProtocolState.LOGIN);

                    // 然后发送登录包
                    ServerboundHelloPacket helloPacket = new ServerboundHelloPacket(bindPlayer.getGameProfile().name(), bindPlayer.getGameProfile().uuid());
                    channel.writeAndFlush(helloPacket).addListener(loginFuture -> {
                        if (!loginFuture.isSuccess()) {
                            MinecraftNettyClient.this.failLogin(loginFuture.cause());
                        }
                    });
                } else {
                    MinecraftNettyClient.this.failLogin(future.cause());
                }
            });

            return loginFuture;
        } catch (Exception e) {
            log.error("Login process failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public void disconnect() {
        if (channel != null) {
            bindPlayer.getPlayerConnection().setDownstreamChannel(null);
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        connected = false;
    }

    // 当状态变为 CONFIGURATION 时调用此方法完成登录
    public void completeLogin() {
        if (loginFuture != null && !loginFuture.isDone()) {
            loginFuture.complete(null);
        }
    }

    // 当登录失败时调用此方法
    public void failLogin(Throwable cause) {
        if (loginFuture != null && !loginFuture.isDone()) {
            loginFuture.completeExceptionally(cause);
        }
    }

    // Getters and setters
    public Channel getChannel() { return channel; }
    public InetSocketAddress getAddress() { return address; }
}