package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.player.data.ClientInformation;
import net.slidermc.sliderproxy.api.utils.UnsignedByte;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerboundClientInformationPlayPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundClientInformationPlayPacket.class);
    private String locale;
    private byte viewDistance;
    private ClientInformation.ChatMode chatMode;
    private boolean chatColors;
    private UnsignedByte displayedSkinParts;
    private ClientInformation.MainHandType mainHandType;
    private boolean enableTextFiltering;
    private boolean allowServerListings;
    private ClientInformation.ParticleStatus particleStatus;

    public ServerboundClientInformationPlayPacket() {}

    public ServerboundClientInformationPlayPacket(String locale, byte viewDistance, ClientInformation.ChatMode chatMode,
                                                  boolean chatColors, UnsignedByte displayedSkinParts, ClientInformation.MainHandType mainHandType,
                                                  boolean enableTextFiltering, boolean allowServerListings, ClientInformation.ParticleStatus particleStatus) {
        this.locale = locale;
        this.viewDistance = viewDistance;
        this.chatMode = chatMode;
        this.chatColors = chatColors;
        this.displayedSkinParts = displayedSkinParts;
        this.mainHandType = mainHandType;
        this.enableTextFiltering = enableTextFiltering;
        this.allowServerListings = allowServerListings;
        this.particleStatus = particleStatus;
    }

    public ServerboundClientInformationPlayPacket(ClientInformation clientInformation) {
        this.locale = clientInformation.getLocale();
        this.viewDistance = clientInformation.getViewDistance();
        this.chatMode = clientInformation.getChatMode();
        this.chatColors = clientInformation.isChatColors();
        this.displayedSkinParts = clientInformation.getDisplayedSkinParts();
        this.mainHandType = clientInformation.getMainHandType();
        this.enableTextFiltering = clientInformation.isEnableTextFiltering();
        this.allowServerListings = clientInformation.isAllowServerListings();
        this.particleStatus = clientInformation.getParticleStatus();
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.locale = MinecraftProtocolHelper.readString(byteBuf);
        this.viewDistance = byteBuf.readByte();
        this.chatMode = MinecraftProtocolHelper.readEnum(byteBuf, ClientInformation.ChatMode::fromId);
        this.chatColors = MinecraftProtocolHelper.readBoolean(byteBuf);
        this.displayedSkinParts = new UnsignedByte(byteBuf.readUnsignedByte());
        this.mainHandType = MinecraftProtocolHelper.readEnum(byteBuf, ClientInformation.MainHandType::fromId);
        this.enableTextFiltering = MinecraftProtocolHelper.readBoolean(byteBuf);
        this.allowServerListings = MinecraftProtocolHelper.readBoolean(byteBuf);
        this.particleStatus = MinecraftProtocolHelper.readEnum(byteBuf, ClientInformation.ParticleStatus::fromId);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, locale);
        byteBuf.writeByte(viewDistance);
        MinecraftProtocolHelper.writeEnum(byteBuf, chatMode);
        MinecraftProtocolHelper.writeBoolean(byteBuf, chatColors);
        byteBuf.writeByte(displayedSkinParts.value());
        MinecraftProtocolHelper.writeEnum(byteBuf, mainHandType);
        MinecraftProtocolHelper.writeBoolean(byteBuf, enableTextFiltering);
        MinecraftProtocolHelper.writeBoolean(byteBuf, allowServerListings);
        MinecraftProtocolHelper.writeEnum(byteBuf, particleStatus);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player == null) return HandleResult.FORWARD;
        ClientInformation clientInformation = player.getClientInformation();
        clientInformation.setLocale(locale);
        clientInformation.setViewDistance(viewDistance);
        clientInformation.setChatMode(chatMode);
        clientInformation.setChatColors(chatColors);
        clientInformation.setDisplayedSkinParts(displayedSkinParts);
        clientInformation.setMainHandType(mainHandType);
        clientInformation.setEnableTextFiltering(enableTextFiltering);
        clientInformation.setAllowServerListings(allowServerListings);
        clientInformation.setParticleStatus(particleStatus);
        clientInformation.setUpdated(true);
        log.debug("已更新玩家 {} 的客户端信息(play): {}", player.getGameProfile().name(), clientInformation);
        return HandleResult.FORWARD;
    }
}