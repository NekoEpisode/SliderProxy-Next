package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.event.Event;
import net.slidermc.sliderproxy.network.packet.clientbound.play.ClientboundSoundEffectPacket;

public class PlaySoundEvent extends Event {
    private ClientboundSoundEffectPacket.SoundEvent soundEvent;
    private ClientboundSoundEffectPacket.SoundCategory category;
    private int x, y, z;
    private float volume, pitch;
    private long seed;

    public PlaySoundEvent (ClientboundSoundEffectPacket.SoundEvent soundEvent, ClientboundSoundEffectPacket.SoundCategory category, int x, int y, int z, float volume, float pitch, long seed) {
        this.soundEvent = soundEvent;
        this.category = category;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    public ClientboundSoundEffectPacket.SoundEvent getSoundEvent() {
        return soundEvent;
    }

    public void setSoundEvent(ClientboundSoundEffectPacket.SoundEvent soundEvent) {
        this.soundEvent = soundEvent;
    }

    public ClientboundSoundEffectPacket.SoundCategory getCategory() {
        return category;
    }

    public void setCategory(ClientboundSoundEffectPacket.SoundCategory category) {
        this.category = category;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }
}
