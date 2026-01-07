package net.slidermc.sliderproxy.network.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import net.slidermc.sliderproxy.RunningData;
import net.slidermc.sliderproxy.api.config.YamlConfiguration;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.player.data.ClientInformation;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.netty.downstream.DownstreamChannelInitializer;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundLoginSuccessPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.configuration.ServerboundClientInformationConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.handshake.ServerboundHandshakePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.login.ServerboundHelloPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * 下游 Minecraft 服务器连接客户端
 * 每个实例独立管理自己的协议状态，与 PlayerConnection 解耦
 */
public class MinecraftNettyClient {
    private static final Logger log = LoggerFactory.getLogger(MinecraftNettyClient.class);

    public static final AttributeKey<MinecraftNettyClient> KEY = AttributeKey.valueOf("downstream_client");

    private final InetSocketAddress address;
    private Channel channel;
    private EventLoopGroup group;
    private boolean connected = false;
    private final ProxiedPlayer bindPlayer;

    // 下游连接自主管理的协议状态
    private volatile ProtocolState inboundProtocolState = ProtocolState.HANDSHAKE;
    private volatile ProtocolState outboundProtocolState = ProtocolState.HANDSHAKE;

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
     * 连接到指定地址服务器
     * 注意：不再自动设置 PlayerConnection.downstreamChannel，由调用方控制
     */
    private void connect() throws InterruptedException {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new DownstreamChannelInitializer(bindPlayer, this));

        ChannelFuture future = bootstrap.connect(address).sync();
        this.channel = future.channel();

        // 协议状态由 MinecraftNettyClient 自主管理，初始化时已设为 HANDSHAKE
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
        if (inboundProtocolState != ProtocolState.HANDSHAKE) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client not in HANDSHAKE state"));
        }

        loginFuture = new CompletableFuture<>();

        try {
            String ipForwardType = "none";
            YamlConfiguration configuration = RunningData.configuration;
            if (configuration != null) {
                ipForwardType = configuration.getString("proxy.ip-forward-type", "none").toLowerCase();
            }

            log.debug("IP转发类型: {}", ipForwardType);
            switch (ipForwardType) {
                case "legacy" -> {
                    String ip = "0.0.0.0";
                    if (channel != null) {
                        SocketAddress remoteAddress = bindPlayer.getPlayerConnection().getUpstreamChannel().remoteAddress();
                        if (remoteAddress instanceof InetSocketAddress) {
                            InetAddress inetAddress = ((InetSocketAddress) remoteAddress).getAddress();
                            if (inetAddress != null) {
                                ip = inetAddress.getHostAddress();
                            }
                        }
                    }
                    
                    // 构建 BungeeCord legacy forwarding 格式
                    // 格式: hostname\0ip\0uuid\0properties_json
                    StringBuilder bungeeCordHost = new StringBuilder();
                    bungeeCordHost.append(address.getHostString());
                    bungeeCordHost.append("\00").append(ip);
                    bungeeCordHost.append("\00").append(bindPlayer.getGameProfile().uuid().toString());
                    
                    // 添加皮肤属性 (如果有)
                    if (bindPlayer.getProperties() != null && !bindPlayer.getProperties().isEmpty()) {
                        JsonArray propsArray = new JsonArray();
                        for (ClientboundLoginSuccessPacket.Property prop : bindPlayer.getProperties()) {
                            JsonObject propObj = new JsonObject();
                            propObj.addProperty("name", prop.name());
                            propObj.addProperty("value", prop.value());
                            if (prop.signature() != null) {
                                propObj.addProperty("signature", prop.signature());
                            }
                            propsArray.add(propObj);
                        }
                        bungeeCordHost.append("\00").append(new Gson().toJson(propsArray));
                    }
                    
                    ServerboundHandshakePacket handshakePacket = new ServerboundHandshakePacket(
                            772, bungeeCordHost.toString(), (short) address.getPort(), 2);
                    channel.writeAndFlush(handshakePacket).addListener(future -> {
                        if (future.isSuccess()) {
                            // 握手包发送成功后，切换到 LOGIN 状态
                            setInboundProtocolState(ProtocolState.LOGIN);
                            setOutboundProtocolState(ProtocolState.LOGIN);

                            ServerboundHelloPacket helloPacket = new ServerboundHelloPacket(bindPlayer.getName(), bindPlayer.getGameProfile().uuid());
                            channel.writeAndFlush(helloPacket).addListener(loginFuture -> {
                                if (!loginFuture.isSuccess()) {
                                    MinecraftNettyClient.this.failLogin(loginFuture.cause());
                                }
                            });
                        } else {
                            MinecraftNettyClient.this.failLogin(future.cause());
                        }
                    });
                }
                default -> {
                    ServerboundHandshakePacket handshakePacket = new ServerboundHandshakePacket(772, address.getHostString(), (short) address.getPort(), 2);
                    channel.writeAndFlush(handshakePacket).addListener(future -> {
                        if (future.isSuccess()) {
                            // 握手包发送成功后，切换到 LOGIN 状态
                            setInboundProtocolState(ProtocolState.LOGIN);
                            setOutboundProtocolState(ProtocolState.LOGIN);

                            ServerboundHelloPacket helloPacket = new ServerboundHelloPacket(bindPlayer.getName(), bindPlayer.getGameProfile().uuid());
                            channel.writeAndFlush(helloPacket).addListener(loginFuture -> {
                                if (!loginFuture.isSuccess()) {
                                    MinecraftNettyClient.this.failLogin(loginFuture.cause());
                                }
                            });
                        } else {
                            MinecraftNettyClient.this.failLogin(future.cause());
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Login process failed", e);
            return CompletableFuture.failedFuture(e);
        }

        return loginFuture;
    }

    public void disconnect() {
        if (channel != null) {
            // 只有当前 channel 是 PlayerConnection 的下游 channel 时才清除引用
            if (bindPlayer.getPlayerConnection().getDownstreamChannel() == channel) {
                bindPlayer.getPlayerConnection().setDownstreamChannel(null);
            }
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

    // ========== 协议状态管理 ==========

    /**
     * 获取下游入站协议状态（代理接收服务器数据时使用的状态）
     */
    public ProtocolState getInboundProtocolState() {
        return inboundProtocolState;
    }

    /**
     * 获取下游出站协议状态（代理发送数据给服务器时使用的状态）
     */
    public ProtocolState getOutboundProtocolState() {
        return outboundProtocolState;
    }

    /**
     * 设置下游入站协议状态
     */
    public void setInboundProtocolState(@NotNull ProtocolState state) {
        this.inboundProtocolState = state;
    }

    /**
     * 设置下游出站协议状态
     */
    public void setOutboundProtocolState(@NotNull ProtocolState state) {
        this.outboundProtocolState = state;
        if (this.outboundProtocolState == ProtocolState.CONFIGURATION) {
            // 向下游服务器发送ClientInformation包，防止下游服务器不知道客户端设置
            ClientInformation clientInformation = bindPlayer.getClientInformation();
            if (clientInformation.isUpdated()) {
                channel.writeAndFlush(new ServerboundClientInformationConfigurationPacket(clientInformation));
                log.debug("已为玩家 {} 的下游服务器发送ClientInformation包", bindPlayer.getGameProfile().name());
            } else {
                log.debug("发现玩家 {} 的ClientInformation不为updated，终止发送下游服务器的ClientInformation包", bindPlayer.getGameProfile().name());
            }
        }
    }

    /**
     * 从 Channel 获取绑定的 MinecraftNettyClient
     */
    @Nullable
    public static MinecraftNettyClient fromChannel(Channel channel) {
        return channel.attr(KEY).get();
    }

    // ========== Getters ==========

    public Channel getChannel() { return channel; }
    public InetSocketAddress getAddress() { return address; }
    public ProxiedPlayer getBindPlayer() { return bindPlayer; }
    public boolean isConnected() { return connected; }
}