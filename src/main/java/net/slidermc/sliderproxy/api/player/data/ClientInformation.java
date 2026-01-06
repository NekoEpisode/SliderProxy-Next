package net.slidermc.sliderproxy.api.player.data;

import net.slidermc.sliderproxy.api.utils.UnsignedByte;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ClientInformation {
    private boolean isUpdated = false;
    private String locale = null;
    private byte viewDistance = -1;
    private ChatMode chatMode = ChatMode.UNKNOWN;
    private boolean chatColors = true;
    private UnsignedByte displayedSkinParts = null;
    private MainHandType mainHandType = MainHandType.UNKNOWN;
    private boolean enableTextFiltering = false;
    private boolean allowServerListings = true;
    private ParticleStatus particleStatus = ParticleStatus.UNKNOWN;

    public enum ChatMode implements MinecraftProtocolHelper.ProtocolEnum {
        ENABLED(0),
        COMMANDS_ONLY(1),
        HIDDEN(2),
        UNKNOWN(-1);

        private final int id;

        ChatMode(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        public static ChatMode fromId(int id) {
            for (ChatMode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            return UNKNOWN;
        }
    }

    public enum MainHandType implements MinecraftProtocolHelper.ProtocolEnum {
        LEFT(0),
        RIGHT(1),
        UNKNOWN(-1);

        private final int id;

        MainHandType(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        public static MainHandType fromId(int id) {
            for (MainHandType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    public enum ParticleStatus implements MinecraftProtocolHelper.ProtocolEnum {
        ALL(0),
        DECREASED(1),
        MINIMAL(2),
        UNKNOWN(-1);

        private final int id;

        ParticleStatus(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        public static ParticleStatus fromId(int id) {
            for (ParticleStatus status : values()) {
                if (status.id == id) {
                    return status;
                }
            }
            return UNKNOWN;
        }
    }

    public @Nullable String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public byte getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(byte viewDistance) {
        this.viewDistance = viewDistance;
    }

    public ChatMode getChatMode() {
        return chatMode;
    }

    public void setChatMode(ChatMode chatMode) {
        this.chatMode = chatMode;
    }

    public boolean isChatColors() {
        return chatColors;
    }

    public void setChatColors(boolean chatColors) {
        this.chatColors = chatColors;
    }

    public MainHandType getMainHandType() {
        return mainHandType;
    }

    public void setMainHandType(MainHandType mainHandType) {
        this.mainHandType = mainHandType;
    }

    public @Nullable UnsignedByte getDisplayedSkinParts() {
        return displayedSkinParts;
    }

    public void setDisplayedSkinParts(UnsignedByte displayedSkinParts) {
        this.displayedSkinParts = displayedSkinParts;
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

    public ParticleStatus getParticleStatus() {
        return particleStatus;
    }

    public void setParticleStatus(ParticleStatus particleStatus) {
        this.particleStatus = particleStatus;
    }

    public void setUpdated(boolean updated) {
        if (this.isUpdated) throw new IllegalStateException("Cannot set 'updated' state when 'updated' is already set to true");
        isUpdated = updated;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ClientInformation that = (ClientInformation) o;
        return isUpdated == that.isUpdated && viewDistance == that.viewDistance && chatColors == that.chatColors && enableTextFiltering == that.enableTextFiltering && allowServerListings == that.allowServerListings && Objects.equals(locale, that.locale) && chatMode == that.chatMode && Objects.equals(displayedSkinParts, that.displayedSkinParts) && mainHandType == that.mainHandType && particleStatus == that.particleStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isUpdated, locale, viewDistance, chatMode, chatColors, displayedSkinParts, mainHandType, enableTextFiltering, allowServerListings, particleStatus);
    }

    @Override
    public String toString() {
        return "ClientInformation{" +
                "isUpdated=" + isUpdated +
                ", locale='" + locale + '\'' +
                ", viewDistance=" + viewDistance +
                ", chatMode=" + chatMode +
                ", chatColors=" + chatColors +
                ", displayedSkinParts=" + displayedSkinParts +
                ", mainHandType=" + mainHandType +
                ", enableTextFiltering=" + enableTextFiltering +
                ", allowServerListings=" + allowServerListings +
                ", particleStatus=" + particleStatus +
                '}';
    }
}
