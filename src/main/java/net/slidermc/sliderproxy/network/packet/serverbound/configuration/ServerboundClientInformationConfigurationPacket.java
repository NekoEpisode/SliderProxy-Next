package net.slidermc.sliderproxy.network.packet.serverbound.configuration;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlayerSettingsChangedEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.player.data.ClientInformation;
import net.slidermc.sliderproxy.api.utils.UnsignedByte;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerboundClientInformationConfigurationPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundClientInformationConfigurationPacket.class);
    private String locale;
    private byte viewDistance;
    private ClientInformation.ChatMode chatMode;
    private boolean chatColors;
    private UnsignedByte displayedSkinParts;
    private ClientInformation.MainHandType mainHandType;
    private boolean enableTextFiltering;
    private boolean allowServerListings;
    private ClientInformation.ParticleStatus particleStatus;

    public ServerboundClientInformationConfigurationPacket() {}

    public ServerboundClientInformationConfigurationPacket(String locale, byte viewDistance, ClientInformation.ChatMode chatMode,
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

    public ServerboundClientInformationConfigurationPacket(ClientInformation clientInformation) {
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
        log.debug("已更新玩家 {} 的客户端信息(configuration): {}", player.getGameProfile().name(), clientInformation);
        
        // 触发设置改变事件
        PlayerSettingsChangedEvent settingsEvent = new PlayerSettingsChangedEvent(player, clientInformation);
        EventRegistry.callEvent(settingsEvent);
        
        return HandleResult.FORWARD;
    }

    public byte getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(byte viewDistance) {
        this.viewDistance = viewDistance;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public ClientInformation.ChatMode getChatMode() {
        return chatMode;
    }

    public void setChatMode(ClientInformation.ChatMode chatMode) {
        this.chatMode = chatMode;
    }

    public boolean isChatColors() {
        return chatColors;
    }

    public void setChatColors(boolean chatColors) {
        this.chatColors = chatColors;
    }

    public UnsignedByte getDisplayedSkinParts() {
        return displayedSkinParts;
    }

    public void setDisplayedSkinParts(UnsignedByte displayedSkinParts) {
        this.displayedSkinParts = displayedSkinParts;
    }

    public ClientInformation.MainHandType getMainHandType() {
        return mainHandType;
    }

    public void setMainHandType(ClientInformation.MainHandType mainHandType) {
        this.mainHandType = mainHandType;
    }

    public boolean isEnableTextFiltering() {
        return enableTextFiltering;
    }

    public void setEnableTextFiltering(boolean enableTextFiltering) {
        this.enableTextFiltering = enableTextFiltering;
    }

    public boolean isAllowServerListings() {
        return allowServerListings;
    }

    public void setAllowServerListings(boolean allowServerListings) {
        this.allowServerListings = allowServerListings;
    }

    public ClientInformation.ParticleStatus getParticleStatus() {
        return particleStatus;
    }

    public void setParticleStatus(ClientInformation.ParticleStatus particleStatus) {
        this.particleStatus = particleStatus;
    }
}