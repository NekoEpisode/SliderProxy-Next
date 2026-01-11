package net.slidermc.sliderproxy.network.packet.serverbound.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.slidermc.sliderproxy.RunningData;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlayerLoginEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.player.data.GameProfile;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.api.server.ServerManager;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.encryption.EncryptionUtil;
import net.slidermc.sliderproxy.network.encryption.LoginState;
import net.slidermc.sliderproxy.network.encryption.ServerEncryptionManager;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundEncryptionRequestPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
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
        try {
            PlayerConnection connection = PlayerConnection.fromChannel(ctx.channel());
            if (connection == null) {
                log.error(TranslateManager.translate("sliderproxy.network.connection.error.get", username));
                ctx.channel().close();
                return HandleResult.UNFORWARD;
            }

            // 检查是否启用 online-mode
            boolean onlineMode = RunningData.configuration.getBoolean("proxy.online-mode", true);
            
            if (onlineMode) {
                // Online mode: 发送加密请求
                return handleOnlineMode(ctx, connection);
            } else {
                // Offline mode: 直接完成登录
                return handleOfflineMode(ctx, connection);
            }

        } catch (Exception e) {
            log.error(TranslateManager.translate("sliderproxy.network.connection.exception", username), e);
            ctx.channel().close();
        }

        return HandleResult.UNFORWARD;
    }

    /**
     * 处理 Online Mode 登录
     */
    private HandleResult handleOnlineMode(ChannelHandlerContext ctx, PlayerConnection connection) {
        // 生成验证令牌
        byte[] verifyToken = EncryptionUtil.generateVerifyToken();
        
        // 创建登录状态并存储到 Channel
        LoginState loginState = new LoginState(username, uuid, verifyToken);
        ctx.channel().attr(LoginState.KEY).set(loginState);
        
        // 获取服务器公钥
        ServerEncryptionManager encryptionManager = ServerEncryptionManager.getInstance();
        byte[] publicKey = encryptionManager.getPublicKeyEncoded();
        
        // 发送加密请求
        // Server ID 总是空字符串，shouldAuthenticate 为 true
        ClientboundEncryptionRequestPacket encryptionRequest = new ClientboundEncryptionRequestPacket(
                "", // serverId
                publicKey,
                verifyToken,
                true // shouldAuthenticate
        );
        
        ctx.channel().writeAndFlush(encryptionRequest);
        log.debug("Sent encryption request to {}", username);
        
        return HandleResult.UNFORWARD;
    }

    /**
     * 处理 Offline Mode 登录
     */
    private HandleResult handleOfflineMode(ChannelHandlerContext ctx, PlayerConnection connection) {
        // 创建玩家对象
        ProxiedPlayer player = new ProxiedPlayer(new GameProfile(username, uuid), connection);
        PlayerManager.getInstance().registerPlayer(player);
        log.info(TranslateManager.translate("sliderproxy.network.connection.connected", username, connection.getUpstreamChannel().remoteAddress()));
        EventRegistry.callEvent(new PlayerLoginEvent(player));

        // 获取默认服务器
        String defaultServerName = RunningData.configuration.getString("proxy.default-server", "lobby");
        ProxiedServer defaultServer = ServerManager.getInstance().getServer(defaultServerName);

        if (defaultServer == null) {
            log.error(TranslateManager.translate("sliderproxy.network.connection.defaultserver.notfound", defaultServerName));
            player.kick(Component.text(Objects.requireNonNull(TranslateManager.translate("sliderproxy.network.connection.defaultserver.unavailable"))).color(NamedTextColor.RED));
            return HandleResult.UNFORWARD;
        }

        // 异步连接到默认服务器
        player.connectTo(defaultServer).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(TranslateManager.translate("sliderproxy.network.connection.defaultserver.failed", username), throwable);
            }
        });

        return HandleResult.UNFORWARD;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}