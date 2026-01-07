package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

public class PlayerLoginCompleteEvent extends PlayerEvent {
    public PlayerLoginCompleteEvent(ProxiedPlayer player) {
        super(player);
    }
}
