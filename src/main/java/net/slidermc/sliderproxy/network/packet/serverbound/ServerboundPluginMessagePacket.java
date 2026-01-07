package net.slidermc.sliderproxy.network.packet.serverbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.key.Key;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.ReceivePluginMessageEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ServerboundPluginMessagePacket implements IMinecraftPacket {
    private Key identifier;
    private byte[] data;

    @Override
    public void read(ByteBuf byteBuf) {
        this.identifier = Key.key(MinecraftProtocolHelper.readString(byteBuf));
        int remainingBytes = byteBuf.readableBytes();
        if (remainingBytes > 32767) {
            throw new RuntimeException("Plugin message data too large: " + remainingBytes + " bytes");
        }
        this.data = new byte[remainingBytes];
        byteBuf.readBytes(this.data);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, identifier.namespace() + ":" + identifier.value());
        byteBuf.writeBytes(data);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ReceivePluginMessageEvent event = new ReceivePluginMessageEvent(identifier, data, ReceivePluginMessageEvent.From.UPSTREAM);
        EventRegistry.callEvent(event);
        if (!event.isCancelled()) {
            this.data = event.getData();
            this.identifier = event.getIdentifier();
            ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
            if (player != null) {
                MinecraftNettyClient client = player.getDownstreamClient();
                if (client != null) {
                    client.getChannel().writeAndFlush(this);
                }
            }
        }
        return HandleResult.UNFORWARD;
    }
}
