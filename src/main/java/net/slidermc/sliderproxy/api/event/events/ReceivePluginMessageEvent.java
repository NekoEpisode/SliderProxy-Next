package net.slidermc.sliderproxy.api.event.events;

import io.netty.channel.Channel;
import net.kyori.adventure.key.Key;
import net.slidermc.sliderproxy.api.event.Event;

public class ReceivePluginMessageEvent extends Event {
    private Key identifier;
    private byte[] data;
    private Result result = Result.FORWARD;
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

    public void setResult(Result result) {
        this.result = result;
    }

    public Result getResult() {
        return result;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new IllegalArgumentException("Use setResult() instead");
    }

    public enum From {
        DOWNSTREAM,
        UPSTREAM
    }

    /**
     * 定义了在处理插件消息事件时可能的结果。
     * <p>
     * 每个枚举值代表了对收到的消息的一种处理策略：
     * - HANDLE_AND_NOT_FORWARD: 处理消息但不转发。
     * - HANDLE_AND_FORWARD: 处理消息并将其转发。
     * - DROP: 丢弃消息，既不处理也不转发。
     * - FORWARD: 不处理消息，直接转发原值。
     */
    public enum Result {
        HANDLE_AND_NOT_FORWARD,
        HANDLE_AND_FORWARD,
        DROP,
        FORWARD
    }
}
