package net.slidermc.sliderproxy.network.packet.clientbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlayerKickEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.network.AdventureNBTHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ClientboundDisconnectPlayPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundDisconnectPlayPacket.class);
    private Component reasonComponent;

    public ClientboundDisconnectPlayPacket() {}

    public ClientboundDisconnectPlayPacket(Component reason) {
        this.reasonComponent = reason;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.reasonComponent = AdventureNBTHelper.readComponent(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        AdventureNBTHelper.writeComponent(byteBuf, reasonComponent);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByDownstreamChannel(ctx.channel());
        if (player == null) return HandleResult.UNFORWARD;
        
        // 触发踢出事件
        PlayerKickEvent kickEvent = new PlayerKickEvent(player, player.getConnectedServer(), reasonComponent);
        EventRegistry.callEvent(kickEvent);
        
        // 如果事件被取消，不踢出玩家
        if (kickEvent.isCancelled()) {
            log.debug("玩家 {} 的踢出被取消", player.getName());
            return HandleResult.UNFORWARD;
        }
        
        // 如果设置了重定向服务器，尝试转移玩家
        if (kickEvent.getRedirectServer() != null) {
            ProxiedServer redirectServer = kickEvent.getRedirectServer();
            log.info("玩家 {} 被踢出，重定向到服务器: {}", player.getName(), redirectServer.getName());
            
            player.connectTo(redirectServer).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    // 重定向失败，踢出玩家
                    log.error("重定向玩家 {} 到 {} 失败", player.getName(), redirectServer.getName(), throwable);
                    player.kick(kickEvent.getReason());
                } else {
                    // 重定向成功，发送消息
                    String locale = player.getClientInformation().getLocale();
                    if (locale == null || !TranslateManager.isLanguageRegistered(locale)) locale = "en_us";
                    
                    Component message = Component.text(
                        Objects.requireNonNull(
                            TranslateManager.translateWithLang(
                                locale,
                                "sliderproxy.network.connection.kick.redirected",
                                player.getConnectedServer().getName(),
                                redirectServer.getName()
                            )
                        )
                    ).color(NamedTextColor.YELLOW);
                    
                    player.sendMessage(message);
                }
            });
            
            return HandleResult.UNFORWARD;
        }
        
        // 没有重定向，正常踢出
        String locale = player.getClientInformation().getLocale();
        if (locale == null || !TranslateManager.isLanguageRegistered(locale)) locale = "en_us";
        Component reason = (
                Component.text(
                        Objects.requireNonNull(
                                TranslateManager.translateWithLang(
                                        locale,
                                        "sliderproxy.network.connection.kick.downstream.reason"
                                )
                        )
                ).append((kickEvent.getReason() == null ? Component.text("Unknown") : kickEvent.getReason())) // key + reason，比如"下游服务器断开了连接: xxx"
        ).color(NamedTextColor.RED);
        player.kick(reason);
        return HandleResult.UNFORWARD;
    }

    public Component getReasonComponent() {
        return reasonComponent;
    }

    public void setReasonComponent(Component reasonComponent) {
        this.reasonComponent = reasonComponent;
    }
}
