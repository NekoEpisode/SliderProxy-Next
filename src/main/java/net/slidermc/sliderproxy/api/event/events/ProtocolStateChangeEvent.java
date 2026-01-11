package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.ProtocolState;

/**
 * 协议状态改变事件
 * 在玩家的协议状态发生改变时触发
 * 
 * 注意：此事件不能被取消，因为协议状态改变通常是不可逆的
 */
public class ProtocolStateChangeEvent extends PlayerEvent {
    private final ProtocolState from;
    private final ProtocolState to;
    private final Direction direction;

    public ProtocolStateChangeEvent(ProxiedPlayer player, ProtocolState from, ProtocolState to, Direction direction) {
        super(player);
        this.from = from;
        this.to = to;
        this.direction = direction;
    }

    /**
     * 获取改变前的协议状态
     * @return 原协议状态
     */
    public ProtocolState getFrom() {
        return from;
    }

    /**
     * 获取改变后的协议状态
     * @return 新协议状态
     */
    public ProtocolState getTo() {
        return to;
    }

    /**
     * 获取状态改变的方向（上游/下游）
     * @return 方向
     */
    public Direction getDirection() {
        return direction;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("ProtocolStateChangeEvent cannot be cancelled - protocol state changes are irreversible");
    }

    /**
     * 协议状态改变的方向
     */
    public enum Direction {
        /** 上游（客户端到代理） */
        UPSTREAM,
        /** 下游（代理到服务器） */
        DOWNSTREAM
    }
}
