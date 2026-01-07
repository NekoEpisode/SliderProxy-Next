package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

public class PlayerQuitEvent extends PlayerEvent {
    public PlayerQuitEvent(ProxiedPlayer player) {
        super(player);
    }
}
