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
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
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

        } catch (Exception e) {
            log.error(TranslateManager.translate("sliderproxy.network.connection.exception", username), e);
            ctx.channel().close();
        }

        return HandleResult.UNFORWARD;
    }
}