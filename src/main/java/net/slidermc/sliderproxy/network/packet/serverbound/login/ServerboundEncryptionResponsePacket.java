package net.slidermc.sliderproxy.network.packet.serverbound.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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
import net.slidermc.sliderproxy.network.encryption.MojangSessionService;
import net.slidermc.sliderproxy.network.encryption.ServerEncryptionManager;
import net.slidermc.sliderproxy.network.netty.CipherDecoder;
import net.slidermc.sliderproxy.network.netty.CipherEncoder;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundDisconnectLoginPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Encryption Response 数据包 (Serverbound, Login State, Packet ID: 0x01)
 * 
 * 协议格式:
 * - Shared Secret: Prefixed Array of Byte - 使用服务器公钥加密的共享密钥
 * - Verify Token: Prefixed Array of Byte - 使用服务器公钥加密的验证令牌
 */
public class ServerboundEncryptionResponsePacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundEncryptionResponsePacket.class);

    private byte[] encryptedSharedSecret;
    private byte[] encryptedVerifyToken;

    public ServerboundEncryptionResponsePacket() {}

    @Override
    public void read(ByteBuf byteBuf) {
        this.encryptedSharedSecret = MinecraftProtocolHelper.readPrefixedByteArray(byteBuf);
        this.encryptedVerifyToken = MinecraftProtocolHelper.readPrefixedByteArray(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writePrefixedByteArray(byteBuf, encryptedSharedSecret);
        MinecraftProtocolHelper.writePrefixedByteArray(byteBuf, encryptedVerifyToken);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        PlayerConnection connection = PlayerConnection.fromChannel(channel);
        
        if (connection == null) {
            log.error("Failed to get PlayerConnection for encryption response");
            channel.close();
            return HandleResult.UNFORWARD;
        }

        // 获取登录状态
        LoginState loginState = channel.attr(LoginState.KEY).get();
        if (loginState == null) {
            log.error("No login state found for encryption response");
            channel.close();
            return HandleResult.UNFORWARD;
        }

        try {
            ServerEncryptionManager encryptionManager = ServerEncryptionManager.getInstance();
            
            // 解密共享密钥和验证令牌
            byte[] sharedSecret = EncryptionUtil.decryptRsa(
                    encryptionManager.getPrivateKey(), 
                    encryptedSharedSecret
            );
            byte[] verifyToken = EncryptionUtil.decryptRsa(
                    encryptionManager.getPrivateKey(), 
                    encryptedVerifyToken
            );

            // 验证令牌是否匹配
            if (!Arrays.equals(verifyToken, loginState.getVerifyToken())) {
                log.error("Verify token mismatch for user {}", loginState.getUsername());
                channel.writeAndFlush(new ClientboundDisconnectLoginPacket(
                        Component.text("Verify token mismatch").color(NamedTextColor.RED)
                ));
                channel.close();
                return HandleResult.UNFORWARD;
            }

            // 计算服务器ID哈希
            String serverIdHash = EncryptionUtil.computeServerIdHash(
                    "", // 服务器ID总是空字符串
                    sharedSecret,
                    encryptionManager.getPublicKeyEncoded()
            );

            // 获取玩家IP
            String playerIp = null;
            if (channel.remoteAddress() instanceof InetSocketAddress inetAddr) {
                playerIp = inetAddr.getAddress().getHostAddress();
            }

            // 异步验证 Mojang 会话
            String finalPlayerIp = playerIp;
            SecretKey secretKey = EncryptionUtil.createSecretKey(sharedSecret);
            
            // 客户端在发送 Encryption Response 后就已经启用了加密
            // 所以我们必须先启用加密，无论验证是否成功
            try {
                enableEncryption(channel, secretKey);
            } catch (Exception e) {
                log.error("Failed to enable encryption for user {}", loginState.getUsername(), e);
                channel.close();
                return HandleResult.UNFORWARD;
            }
            
            MojangSessionService.hasJoined(loginState.getUsername(), serverIdHash, finalPlayerIp)
                    .thenAccept(result -> {
                        if (result == null) {
                            log.warn("Authentication failed for user {}", loginState.getUsername());
                            String authFailedMsg = TranslateManager.translate("sliderproxy.auth.failed");
                            if (authFailedMsg == null) {
                                authFailedMsg = "Authentication failed. Please make sure you are using a premium account.";
                            }
                            channel.writeAndFlush(new ClientboundDisconnectLoginPacket(
                                    Component.text(authFailedMsg).color(NamedTextColor.RED)
                            ));
                            channel.close();
                            return;
                        }

                        // 更新登录状态
                        loginState.setAuthenticatedProfile(result.gameProfile());
                        loginState.setProperties(result.properties());

                        // 继续登录流程
                        completeLogin(ctx, connection, loginState);
                    })
                    .exceptionally(e -> {
                        log.error("Authentication error for user {}", loginState.getUsername(), e);
                        channel.writeAndFlush(new ClientboundDisconnectLoginPacket(
                                Component.text("Authentication error").color(NamedTextColor.RED)
                        ));
                        channel.close();
                        return null;
                    });

        } catch (Exception e) {
            log.error("Failed to process encryption response", e);
            channel.close();
        }

        return HandleResult.UNFORWARD;
    }

    /**
     * 启用 AES 加密
     */
    private void enableEncryption(Channel channel, SecretKey secretKey) throws Exception {
        Cipher encryptCipher = EncryptionUtil.createEncryptCipher(secretKey);
        Cipher decryptCipher = EncryptionUtil.createDecryptCipher(secretKey);

        // 在 frame-decoder 之后添加解密器
        channel.pipeline().addBefore("frame-decoder", "cipher-decoder", new CipherDecoder(decryptCipher));
        // 在 packet-encoder 之前添加加密器
        channel.pipeline().addBefore("packet-encoder", "cipher-encoder", new CipherEncoder(encryptCipher));
        
        log.debug("Encryption enabled for channel {}", channel.remoteAddress());
    }

    /**
     * 完成登录流程
     */
    private void completeLogin(ChannelHandlerContext ctx, PlayerConnection connection, LoginState loginState) {
        GameProfile profile = loginState.getAuthenticatedProfile();
        
        // 创建玩家对象
        ProxiedPlayer player = new ProxiedPlayer(profile, connection);
        player.setProperties(loginState.getProperties());
        PlayerManager.getInstance().registerPlayer(player);
        
        log.info(TranslateManager.translate("sliderproxy.network.connection.connected", 
                profile.name(), connection.getUpstreamChannel().remoteAddress()));
        EventRegistry.callEvent(new PlayerLoginEvent(player));

        // 获取默认服务器
        String defaultServerName = RunningData.configuration.getString("proxy.default-server", "lobby");
        ProxiedServer defaultServer = ServerManager.getInstance().getServer(defaultServerName);

        if (defaultServer == null) {
            log.error(TranslateManager.translate("sliderproxy.network.connection.defaultserver.notfound", defaultServerName));
            String unavailableMsg = TranslateManager.translate("sliderproxy.network.connection.defaultserver.unavailable");
            if (unavailableMsg == null) {
                unavailableMsg = "Default server unavailable";
            }
            player.kick(Component.text(unavailableMsg).color(NamedTextColor.RED));
            return;
        }

        // 异步连接到默认服务器
        player.connectTo(defaultServer).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(TranslateManager.translate("sliderproxy.network.connection.defaultserver.failed", 
                        profile.name()), throwable);
            }
        });
    }

    public byte[] getEncryptedSharedSecret() {
        return encryptedSharedSecret;
    }

    public byte[] getEncryptedVerifyToken() {
        return encryptedVerifyToken;
    }

    public void setEncryptedSharedSecret(byte[] encryptedSharedSecret) {
        this.encryptedSharedSecret = encryptedSharedSecret;
    }

    public void setEncryptedVerifyToken(byte[] encryptedVerifyToken) {
        this.encryptedVerifyToken = encryptedVerifyToken;
    }
}
