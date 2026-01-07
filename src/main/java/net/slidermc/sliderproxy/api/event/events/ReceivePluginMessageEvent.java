package net.slidermc.sliderproxy.api.event.events;

import io.netty.channel.Channel;
import net.kyori.adventure.key.Key;
import net.slidermc.sliderproxy.api.event.Event;

public class ReceivePluginMessageEvent extends Event {
    private Key identifier;
    private byte[] data;
    private final From from;
    private final Channel channel;

    public ReceivePluginMessageEvent(Key identifier, byte[] data, From from, Channel channel) {
        this.identifier = identifier;
        this.data = data;
        this.from = from;
        this.channel = channel;
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

    public Channel getChannel() {
        return channel;
    }

    public enum From {
        DOWNSTREAM,
        UPSTREAM
    }
}
