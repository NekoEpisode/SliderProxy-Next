package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

public class PlayerLoginEvent extends PlayerEvent {
    public PlayerLoginEvent(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public void setCancelled(boolean cancelled) {
        if (cancelled) {
            throw new IllegalArgumentException("Cannot cancell player login event, please use ProxiedPlayer.kick() instead");
        }
    }
}
