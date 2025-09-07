package net.slidermc.sliderproxy.network.packet.clientbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientboundPluginMessagePacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundPluginMessagePacket.class);
    private String identifier;
    private byte[] data;

    public ClientboundPluginMessagePacket() {}

    public ClientboundPluginMessagePacket(String identifier, byte[] data) {
        this.identifier = identifier;
        this.data = data;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.identifier = MinecraftProtocolHelper.readString(byteBuf);

        int remainingBytes = byteBuf.readableBytes();
        if (remainingBytes > 1048576) {
            throw new RuntimeException("Plugin message data too large: " + remainingBytes + " bytes");
        }
        this.data = new byte[remainingBytes];
        byteBuf.readBytes(this.data);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, identifier);
        byteBuf.writeBytes(data);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        if (identifier.equals("minecraft:brand")) {
            try {
                PlayerConnection connection = ctx.channel().attr(PlayerConnection.KEY).get();

                ByteBuf buffer = ctx.alloc().buffer();
                try {
                    buffer.writeBytes(data);

                    String originalBrand = MinecraftProtocolHelper.readString(buffer);
                    String newBrand = "SliderProxy -> " + originalBrand;

                    ByteBuf newBuffer = ctx.alloc().buffer();
                    try {
                        MinecraftProtocolHelper.writeString(newBuffer, newBrand);
                        byte[] newData = new byte[newBuffer.readableBytes()];
                        newBuffer.readBytes(newData);

                        ClientboundPluginMessagePacket newPacket = new ClientboundPluginMessagePacket(
                                identifier,
                                newData
                        );

                        connection.getUpstreamChannel().writeAndFlush(newPacket);
                        return HandleResult.UNFORWARD;
                    } finally {
                        newBuffer.release();
                    }
                } finally {
                    buffer.release();
                }
            } catch (Exception e) {
                log.error("处理Brand PluginMessage包失败", e);
                return HandleResult.FORWARD;
            }
        }
        return HandleResult.FORWARD;
    }

    public byte[] getData() {
        return data;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
