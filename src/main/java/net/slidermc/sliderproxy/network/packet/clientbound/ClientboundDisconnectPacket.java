package net.slidermc.sliderproxy.network.packet.clientbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientboundDisconnectPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundDisconnectPacket.class);
    private String reasonComponent;

    public ClientboundDisconnectPacket() {}

    public ClientboundDisconnectPacket(String reasonComponent) {
        this.reasonComponent = reasonComponent;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.reasonComponent = MinecraftProtocolHelper.readString(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, reasonComponent);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ctx.channel().close();
        log.info("Disconnected: {}", reasonComponent);
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByDownstreamChannel(ctx.channel());
        if (player != null) {
            player.kick(reasonComponent);
        }
        return HandleResult.UNFORWARD;
    }

    public String getReasonComponent() {
        return reasonComponent;
    }

    public void setReasonComponent(String reasonComponent) {
        this.reasonComponent = reasonComponent;
    }
}
