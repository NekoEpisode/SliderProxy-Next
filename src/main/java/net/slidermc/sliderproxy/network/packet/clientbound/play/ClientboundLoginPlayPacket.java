package net.slidermc.sliderproxy.network.packet.clientbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ClientboundLoginPlayPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundLoginPlayPacket.class);
    public int entityId;
    public boolean isHardcore;
    public List<String> dimensionNames = new ArrayList<>();
    public int maxPlayers;
    public int viewDistance;
    public int simulationDistance;
    public boolean reducedDebugInfo;
    public boolean enableRespawnScreen;
    public boolean doLimitedCrafting;
    public int dimensionType; // VarInt
    public String dimensionName;
    public long hashedSeed;
    public int gameMode; // Unsigned Byte
    public byte previousGameMode; // -1 undefined
    public boolean isDebug;
    public boolean isFlat;
    public boolean hasDeathLocation;
    public String deathDimensionName; // optional
    public int[] deathLocation;       // optional, [x, y, z]
    public int portalCooldown; // VarInt
    public int seaLevel;       // VarInt
    public boolean enforcesSecureChat;

    @Override
    public void read(ByteBuf buf) {
        entityId = buf.readInt();
        isHardcore = MinecraftProtocolHelper.readBoolean(buf);

        int dimensionCount = MinecraftProtocolHelper.readVarInt(buf);
        dimensionNames.clear();
        for (int i = 0; i < dimensionCount; i++) {
            dimensionNames.add(MinecraftProtocolHelper.readString(buf));
        }

        maxPlayers = MinecraftProtocolHelper.readVarInt(buf);
        viewDistance = MinecraftProtocolHelper.readVarInt(buf);
        simulationDistance = MinecraftProtocolHelper.readVarInt(buf);
        reducedDebugInfo = MinecraftProtocolHelper.readBoolean(buf);
        enableRespawnScreen = MinecraftProtocolHelper.readBoolean(buf);
        doLimitedCrafting = MinecraftProtocolHelper.readBoolean(buf);

        dimensionType = MinecraftProtocolHelper.readVarInt(buf);
        dimensionName = MinecraftProtocolHelper.readString(buf);
        hashedSeed = buf.readLong();

        gameMode = buf.readUnsignedByte();
        previousGameMode = buf.readByte();

        isDebug = MinecraftProtocolHelper.readBoolean(buf);
        isFlat = MinecraftProtocolHelper.readBoolean(buf);

        hasDeathLocation = MinecraftProtocolHelper.readBoolean(buf);
        if (hasDeathLocation) {
            deathDimensionName = MinecraftProtocolHelper.readString(buf);
            deathLocation = MinecraftProtocolHelper.readPosition(buf);
        }

        portalCooldown = MinecraftProtocolHelper.readVarInt(buf);
        seaLevel = MinecraftProtocolHelper.readVarInt(buf);
        enforcesSecureChat = MinecraftProtocolHelper.readBoolean(buf);
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(entityId);
        MinecraftProtocolHelper.writeBoolean(buf, isHardcore);

        MinecraftProtocolHelper.writeVarInt(buf, dimensionNames.size());
        for (String dim : dimensionNames) {
            MinecraftProtocolHelper.writeString(buf, dim);
        }

        MinecraftProtocolHelper.writeVarInt(buf, maxPlayers);
        MinecraftProtocolHelper.writeVarInt(buf, viewDistance);
        MinecraftProtocolHelper.writeVarInt(buf, simulationDistance);
        MinecraftProtocolHelper.writeBoolean(buf, reducedDebugInfo);
        MinecraftProtocolHelper.writeBoolean(buf, enableRespawnScreen);
        MinecraftProtocolHelper.writeBoolean(buf, doLimitedCrafting);

        MinecraftProtocolHelper.writeVarInt(buf, dimensionType);
        MinecraftProtocolHelper.writeString(buf, dimensionName);
        buf.writeLong(hashedSeed);

        buf.writeByte(gameMode);
        buf.writeByte(previousGameMode);

        MinecraftProtocolHelper.writeBoolean(buf, isDebug);
        MinecraftProtocolHelper.writeBoolean(buf, isFlat);

        MinecraftProtocolHelper.writeBoolean(buf, hasDeathLocation);
        if (hasDeathLocation) {
            MinecraftProtocolHelper.writeString(buf, deathDimensionName);
            MinecraftProtocolHelper.writePosition(buf, deathLocation[0], deathLocation[1], deathLocation[2]);
        }

        MinecraftProtocolHelper.writeVarInt(buf, portalCooldown);
        MinecraftProtocolHelper.writeVarInt(buf, seaLevel);
        MinecraftProtocolHelper.writeBoolean(buf, enforcesSecureChat);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByDownstreamChannel(ctx.channel());
        if (player != null) {
            String gradientMessage = "<white>Welcome to </white>" +
                    "<gradient:#0ebeff:#42fcff>SliderProxy</gradient>";

            Component message = MiniMessage.miniMessage().deserialize(gradientMessage);
            player.sendMessage(message);
        }
        return HandleResult.FORWARD;
    }
}
