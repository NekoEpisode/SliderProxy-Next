package net.slidermc.sliderproxy.network.packet.clientbound.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;

import java.util.Objects;

public class ClientboundDisconnectLoginPacket implements IMinecraftPacket {
    private static final GsonComponentSerializer gsonSerializer = GsonComponentSerializer.builder().build();

    private String reasonJsonComponent;

    public ClientboundDisconnectLoginPacket() {}

    public ClientboundDisconnectLoginPacket(Component reason) {
        this.reasonJsonComponent = gsonSerializer.serialize(reason);
    }

    public ClientboundDisconnectLoginPacket(String reasonJsonComponent) {
        this.reasonJsonComponent = reasonJsonComponent;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.reasonJsonComponent = MinecraftProtocolHelper.readString(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, reasonJsonComponent);
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
                ).append(gsonSerializer.deserialize(reasonJsonComponent)) // key + reason，比如"下游服务器断开了连接: xxx"
        ).color(NamedTextColor.RED);
        player.kick(reason);
        return HandleResult.UNFORWARD;
    }

    public String getReasonJsonComponent() {
        return reasonJsonComponent;
    }

    public void setReasonJsonComponent(String reasonJsonComponent) {
        this.reasonJsonComponent = reasonJsonComponent;
    }
}
