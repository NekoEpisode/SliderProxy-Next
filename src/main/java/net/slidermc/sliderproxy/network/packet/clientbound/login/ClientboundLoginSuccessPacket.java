package net.slidermc.sliderproxy.network.packet.clientbound.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.login.ServerboundLoginAcknowledgePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientboundLoginSuccessPacket implements IMinecraftPacket {

    private static final Logger log = LoggerFactory.getLogger(ClientboundLoginSuccessPacket.class);
    private UUID uuid;
    private String username;
    private List<Property> properties;

    public ClientboundLoginSuccessPacket() {}

    public ClientboundLoginSuccessPacket(UUID uuid, String username, List<Property> properties) {
        this.uuid = uuid;
        this.username = username;
        this.properties = properties;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.uuid = MinecraftProtocolHelper.readUUID(byteBuf);
        this.username = MinecraftProtocolHelper.readString(byteBuf);
        int propertyCount = MinecraftProtocolHelper.readVarInt(byteBuf);
        this.properties = new ArrayList<>();
        for (int i = 0; i < propertyCount; i++) {
            String name = MinecraftProtocolHelper.readString(byteBuf);
            String value = MinecraftProtocolHelper.readString(byteBuf);
            boolean hasSignature = byteBuf.readBoolean();
            String signature = hasSignature ? MinecraftProtocolHelper.readString(byteBuf) : null;
            properties.add(new Property(name, value, signature));
        }
    }

    @Override
    public void write(ByteBuf buf) {
        MinecraftProtocolHelper.writeUUID(buf, uuid);
        MinecraftProtocolHelper.writeString(buf, username);

        MinecraftProtocolHelper.writeVarInt(buf, properties.size());
        for (Property prop : properties) {
            MinecraftProtocolHelper.writeString(buf, prop.name());
            MinecraftProtocolHelper.writeString(buf, prop.value());
            if (prop.signature() != null) {
                buf.writeBoolean(true);
                MinecraftProtocolHelper.writeString(buf, prop.signature());
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByDownstreamChannel(ctx.channel());
        if (player != null) {
            PlayerConnection playerConnection = player.getPlayerConnection();
            if (playerConnection != null && playerConnection.getDownstreamChannel() != null) {
                playerConnection.getDownstreamChannel().writeAndFlush(new ServerboundLoginAcknowledgePacket());
                playerConnection.setDownstreamInboundProtocolState(ProtocolState.CONFIGURATION);
                playerConnection.setDownstreamOutboundProtocolState(ProtocolState.CONFIGURATION);
                player.getDownstreamClient().completeLogin();
            }
        }
        return HandleResult.UNFORWARD;
    }

    // 内部类表示属性
    public record Property(String name, String value, String signature) {}
}