package net.slidermc.sliderproxy.network.packet.clientbound.configuration;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.AdventureNBTHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;

import java.util.Objects;

public class ClientboundDisconnectConfigurationPacket implements IMinecraftPacket {
    private Component reasonComponent;

    public ClientboundDisconnectConfigurationPacket() {}

    public ClientboundDisconnectConfigurationPacket(Component reason) {
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
                ).append((reasonComponent == null ? Component.text("Unknown") : reasonComponent)) // key + reason，比如"下游服务器断开了连接: xxx"
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
