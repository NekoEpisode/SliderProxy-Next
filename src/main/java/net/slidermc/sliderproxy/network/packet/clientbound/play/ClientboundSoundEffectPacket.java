package net.slidermc.sliderproxy.network.packet.clientbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.key.Key;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlaySoundEvent;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientboundSoundEffectPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundSoundEffectPacket.class);
    private SoundEvent soundEvent;
    private SoundCategory category;
    private int x, y, z;
    private float volume, pitch;
    private long seed;

    public ClientboundSoundEffectPacket() {}

    public ClientboundSoundEffectPacket(SoundEvent soundEvent, SoundCategory category, int x, int y, int z, float volume, float pitch, long seed) {
        this.soundEvent = soundEvent;
        this.category = category;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    /**
     * 使用注册表 ID 创建
     */
    public static ClientboundSoundEffectPacket fromRegistryId(int registryId, SoundCategory category, int x, int y, int z, float volume, float pitch, long seed) {
        return new ClientboundSoundEffectPacket(new SoundEvent(registryId, null, false, 0), category, x, y, z, volume, pitch, seed);
    }

    /**
     * 使用内联定义创建
     */
    public static ClientboundSoundEffectPacket fromIdentifier(Key identifier, boolean hasFixedRange, float fixedRange, SoundCategory category, int x, int y, int z, float volume, float pitch, long seed) {
        return new ClientboundSoundEffectPacket(new SoundEvent(null, identifier, hasFixedRange, fixedRange), category, x, y, z, volume, pitch, seed);
    }

    @Override
    public void read(ByteBuf byteBuf) {
        // Sound Event: ID or Sound Event
        // 先读取 VarInt，如果是 0 则后面是内联定义，否则是注册表 ID
        int soundId = MinecraftProtocolHelper.readVarInt(byteBuf);
        if (soundId == 0) {
            // 内联定义: Identifier + Optional fixed range
            Key identifier = MinecraftProtocolHelper.readKey(byteBuf);
            boolean hasFixedRange = byteBuf.readBoolean();
            if (hasFixedRange) {
                this.soundEvent = new SoundEvent(null, identifier, true, byteBuf.readFloat());
            } else {
                this.soundEvent = new SoundEvent(null, identifier, false, 0);
            }
        } else {
            // 注册表 ID (实际 ID = soundId - 1)
            this.soundEvent = new SoundEvent(soundId - 1, null, false, 0);
        }
        this.category = MinecraftProtocolHelper.readEnum(byteBuf, SoundCategory::fromId);
        this.x = byteBuf.readInt();
        this.y = byteBuf.readInt();
        this.z = byteBuf.readInt();
        this.volume = byteBuf.readFloat();
        this.pitch = byteBuf.readFloat();
        this.seed = byteBuf.readLong();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        if (soundEvent.registryId() != null) {
            // 注册表 ID
            MinecraftProtocolHelper.writeVarInt(byteBuf, soundEvent.registryId() + 1);
        } else {
            // 内联定义
            MinecraftProtocolHelper.writeVarInt(byteBuf, 0);
            MinecraftProtocolHelper.writeKey(byteBuf, soundEvent.identifier());
            byteBuf.writeBoolean(soundEvent.hasFixedRange());
            if (soundEvent.hasFixedRange()) {
                byteBuf.writeFloat(soundEvent.fixedRange());
            }
        }
        MinecraftProtocolHelper.writeEnum(byteBuf, category);
        byteBuf.writeInt(x);
        byteBuf.writeInt(y);
        byteBuf.writeInt(z);
        byteBuf.writeFloat(volume);
        byteBuf.writeFloat(pitch);
        byteBuf.writeLong(seed);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        PlaySoundEvent playSoundEvent = new PlaySoundEvent(soundEvent, category, x, y, z, volume, pitch, seed);
        EventRegistry.callEvent(playSoundEvent);
        this.soundEvent = playSoundEvent.getSoundEvent();
        this.category = playSoundEvent.getCategory();
        this.x = playSoundEvent.getX();
        this.y = playSoundEvent.getY();
        this.z = playSoundEvent.getZ();
        this.volume = playSoundEvent.getVolume();
        this.pitch = playSoundEvent.getPitch();
        this.seed = playSoundEvent.getSeed();
        return HandleResult.FORWARD;
    }

    public enum SoundCategory implements MinecraftProtocolHelper.ProtocolEnum {

        MASTER(0),
        MUSIC(1),
        RECORDS(2),
        WEATHER(3),
        BLOCKS(4),
        HOSTILE(5),
        NEUTRAL(6),
        PLAYERS(7),
        AMBIENT(8),
        VOICE(9);

        private final int id;

        SoundCategory(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        public static SoundCategory fromId(int id) {
            return switch (id) {
                case 0 -> MASTER;
                case 1 -> MUSIC;
                case 2 -> RECORDS;
                case 3 -> WEATHER;
                case 4 -> BLOCKS;
                case 5 -> HOSTILE;
                case 6 -> NEUTRAL;
                case 7 -> PLAYERS;
                case 8 -> AMBIENT;
                case 9 -> VOICE;
                default -> null;
            };
        }
    }

    /**
     * Sound Event - 可以是注册表 ID 或内联定义
     * @param registryId 注册表 ID（如果是内联定义则为 null）
     * @param identifier 声音标识符（如果是注册表 ID 则为 null）
     * @param hasFixedRange 是否有固定范围
     * @param fixedRange 固定范围值
     */
    public record SoundEvent(Integer registryId, Key identifier, boolean hasFixedRange, float fixedRange) {}
}
