package net.slidermc.sliderproxy.network.packet.clientbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.key.Key;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientboundSoundEffectPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundSoundEffectPacket.class);
    private Key soundEvent;
    private SoundCategory category;
    private int x, y, z;
    private float volume, pitch;
    private long seed;

    public ClientboundSoundEffectPacket() {}

    public ClientboundSoundEffectPacket(Key soundEvent, SoundCategory category, int x, int y, int z, float volume, float pitch, long seed) {
        this.soundEvent = soundEvent;
        this.category = category;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.soundEvent = MinecraftProtocolHelper.readKey(byteBuf);
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
        MinecraftProtocolHelper.writeKey(byteBuf, soundEvent);
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
        /*PlaySoundEvent playSoundEvent = new PlaySoundEvent(soundEvent, category, x, y, z, volume, pitch, seed);
        EventRegistry.callEvent(playSoundEvent);
        this.soundEvent = playSoundEvent.getSoundEvent();
        this.category = playSoundEvent.getCategory();
        this.x = playSoundEvent.getX();
        this.y = playSoundEvent.getY();
        this.z = playSoundEvent.getZ();
        this.volume = playSoundEvent.getVolume();
        this.pitch = playSoundEvent.getPitch();
        this.seed = playSoundEvent.getSeed();*/
        log.debug("收到sound effect包: " + this.soundEvent);
        this.pitch = this.pitch + 0.5f;
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByDownstreamChannel(ctx.channel());
        if (player != null) {
            player.sendPacket(this);
        }
        return HandleResult.UNFORWARD;
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
}
