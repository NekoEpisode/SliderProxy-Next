package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

public class PlayerLoginEvent extends PlayerEvent {
    public PlayerLoginEvent(ProxiedPlayer player) {
        super(player);
    }
}
