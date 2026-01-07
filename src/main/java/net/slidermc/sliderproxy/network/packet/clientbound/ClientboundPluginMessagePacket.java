package net.slidermc.sliderproxy.network.packet.clientbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.key.Key;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.ReceivePluginMessageEvent;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ClientboundPluginMessagePacket implements IMinecraftPacket {
    private Key identifier;
    private byte[] data;

    public ClientboundPluginMessagePacket() {}

    public ClientboundPluginMessagePacket(Key identifier, byte[] data) {
        this.identifier = identifier;
        this.data = data;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.identifier = Key.key(MinecraftProtocolHelper.readString(byteBuf));

        int remainingBytes = byteBuf.readableBytes();
        if (remainingBytes > 1048576) {
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
        ReceivePluginMessageEvent event = new ReceivePluginMessageEvent(identifier, data, ReceivePluginMessageEvent.From.DOWNSTREAM, ctx.channel());
        EventRegistry.callEvent(event);
        if (!event.isCancelled()) {
            this.data = event.getData();
            this.identifier = event.getIdentifier();
            PlayerConnection playerConnection = PlayerConnection.fromChannel(ctx.channel());
            if (playerConnection != null) {
                playerConnection.getUpstreamChannel().writeAndFlush(this);
            }
        }
        return HandleResult.UNFORWARD;
    }
}
