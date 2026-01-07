package net.slidermc.sliderproxy.api.event.events;

import net.kyori.adventure.key.Key;
import net.slidermc.sliderproxy.api.event.Event;

public class ReceivePluginMessageEvent extends Event {
    private Key identifier;
    private byte[] data;
    private final From from;

    public ReceivePluginMessageEvent(Key identifier, byte[] data, From from) {
        this.identifier = identifier;
        this.data = data;
        this.from = from;
    }

    public byte[] getData() {
        return data;
    }

    public From getFrom() {
        return from;
    }

    public Key getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Key identifier) {
        this.identifier = identifier;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public enum From {
        DOWNSTREAM,
        UPSTREAM
    }
}
